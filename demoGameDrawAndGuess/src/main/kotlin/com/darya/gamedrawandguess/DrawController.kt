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
import javafx.scene.shape.StrokeLineCap
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

    data class DrawLine(
        val x1: Double, val y1: Double, // Старт (в %)
        val x2: Double, val y2: Double, // Конец (в %)
    )

    private val drawHistory = mutableListOf<DrawLine>()
    private var lastX: Double = 0.0
    private var lastY: Double = 0.0

    @FXML
    fun initialize() {
        gameCanvas.widthProperty().bind(canvasContainer.widthProperty())
        gameCanvas.heightProperty().bind(canvasContainer.heightProperty())

        canvasContainer.minWidth = 0.0
        canvasContainer.minHeight = 0.0

        gameCanvas.widthProperty().addListener { _ -> redraw() }
        gameCanvas.heightProperty().addListener { _ -> redraw() }

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
        gc.lineCap = StrokeLineCap.ROUND

        val socket = ToServer.connect(chatTextArea, gameCanvas)
        out = PrintWriter(socket!!.getOutputStream(), true)     // МОЖЕТ БЫТЬ NULL
        out.println("Darya") // ПОЛЬЗОВАТЕЛЬКИЕ ИМЕНА

        setupDrawingEvents()
    }

    private fun drawLineOnCanvas(line: DrawLine) {
        gc.strokeLine(line.x1 * gameCanvas.width, line.y1 * gameCanvas.height,
                      line.x2 * gameCanvas.width, line.y2 * gameCanvas.height)
    }

    private fun redraw() {
        gc.clearRect(0.0, 0.0, gameCanvas.width, gameCanvas.height)
        drawHistory.forEach { drawLineOnCanvas(it) }
    }

    private fun setupDrawingEvents() {
        gameCanvas.setOnMousePressed { event ->
            lastX = event.x / gameCanvas.width
            lastY = event.y / gameCanvas.height
        }

        gameCanvas.setOnMouseDragged { event ->
            val currentX = event.x / gameCanvas.width
            val currentY = event.y / gameCanvas.height

            val line = DrawLine(lastX, lastY, currentX, currentY)
            drawHistory.add(line)

            drawLineOnCanvas(line)

            out.println("DRAW:${lastX},${lastY},${currentX},${currentY},${colorPicker.value},${sizeSlider.value}")

            lastX = currentX
            lastY = currentY
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
        drawHistory.clear()
        out.println("CLEAR")
    }
}