package com.darya.gamedrawandguess.ui

import com.darya.gamedrawandguess.drawingpart.Drawing
import com.darya.gamedrawandguess.ToServer
import com.darya.gamedrawandguess.model.GameEvent
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.fxml.FXML
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.ColorPicker
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

    private lateinit var gc: GraphicsContext
    private lateinit var out: PrintWriter
    private var userName: String = ""
    private var timeLine: Timeline? = null
    private lateinit var serverConnection: ToServer
    private var playersInfo =  mutableMapOf<Int, Pair<HBox, Label>>()
    private var drawingHistory = mutableListOf<GameEvent.Draw>()

    @FXML
    fun initialize() {
        Init.initCanvas(gameCanvas, canvasContainer, drawingHistory)
        Init.initSizeSlider(sizeSlider)
        Init.initColorPicker(colorPicker)
        gc = Init.initGraphicContext(gameCanvas, sizeSlider, colorPicker)

        serverConnection = ToServer(this)
        val socket = serverConnection.connect(chatTextArea, gameCanvas)
        if (socket != null) {
            out = PrintWriter(socket.getOutputStream(), true)     // МОЖЕТ БЫТЬ NULL

            Drawing.setupDrawingEvents(gameCanvas, colorPicker, sizeSlider, out)
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
            val jsonMessage = Json.encodeToString(message)
            out.println(jsonMessage)
            messageTextField.clear()
        }
    }

    // СОРТИРГОВАТЬ СВЕРХУ У КОГО БОЛЬШЕ ОЧКОВ
    fun updatePlayersInfo() {
        leftVBox.children.clear()
        val sortedList = playersInfo.toList().sortedBy { it.second.second.text.toInt() }
        sortedList.forEach { leftVBox.children.add(it.second.first) }
    }

    fun updatePlayerScore(id: Int, score: String) {
        playersInfo[id]!!.second.text = score
    }

    fun createPlayerInfo(id: Int, userName: String, score: String) {
        val nameLabel = Label("$userName:")
        val scoreLabel = Label(score)

        val row = HBox(10.0, nameLabel, scoreLabel) // 10.0 ФИКС СДЕЛАТЬ КОНСТАНТОЙ
        playersInfo[id] = Pair(row, scoreLabel)
        updatePlayersInfo()
    }

    fun removePlayerInfo(id: Int) {
        playersInfo.remove(id)
        updatePlayersInfo()
    }

    fun clearDrawingHistory() {
        drawingHistory.clear()
    }

    fun addLineToDrawingHistory(line: GameEvent.Draw) {
        drawingHistory.add(line)
    }

    @FXML
    fun clearCanvas() {
        val event = GameEvent.Clear
        val jsonMessage = Json.encodeToString<GameEvent>(event)
        out.println(jsonMessage)
        gameCanvas.graphicsContext2D.clearRect(0.0, 0.0, gameCanvas.width, gameCanvas.height)
    }

    fun updateTimer(seconds: Int) {
        timeLine?.stop()

        var timeLeft = seconds
        timeLine = Timeline(KeyFrame(Duration.seconds(1.0), {
            timeLeft--
            timerLabel.text = "Осталось: $timeLeft"

            if (timeLeft <= 0)  timeLine?.stop()
        }))

        timeLine?.cycleCount = seconds
        timeLine?.play()
    }

    fun stopTimer() {
        timeLine?.stop()
        timerLabel.text = "Время вышло!"
    }

    fun updateWord(word: String) {
        wordLabel.text = word
    }

    fun setDrawingMode(isPainterMode: Boolean) {
        if (isPainterMode) {
            gameCanvas.disableProperty().set(false)
           // messageTextField.disableProperty().set(true)
            bottomHBox.visibleProperty().set(true)
        }
        else {
            gameCanvas.disableProperty().set(true)
            messageTextField.disableProperty().set(false)
            bottomHBox.visibleProperty().set(false)
            wordLabel.text = "*****"
        }
    }


}