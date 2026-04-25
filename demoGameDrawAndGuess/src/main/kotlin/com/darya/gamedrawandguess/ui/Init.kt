package com.darya.gamedrawandguess.ui

import com.darya.gamedrawandguess.drawingpart.Drawing
import com.darya.gamedrawandguess.model.GameEvent
import com.darya.gamedrawandguess.model.ShapeType
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.Button
import javafx.scene.control.ColorPicker
import javafx.scene.control.Slider
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.shape.StrokeLineCap



object Init {
    fun initCanvas(canvas: Canvas, canvasContainer: StackPane, drawingHistory: MutableList<GameEvent>) {
        canvas.widthProperty().bind(canvasContainer.widthProperty())
        canvas.heightProperty().bind(canvasContainer.heightProperty())

        canvasContainer.minWidth = 0.0
        canvasContainer.minHeight = 0.0

        canvas.graphicsContext2D.fill = Color.WHITE
        canvas.graphicsContext2D.fillRect(0.0, 0.0, canvas.width, canvas.height)
        println("${canvas.width}, ${canvas.height}")

        canvas.widthProperty().addListener { _ -> Drawing.redraw(canvas, drawingHistory) }
        canvas.heightProperty().addListener { _ -> Drawing.redraw(canvas, drawingHistory) }
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

    fun initToolButtons(toolsVBox: VBox, onToolSelected: (ShapeType) -> Unit) {
        val types = ShapeType.entries

        for (type in types) {
            val button = Button(type.toString()).apply {
                maxWidth = Double.MAX_VALUE

                styleClass.add("tool-button")
                if (type == ShapeType.PENCIL) { // замени на свой дефолтный тип
                    styleClass.add("active-tool")
                }

                setOnAction {
                    toolsVBox.children.filterIsInstance<Button>().forEach {
                        it.styleClass.remove("active-tool")
                    }
                    styleClass.add("active-tool")
                    onToolSelected(type)
                }
            }

            toolsVBox.children.add(button)
        }
    }


}