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
        gameCanvas.widthProperty().bind(canvasContainer.widthProperty())
        gameCanvas.heightProperty().bind(canvasContainer.heightProperty())

        sizeSlider.min = 1.0
        sizeSlider.max = 30.0
        sizeSlider.value = 3.0
        colorPicker.value = Color.BLACK

        gc = gameCanvas.graphicsContext2D

        sizeSlider.valueProperty().addListener { _, _, newValue ->
            gc.lineWidth = newValue.toDouble()
        }
        colorPicker.valueProperty().addListener { _, _, newColor ->
            gc.stroke = newColor
        }
        gc.lineCap = javafx.scene.shape.StrokeLineCap.ROUND

        val socket = ToServer.connect(chatTextArea, gc, gameCanvas)
        out = PrintWriter(socket!!.getOutputStream(), true)     // МОЖЕТ БЫТЬ NULL
        out.println("Darya") // ПОЛЬЗОВАТЕЛЬКИЕ ИМЕНА


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

    @FXML
    fun clearCanvas() {
        val width = gameCanvas.width
        val height = gameCanvas.height

        gc.clearRect(0.0, 0.0, width, height)
        out.println("CLEAR")
    }
}