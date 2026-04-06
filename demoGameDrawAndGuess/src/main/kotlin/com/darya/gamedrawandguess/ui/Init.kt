package com.darya.gamedrawandguess.ui

import com.darya.gamedrawandguess.drawingpart.Drawing
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.ColorPicker
import javafx.scene.control.Slider
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.StrokeLineCap



object Init {
    fun initCanvas(canvas: Canvas, canvasContainer: StackPane) {
        canvas.widthProperty().bind(canvasContainer.widthProperty())
        canvas.heightProperty().bind(canvasContainer.heightProperty())

        canvasContainer.minWidth = 0.0
        canvasContainer.minHeight = 0.0

        canvas.widthProperty().addListener { _ -> Drawing.redraw(canvas) }
        canvas.heightProperty().addListener { _ -> Drawing.redraw(canvas) }
    }

    fun initSizeSlider(sizeSlider: Slider) {
        sizeSlider.min = 1.0
        sizeSlider.max = 30.0
        sizeSlider.value = 3.0
    }

    fun initColorPicker(colorPicker: ColorPicker) {
        colorPicker.value = Color.BLACK
    }

    fun initGraphicContext(canvas: Canvas, sizeSlider: Slider, colorPicker: ColorPicker):GraphicsContext {
        val gc = canvas.graphicsContext2D

        sizeSlider.valueProperty().addListener { _, _, newValue ->
            gc.lineWidth = newValue.toDouble()
        }
        colorPicker.valueProperty().addListener { _, _, newColor ->
            gc.stroke = newColor
        }
        gc.lineCap = StrokeLineCap.ROUND

        return gc
    }

}