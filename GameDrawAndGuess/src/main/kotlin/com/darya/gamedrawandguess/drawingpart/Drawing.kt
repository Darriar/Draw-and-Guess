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

    private var drawingHistory = mutableListOf<GameEvent.DrawShape>()
    private var redoStack = mutableListOf<GameEvent.DrawShape>()

    fun redraw(canvas: Canvas) {
        clearCanvas(canvas)
        for (shape in drawingHistory)
            drawShape(shape, canvas)
    }

    fun setupDrawingEvents(gameCanvas: Canvas, tempCanvas: Canvas, colorPicker: ColorPicker, sizeSlider: Slider,
                           clearBtn: Button, undoBtn: Button, redoBtn: Button,
                           shapeProvider: () -> ShapeType, out: PrintWriter) {
        var startX = 0.0
        var startY = 0.0
        val tempLineCoords: MutableList<GameEvent.Point> = mutableListOf()

        fun createShape(event: MouseEvent) {
            val shapeType = shapeProvider()
            if (shapeType.isFloodFill) return

            clearCanvas(tempCanvas)
            val currentX = event.x / tempCanvas.width
            val currentY = event.y / tempCanvas.height

            val color = if (shapeType == ShapeType.ERASER) Color.WHITE else colorPicker.value
            val isPreview = event.eventType != MouseEvent.MOUSE_RELEASED
            val canvas = if (isPreview) tempCanvas else gameCanvas

            var points = listOf(GameEvent.Point(startX, startY),
                                GameEvent.Point(currentX, currentY))

            if (shapeType.isHandleDrawing) {
                tempLineCoords.add(GameEvent.Point(startX, startY))
                startX = currentX
                startY = currentY
                points = tempLineCoords.toMutableList()
            }

            val shape = GameEvent.DrawShape(shapeType,points, color.toString(), sizeSlider.value, isPreview)

            val jsonMessage = Json.encodeToString<GameEvent>(shape)
            out.println(jsonMessage)

            drawShape(shape, canvas)
            if (!isPreview)  {
                drawingHistory.add(shape)
                tempLineCoords.clear()
            }
        }

        tempCanvas.setOnMousePressed { event ->
            startX = event.x / tempCanvas.width
            startY = event.y / tempCanvas.height

            val shapeType = shapeProvider()
            if (shapeType.isFloodFill) {
                val points = listOf(GameEvent.Point(startX, startY))
                val fillShape = GameEvent.DrawShape(
                    ShapeType.FLOODFILL, points,
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
                ShapeType.CLEAR, listOf(), Color.WHITE.toString(), 0.0, false
            )
            out.println(Json.encodeToString<GameEvent>(clearShape))
            drawingHistory.add(clearShape)
        }

        undoBtn.setOnMousePressed {
            val undoAction = GameEvent.DrawShape(ShapeType.UNDO)
            undo(gameCanvas)
            out.println(Json.encodeToString<GameEvent>(undoAction))
        }

        redoBtn.setOnMousePressed {
            val redoAction = GameEvent.DrawShape(ShapeType.REDO)
            out.println(Json.encodeToString(redoAction))
            redo(gameCanvas)
        }
    }

    fun drawShape(shape: GameEvent.DrawShape, canvas: Canvas) {
        val gc = initGc(shape, canvas)

        val points = conversePoints(shape.points, canvas.width, canvas.height)
        var x1 = 0.0
        var y1 = 0.0
        if (shape.points.isNotEmpty()) {
            x1 = points[0].x
            y1 = points[0].y
        }

        when (shape.shapeType) {
            ShapeType.OVAL, ShapeType.RECT -> {
                val x2 = points[1].x
                val y2 = points[1].y

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
                points.drop(1).forEach { point ->
                    gc.strokeLine(x1, y1, point.x, point.y)
                    x1 = point.x
                    y1 = point.y
                }
            }
            ShapeType.FLOODFILL -> {
                floodFill(x1, y1, shape.color, canvas)
            }
            ShapeType.CLEAR -> {
                clearCanvas(canvas)
            }
            ShapeType.UNDO -> {
                undo(canvas)
            }
            ShapeType.REDO -> {
                redo(canvas)
            }
        }
    }

    private fun conversePoints(points: List<GameEvent.Point>, width: Double, height: Double): List<GameEvent.Point> {
        return points.map { point -> GameEvent.Point(
            x = point.x * width,
            y = point.y * height)
        }
    }

    private fun floodFill(startX: Double, startY: Double, color: String, canvas: Canvas) {
        val snapshotResult = canvas.snapshot(null, null)
        val reader = snapshotResult.pixelReader
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

    private fun undo(gameCanvas: Canvas) {
        if (drawingHistory.isNotEmpty()) {
            val move = drawingHistory.removeLast()
            redoStack.add(move)
            redraw(gameCanvas)
        }
    }

    private fun redo(gameCanvas: Canvas) {
        if (redoStack.isNotEmpty()) {
            val move = redoStack.removeLast()
            drawingHistory.add(move)
            drawShape(move, gameCanvas)
        }
    }

    fun clearDrawingHistory() {
        drawingHistory.clear()
        redoStack.clear()
    }

    fun addLineToDrawingHistory(line: GameEvent.DrawShape) {
        drawingHistory.add(line)
    }
}