package com.darya.gamedrawandguess

import javafx.fxml.FXML
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.ColorPicker
import javafx.scene.control.Slider
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import java.io.PrintWriter
import java.net.Socket
import java.util.*

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

    private lateinit var gc: GraphicsContext    // Объект "кисть" для рисования
    private lateinit var out: PrintWriter       // Этот объект создан при подключении к сокету

    @FXML
    fun initialize() {

        try {
            val socket = Socket("192.168.100.11", 8080)
            out = PrintWriter(socket.getOutputStream(), true)
            startListening(socket)
            chatTextArea.appendText("Система: Вы подключены к серверу!\n")
            out.println("Darya") // Отправляем имя серверу первым делом!
        } catch (e: Exception) {
            chatTextArea.appendText("Ошибка: Не удалось подключиться.\n")
        }


        gameCanvas.widthProperty().bind(canvasContainer.widthProperty())
        gameCanvas.heightProperty().bind(canvasContainer.heightProperty())

        sizeSlider.min = 1.0
        sizeSlider.max = 30.0
        sizeSlider.value = 3.0
        colorPicker.value = Color.BLACK

        gc = gameCanvas.graphicsContext2D

        sizeSlider.valueProperty().addListener { _, _, newValue ->  // ???
            gc.lineWidth = newValue.toDouble()
        }
        colorPicker.valueProperty().addListener { _, _, newColor ->
            gc.stroke = newColor
        }
        gc.lineCap = javafx.scene.shape.StrokeLineCap.ROUND

        setupDrawingEvents()
    }

    private fun setupDrawingEvents() {
        gameCanvas.setOnMousePressed { event ->
            val x = event.x
            val y = event.y
            gc.beginPath()
           // gc.moveTo(event.x, event.y) // Перемещаем "перо" в точку нажатия
            gc.stroke()

            out.println("START:$x,$y")
        }

        gameCanvas.setOnMouseDragged { event ->
            val x = event.x
            val y = event.y
            gc.lineTo(event.x, event.y)
            gc.stroke()

            out.println("DRAW:$x,$y")
        }
    }

    @FXML
    fun onSendBtnClick() {
        val text = messageTextField.text
        if (text.isNotEmpty()) {
            out.println("CHAT:$text")
            messageTextField.clear()
        }
    }


    private fun startListening(socket: Socket) {
        val input = Scanner(socket.getInputStream())

        Thread {
            try {
                while (input.hasNextLine()) {
                    val message = input.nextLine()

                    javafx.application.Platform.runLater {
                        processMessage(message)
                    }
                }
            } catch (e: Exception) {
                javafx.application.Platform.runLater {
                    chatTextArea.appendText("Система: Соединение разорвано: ${e.message}\n")
                }
            }
        }.start()
    }

    private fun processMessage(message: String) {
        try {
            when {
                message.startsWith("CHAT:") -> {
                    chatTextArea.appendText(message.substring(5) + "\n")
                }
                message.startsWith("START:") -> {
                    val coords = message.substring(6).split(",")
                    gc.beginPath()
                    gc.moveTo(coords[0].toDouble(), coords[1].toDouble())

                }
                message.startsWith("DRAW:") -> {
                    val coords = message.substring(5).split(",")
                    gc.lineTo(coords[0].toDouble(), coords[1].toDouble())
                    gc.stroke()
                }
                message == "CLEAR" -> {
                    gc.clearRect(0.0, 0.0, gameCanvas.width, gameCanvas.height)
                }
            }
        } catch (e: Exception) {
            println("Ошибка парсинга сообщения: $message")
        }
    }

    @FXML
    fun clearCanvas() {
        val width = gameCanvas.width
        val height = gameCanvas.height

        gc.clearRect(0.0, 0.0, width, height)
        out.println("CLEAR")
    }
}