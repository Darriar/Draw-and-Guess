package com.darya.gamedrawandguess.ui

import com.darya.gamedrawandguess.drawingpart.Drawing
import com.darya.gamedrawandguess.drawingpart.DrawingHistory
import com.darya.gamedrawandguess.ToServer
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.ColorPicker
import javafx.scene.control.Label
import javafx.scene.control.Slider
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.layout.StackPane
import javafx.util.Duration
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

    private lateinit var gc: GraphicsContext
    private lateinit var out: PrintWriter       // Этот объект создан при подключении к сокету
    private var userName: String = ""
    private var timeLine: Timeline? = null
    private lateinit var serverConnection: ToServer

    @FXML
    fun initialize() {
        Init.initCanvas(gameCanvas, canvasContainer)
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

    fun setCurrentPainterName(name: String) {
        statusLabel.text = "Рисует $name... Угадайте слово!"
    }

    @FXML
    fun onSendBtnClick() {
        val text =messageTextField.text
        if (text.isNotEmpty()) {
            out.println("CHAT:$userName: $text")
            messageTextField.clear()
        }
    }

    @FXML
    fun clearCanvas() {
        val width = gameCanvas.width
        val height = gameCanvas.height

        gc.clearRect(0.0, 0.0, width, height)
        DrawingHistory.clear()
        out.println("CLEAR")
    }

    fun updateTimer(seconds: Int) {
        timeLine?.stop()        //  зачем останавливать старый

        var timeLeft = seconds
        timeLine = Timeline(KeyFrame(Duration.seconds(1.0), {
            timeLeft--
            Platform.runLater {
                timerLabel.text = "Осталось: $timeLeft"
            }
            if (timeLeft <= 0)  timeLine?.stop()
        }))

        timeLine?.cycleCount = seconds
        timeLine?.play()
    }

    fun stopTimer() {
        Platform.runLater {
            timeLine?.stop()
            timerLabel.text = "Время вышло!"
        }
    }

    fun updateWord(word: String) {
        wordLabel.text = word
    }
}