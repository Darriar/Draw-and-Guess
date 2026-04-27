package com.darya.gamedrawandguess.drawingpart

import com.darya.gamedrawandguess.model.GameEvent
import com.darya.gamedrawandguess.model.ShapeType
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.Button
import javafx.scene.control.ColorPicker
import javafx.scene.control.Slider
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import javafx.scene.shape.StrokeLineCap
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.PrintWriter
import kotlin.collections.ArrayDeque
import kotlin.math.abs

object Drawing {

    fun redraw(canvas: Canvas, drawingHistory: MutableList<GameEvent>) {
        clearCanvas(canvas)
        for (shape in drawingHistory)
            drawShape(shape as GameEvent.DrawShape, canvas)
    }

    fun setupDrawingEvents(gameCanvas: Canvas, tempCanvas: Canvas, colorPicker: ColorPicker, sizeSlider: Slider, clearBtn: Button, shapeProvider: () -> ShapeType, out: PrintWriter, drawingHistory: MutableList<GameEvent>) {
        var startX = 0.0
        var startY = 0.0

        fun createShape(event: MouseEvent) {
            val shapeType = shapeProvider()
            if (shapeType.isFloodFill) return

            clearCanvas(tempCanvas)
            val currentX = event.x / tempCanvas.width
            val currentY = event.y / tempCanvas.height

            val color = if (shapeType == ShapeType.ERASER) Color.WHITE else colorPicker.value
            val isPreview = !(shapeType.isHandleDrawing || event.eventType == MouseEvent.MOUSE_RELEASED)
            val canvas = if (isPreview) tempCanvas else gameCanvas

            val shape = GameEvent.DrawShape(shapeType, startX, startY, currentX, currentY, color.toString(), sizeSlider.value, isPreview)

            val jsonMessage = Json.encodeToString<GameEvent>(shape)
            out.println(jsonMessage)

            if (shapeType.isHandleDrawing) {
                startX = currentX
                startY = currentY
            }

            drawShape(shape, canvas)
            if (!isPreview) drawingHistory.add(shape)
        }

        tempCanvas.setOnMousePressed { event ->
            startX = event.x / tempCanvas.width
            startY = event.y / tempCanvas.height

            val shapeType = shapeProvider()
            if (shapeType.isFloodFill) {
                val fillShape = GameEvent.DrawShape(
                    ShapeType.FLOODFILL, startX, startY, startX, startY,
                    colorPicker.value.toString(), 0.0, false
                )
                drawShape(fillShape, gameCanvas)
                out.println(Json.encodeToString<GameEvent>(fillShape))
                drawingHistory.add(fillShape)
            }
        }

        tempCanvas.setOnMouseDragged { event ->
            if (tempCanvas.isDisable) return@setOnMouseDragged
            createShape(event)
        }

        tempCanvas.setOnMouseReleased { event ->
            if (tempCanvas.isDisable) return@setOnMouseReleased
            createShape(event)
        }

        clearBtn.setOnMousePressed {
            clearCanvas(gameCanvas)
            val clearShape = GameEvent.DrawShape(
                ShapeType.CLEAR, 0.0, 0.0, 0.0, 0.0, Color.WHITE.toString(), 0.0, false
            )
            out.println(Json.encodeToString<GameEvent>(clearShape))
            drawingHistory.add(clearShape)
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

                if (shape.shapeType == ShapeType.OVAL)
                    gc.strokeOval(x, y, w, h)
                else
                    gc.strokeRect(x, y, w, h)

            }
            ShapeType.LINE, ShapeType.PENCIL, ShapeType.ERASER -> {
                gc.strokeLine(x1, y1, x2, y2)
            }
            ShapeType.FLOODFILL -> {
                floodFill(x1, y1, shape.color, canvas)
            }
            ShapeType.CLEAR -> {
                clearCanvas(canvas)
            }
        }
    }

    private fun floodFill(startX: Double, startY: Double, color: String, canvas: Canvas) {
        val snapshot = canvas.snapshot(null, null)
        val reader = snapshot.pixelReader
        val writer = canvas.graphicsContext2D.pixelWriter
        val width = canvas.width.toInt()
        val height = canvas.height.toInt()

        val fillColor = Color.web(color)
        val startColor = reader.getColor(startX.toInt(), startY.toInt())
        if (startColor == fillColor) return

        val points = ArrayDeque<Pair<Int, Int>>()
        points.add(startX.toInt() to startY.toInt())
        val visitedPoints = Array(width) { BooleanArray(height) }

        while (points.isNotEmpty()) {
            val (x, y) = points.removeFirst()
            if (x < 0 || x >= width || y < 0 || y >= height) continue
            if (visitedPoints[x][y]) continue

            writer.setColor(x, y, fillColor)
            visitedPoints[x][y] = true

            val currentColor = reader.getColor(x, y)
            if (currentColor == startColor) {   // еще не контур
                points.add(x + 1 to y)
                points.add(x - 1 to y)
                points.add(x to y + 1)
                points.add(x to y - 1)
            }
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

    fun clearCanvas(canvas: Canvas) {
        canvas.graphicsContext2D.clearRect(0.0, 0.0, canvas.width, canvas.height)
    }

}