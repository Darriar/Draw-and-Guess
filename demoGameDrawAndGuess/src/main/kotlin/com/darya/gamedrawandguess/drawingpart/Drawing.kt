package com.darya.gamedrawandguess.drawingpart

import com.darya.gamedrawandguess.model.LineData
import javafx.scene.canvas.Canvas
import javafx.scene.control.ColorPicker
import javafx.scene.control.Slider
import javafx.scene.shape.StrokeLineCap
import java.io.PrintWriter

object Drawing {
    private var lastX: Double = 0.0
    private var lastY: Double = 0.0

    private fun drawLineOnCanvas(line: LineData, canvas: Canvas) {
        val gc = canvas.graphicsContext2D
        gc.lineWidth = line.size
        gc.stroke = line.color
        gc.lineCap = StrokeLineCap.ROUND

        gc.strokeLine(line.x1 * canvas.width, line.y1 * canvas.height,
            line.x2 * canvas.width, line.y2 * canvas.height)
    }

    fun redraw(canvas: Canvas) {
        val gc = canvas.graphicsContext2D
        gc.clearRect(0.0, 0.0, canvas.width, canvas.height)
        val drawingHistory = DrawingHistory
        for (line in drawingHistory)
            drawLineOnCanvas(line, canvas)
    }

    fun setupDrawingEvents(canvas: Canvas, colorPicker: ColorPicker, sizeSlider: Slider, out: PrintWriter) {
        canvas.setOnMousePressed { event ->
            lastX = event.x / canvas.width
            lastY = event.y / canvas.height
        }

        canvas.setOnMouseDragged { event ->
            val currentX = event.x / canvas.width
            val currentY = event.y / canvas.height

            val line = LineData(lastX, lastY, currentX, currentY, colorPicker.value, sizeSlider.value)
            val drawingHistory = DrawingHistory
            drawingHistory.add(line)        // МОЖНО DrawingHistory.add(line)

            drawLineOnCanvas(line, canvas)

            out.println("DRAW:$lastX,$lastY,${currentX},${currentY},${colorPicker.value},${sizeSlider.value}")

            lastX = currentX
            lastY = currentY
        }
    }
}