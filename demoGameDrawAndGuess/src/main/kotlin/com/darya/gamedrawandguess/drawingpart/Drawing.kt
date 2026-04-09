package com.darya.gamedrawandguess.drawingpart

import com.darya.gamedrawandguess.model.GameEvent
import javafx.scene.canvas.Canvas
import javafx.scene.control.ColorPicker
import javafx.scene.control.Slider
import javafx.scene.paint.Color
import javafx.scene.shape.StrokeLineCap
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.PrintWriter

object Drawing {

    private fun drawLineOnCanvas(line: GameEvent.Draw, canvas: Canvas) {
        val gc = canvas.graphicsContext2D
        gc.lineWidth = line.size
        gc.stroke = Color.web(line.color)
        gc.lineCap = StrokeLineCap.ROUND

        gc.strokeLine(line.x1 * canvas.width, line.y1 * canvas.height,
            line.x2 * canvas.width, line.y2 * canvas.height)
    }

    fun redraw(canvas: Canvas, drawingHistory: MutableList<GameEvent.Draw>) {
        val gc = canvas.graphicsContext2D
        gc.clearRect(0.0, 0.0, canvas.width, canvas.height)

        for (line in drawingHistory)
            drawLineOnCanvas(line, canvas)
    }

    fun setupDrawingEvents(canvas: Canvas, colorPicker: ColorPicker, sizeSlider: Slider, out: PrintWriter) {
        var lastX = 0.0
        var lastY = 0.0

        canvas.setOnMousePressed { event ->
            lastX = event.x / canvas.width
            lastY = event.y / canvas.height
        }

        canvas.setOnMouseDragged { event ->
            val currentX = event.x / canvas.width
            val currentY = event.y / canvas.height

            val line = GameEvent.Draw(lastX, lastY, currentX, currentY, colorPicker.value.toString(), sizeSlider.value)
            drawLineOnCanvas(line, canvas)
            val jsonMessage = Json.encodeToString<GameEvent>(line)
            out.println(jsonMessage)

            lastX = currentX
            lastY = currentY
        }
    }
}