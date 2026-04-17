package com.darya.gamedrawandguess.drawingpart

import com.darya.gamedrawandguess.model.GameEvent
import com.darya.gamedrawandguess.model.ShapeType
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.ColorPicker
import javafx.scene.control.ComboBox
import javafx.scene.control.Slider
import javafx.scene.paint.Color
import javafx.scene.shape.StrokeLineCap
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.PrintWriter
import kotlin.collections.ArrayDeque
import kotlin.math.abs

object Drawing {

    fun redraw(canvas: Canvas, drawingHistory: MutableList<GameEvent>) {
        clearCanvasToWhite(canvas)
        for (shape in drawingHistory)
            drawShape(shape as GameEvent.DrawShape, canvas)
    }

    fun setupDrawingEvents(gameCanvas: Canvas, tempCanvas: Canvas, colorPicker: ColorPicker, sizeSlider: Slider, shapeComboBox: ComboBox<ShapeType>, out: PrintWriter) {
        var startX = 0.0
        var startY = 0.0

        tempCanvas.setOnMousePressed { event ->
            startX = event.x / tempCanvas.width
            startY = event.y / tempCanvas.height

            if (shapeComboBox.value == ShapeType.FLOODFILL) {
                val fillEvent = GameEvent.DrawShape(
                    ShapeType.FLOODFILL, startX, startY, startX, startY,
                    colorPicker.value.toString(), sizeSlider.value, false
                )
                drawShape(fillEvent, gameCanvas)
                out.println(Json.encodeToString<GameEvent>(fillEvent))
            }
        }

        tempCanvas.setOnMouseDragged { event ->
            tempCanvas.graphicsContext2D.clearRect(0.0, 0.0, tempCanvas.width, tempCanvas.height)
            val currentX = event.x / gameCanvas.width
            val currentY = event.y / gameCanvas.height

            val shape = shapeComboBox.value

            val isPreview = !(shape == ShapeType.FREEHAND || shape == ShapeType.ERASER)
            val color = if (shape == ShapeType.ERASER) Color.WHITE.toString() else colorPicker.value.toString()
            val preview = GameEvent.DrawShape(shape, startX, startY, currentX, currentY, color, sizeSlider.value,  isPreview)

            val jsonMessage = Json.encodeToString<GameEvent>(preview)
            out.println(jsonMessage)

            if (shape == ShapeType.FREEHAND || shape == ShapeType.ERASER) {
                drawShape(preview, gameCanvas)
                startX = currentX
                startY = currentY
            } else {
                drawShape(preview, tempCanvas)
            }
        }

        tempCanvas.setOnMouseReleased { event ->
            tempCanvas.graphicsContext2D.clearRect(0.0, 0.0, tempCanvas.width, tempCanvas.height)
            val endX = event.x / gameCanvas.width
            val endY = event.y / gameCanvas.height
            val color = if (shapeComboBox.value == ShapeType.ERASER) Color.WHITE.toString() else colorPicker.value.toString()
            val finalShape = GameEvent.DrawShape(shapeComboBox.value, startX, startY, endX, endY, color, sizeSlider.value, false)
            val jsonMessage = Json.encodeToString<GameEvent>(finalShape)
            out.println(jsonMessage)
            drawShape(finalShape, gameCanvas)
        }
    }

    fun drawShape(shape: GameEvent.DrawShape, canvas: Canvas) {
        val gc = initGc(shape, canvas)

        val x1 = shape.x1 * canvas.width
        val y1 = shape.y1 * canvas.height
        val x2 = shape.x2 * canvas.width
        val y2 = shape.y2 * canvas.height


        when (shape.shapeType) {
            ShapeType.OVAL, ShapeType.RECT -> {
                val x = minOf(x1, x2)
                val y = minOf(y1, y2)
                val w = abs(x1 - x2)
                val h = abs(y1 - y2)

                if (shape.shapeType == ShapeType.OVAL) {
                    gc.strokeOval(x, y, w, h)
                } else {
                    gc.strokeRect(x, y, w, h)
                }
            }
            ShapeType.LINE, ShapeType.FREEHAND, ShapeType.ERASER -> {
                gc.strokeLine(x1, y1, x2, y2)
            }
            ShapeType.FLOODFILL -> {
                floodFill(x1, y1, shape.color, canvas)
            }

            else -> {}
        }
    }

    private fun floodFill(startX: Double, startY: Double, color: String, canvas: Canvas) {
        val snapshot = canvas.snapshot(null, null)
        val reader = snapshot.pixelReader
        val writer = canvas.graphicsContext2D.pixelWriter
        val width = canvas.width.toInt()
        val height = canvas.height.toInt()

        val fillColor = Color.web(color)
        val targetColor = reader.getColor(startX.toInt(), startY.toInt())
        if (targetColor == fillColor) return

        val points = ArrayDeque<Pair<Int, Int>>()
        points.add(startX.toInt() to startY.toInt())
        val visitedPoints = Array(width) { BooleanArray(height) }

        while (points.isNotEmpty()) {
            val (x, y) = points.removeFirst()

            if (x < 0 || x >= width || y < 0 || y >= height) continue
            if (visitedPoints[x][y] || reader.getColor(x, y) == fillColor) continue

            writer.setColor(x, y, fillColor)
            visitedPoints[x][y] = true

            points.add(x + 1 to y)
            points.add(x - 1 to y)
            points.add(x to y + 1)
            points.add(x to y - 1)
        }
    }

    private fun initGc(shape: GameEvent.DrawShape, canvas: Canvas): GraphicsContext {
        val gc = canvas.graphicsContext2D

        return gc.apply {
            stroke = Color.web(shape.color)
            lineWidth = shape.size
            lineCap = StrokeLineCap.ROUND
        }
    }

    fun clearCanvasToWhite(canvas: Canvas) {
        val gc = canvas.graphicsContext2D
        gc.fill = Color.WHITE
        gc.fillRect(0.0, 0.0, canvas.width, canvas.height)
    }
}