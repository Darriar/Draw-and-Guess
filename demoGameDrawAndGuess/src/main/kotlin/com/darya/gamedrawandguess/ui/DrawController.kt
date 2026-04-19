package com.darya.gamedrawandguess.ui

import com.darya.gamedrawandguess.drawingpart.Drawing
import com.darya.gamedrawandguess.ToServer
import com.darya.gamedrawandguess.model.GameEvent
import com.darya.gamedrawandguess.model.PlayerInfo
import com.darya.gamedrawandguess.model.ShapeType
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.fxml.FXML
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.ColorPicker
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.Slider
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.util.Duration
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.PrintWriter

class DrawController {
    @FXML
    private lateinit var gameCanvas: Canvas
    @FXML
    private lateinit var tempCanvas: Canvas
    @FXML
    private lateinit var canvasContainer: StackPane
    @FXML
    private lateinit var colorPicker: ColorPicker
    @FXML
    private lateinit var sizeSlider: Slider
    @FXML
    private lateinit var messageTextField: TextField
    @FXML
    private lateinit var chatTextArea: TextArea
    @FXML
    private lateinit var timerLabel: Label
    @FXML
    private lateinit var wordLabel: Label
    @FXML
    private lateinit var statusLabel: Label
    @FXML
    private lateinit var leftVBox: VBox
    @FXML
    private lateinit var bottomHBox: HBox
    @FXML
    private lateinit var shapeComboBox: ComboBox<ShapeType>

    private lateinit var gc: GraphicsContext
    private lateinit var out: PrintWriter
    private var userName: String = ""
    private var timeLine: Timeline? = null
    private lateinit var serverConnection: ToServer
    private var playersInfo =  FXCollections.observableArrayList<PlayerInfo>()
    private var drawingHistory = mutableListOf<GameEvent>()

    @FXML
    fun initialize() {
        setUpIU()
        setUpNetwork()
    }

    private fun setUpIU() {
        Init.initCanvas(gameCanvas, canvasContainer, drawingHistory)
        Init.initCanvas(tempCanvas, canvasContainer, drawingHistory)
        Init.initSizeSlider(sizeSlider)
        Init.initColorPicker(colorPicker)
        Init.initComboBox(shapeComboBox)
        gc = Init.initGraphicContext(gameCanvas, sizeSlider, colorPicker)
        playersInfo.addListener(ListChangeListener {
            val nodes = playersInfo.map { createPlayerRow(it) }
            leftVBox.children.setAll(nodes)
        })
    }

    private fun setUpNetwork() {
        serverConnection = ToServer(this)
        val socket = serverConnection.connect(chatTextArea, gameCanvas, tempCanvas)
        if (socket != null) {
            out = PrintWriter(socket.getOutputStream(), true)     // МОЖЕТ БЫТЬ NULL
            Drawing.setupDrawingEvents(gameCanvas, tempCanvas, colorPicker, sizeSlider, shapeComboBox, out)
        }
    }

    fun setUserName(name: String) {
        userName = name
        out.println(userName)
    }

    fun setCurrentPainter(name: String) {
        statusLabel.text = "Рисует $name... Угадайте слово!"
    }

    @FXML
    fun onSendBtnClick() {
        val text =messageTextField.text
        if (text.isNotEmpty()) {
            val message: GameEvent = GameEvent.Chat(userName = this.userName, message = text)
            out.println(Json.encodeToString(message))
            messageTextField.clear()
        }
    }

    // СОРТИРГОВАТЬ СВЕРХУ У КОГО БОЛЬШЕ ОЧКОВ
    private fun createPlayerRow(player: PlayerInfo): HBox {
        val scoreLabel = Label().apply {
            textProperty().bind(player.scoreProperty.asString())
        }
        val nameLabel = Label("${player.name}:")

        return HBox(10.0, nameLabel, scoreLabel)
    }

    fun updatePlayerScore(id: Int, score: String) {
        val newScore = score.toIntOrNull() ?: 0
        val player = playersInfo.find { it.id == id }

        player?.scoreProperty?.set(newScore)

        playersInfo.sortByDescending { it.scoreProperty.get() }
    }

    fun createPlayerInfo(id: Int, userName: String, score: String) {
        val p = PlayerInfo(id, userName, score.toIntOrNull() ?: 0)
        playersInfo.add(p)
        playersInfo.sortByDescending { it.scoreProperty.get() }
    }

    fun removePlayerInfo(id: Int) {
        playersInfo.removeIf { it.id == id }
    }


    fun clearDrawingHistory() {
        drawingHistory.clear()
    }

    fun addLineToDrawingHistory(line: GameEvent.DrawShape) {
        drawingHistory.add(line)
    }

    @FXML
    fun clearCanvas() {
        val event = GameEvent.Clear
        out.println(Json.encodeToString<GameEvent>(event))
        Drawing.clearGameCanvasToWhite(gameCanvas)
    }

    fun updateTimer(seconds: Int) {
        timeLine?.stop()
        var timeLeft = seconds

        timeLine = Timeline(KeyFrame(Duration.seconds(1.0), {
            timeLeft--
            timerLabel.text = "Осталось: $timeLeft"
            if (timeLeft <= 0) stopTimer()
        })).apply {
            cycleCount = seconds
            play()
        }
    }

    fun stopTimer() {
        timeLine?.stop()
        timerLabel.text = "Время вышло!"
    }

    fun updateWord(word: String) {
        wordLabel.text = word
    }

    fun blockCanvas() {
        tempCanvas.disableProperty().set(true)
    }

    fun setDrawingMode(isPainterMode: Boolean) {
        tempCanvas.isDisable = !isPainterMode
        messageTextField.isDisable = isPainterMode
        bottomHBox.isVisible = isPainterMode
        if (!isPainterMode) { updateWord("*****")}

    }
}