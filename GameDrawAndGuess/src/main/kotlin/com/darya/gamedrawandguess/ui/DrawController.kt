package com.darya.gamedrawandguess.ui

import com.darya.gamedrawandguess.DrawApplication
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
import javafx.fxml.FXMLLoader
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.stage.Stage
import javafx.util.Duration
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.PrintWriter
import kotlin.math.abs

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
    private lateinit var statusLabel: Label
    @FXML
    private lateinit var bottomHBox: HBox
    @FXML
    private lateinit var toolsVBox: VBox
    @FXML
    private lateinit var toolsPane: VBox
    @FXML
    private lateinit var playersPane: VBox
    @FXML
    private lateinit var clearBtn: Button
    @FXML
    private lateinit var undoBtn: Button
    @FXML
    private lateinit var redoBtn: Button


    private lateinit var gc: GraphicsContext
    private var currentTool: ShapeType = ShapeType.PENCIL
    private lateinit var out: PrintWriter
    private var userName: String = ""
    private var timeLine: Timeline? = null
    private lateinit var serverConnection: ToServer
    private var playersInfo =  FXCollections.observableArrayList<PlayerInfo>()

    @FXML
    fun initialize() {

        Init.initCanvas(gameCanvas, tempCanvas, canvasContainer)
        Init.initSizeSlider(sizeSlider)
        Init.initColorPicker(colorPicker)
        Init.initToolButtons(toolsVBox, undoBtn, redoBtn) { selectedType ->  currentTool = selectedType}
        gc = Init.initGraphicContext(gameCanvas, sizeSlider, colorPicker)
        playersInfo.addListener(ListChangeListener {
            val nodes = playersInfo.map { createPlayerRow(it) }
            playersPane.children.remove(1, playersPane.children.size)
            playersPane.children.addAll(nodes)
        })
    }

    fun attemptConnection(ip: String, port: Int): Boolean {
        serverConnection = ToServer(this)
        val socket = serverConnection.connect(chatTextArea, gameCanvas, tempCanvas, ip, port) ?: return false

        out = PrintWriter(socket.getOutputStream(), true)
        Drawing.setupDrawingEvents(gameCanvas, tempCanvas, colorPicker, sizeSlider, clearBtn, undoBtn, redoBtn, { currentTool }, out)
        return true
    }

    fun setUserName(name: String) {
        userName = name
        out.println(userName)
    }

    fun setCurrentPainter(name: String) {
        statusLabel.text = "Рисует $name Угадайте слово!"
    }

    fun updateWord(word: String) {
        statusLabel.text = word
    }

    @FXML
    fun onSendBtnClick() {
        val text =messageTextField.text
        if (text.isNotEmpty()) {
            val message: GameEvent = GameEvent.Chat("$userName: $text")
            out.println(Json.encodeToString(message))
            messageTextField.clear()
        }
    }

    private fun createPlayerRow(player: PlayerInfo): HBox {
        val nameLabel = Label(player.name).apply {
            styleClass.add("player-name")
            maxWidth = Double.MAX_VALUE
            HBox.setHgrow(this, Priority.ALWAYS)
        }

        val scoreLabel = Label().apply {
            textProperty().bind(player.scoreProperty.asString("%d PTS"))
            styleClass.add("player-score")
        }

        val playerColor = generateColorFromString(player.name)
        val avatar = Circle(16.0, playerColor)

        return HBox(12.0, avatar, nameLabel, scoreLabel).apply {
            alignment = Pos.CENTER_LEFT
            styleClass.add("player-row-container")
        }
    }

    private fun generateColorFromString(input: String): Color {
        val hash = input.hashCode()

        val hue = abs(hash % 360).toDouble()
        return Color.hsb(hue, 0.4, 0.9)
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
        timerLabel.text = "Конец раунда!"
    }


    fun blockCanvas() {
        tempCanvas.disableProperty().set(true)
        toolsPane.isVisible = false
        playersPane.isVisible = true
        playersPane.isManaged = true
        bottomHBox.isVisible = false
    }

    fun setDrawingMode(isPainterMode: Boolean) {
        tempCanvas.isDisable = !isPainterMode
        messageTextField.isDisable = isPainterMode
        bottomHBox.isVisible = isPainterMode

        toolsPane.isVisible = isPainterMode
        toolsPane.isManaged = isPainterMode

        playersPane.isVisible = !isPainterMode
        playersPane.isManaged = !isPainterMode
    }

    fun onDisconnect() {
        gameCanvas.isDisable = true

        val alert = Alert(Alert.AlertType.ERROR)
        alert.title = "Связь потеряна"
        alert.headerText = "Сервер отключился"
        alert.contentText = "Вы будете возвращены в лобби."
        alert.showAndWait()

        val fxmlLoader = FXMLLoader(DrawApplication::class.java.getResource("lobby-view.fxml"))
        val root = fxmlLoader.load<Parent>()
        val stage = gameCanvas.scene.window as Stage
        stage.scene = Scene(root)
    }

}