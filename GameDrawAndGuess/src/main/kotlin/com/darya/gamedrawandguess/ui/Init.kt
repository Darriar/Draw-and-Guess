package com.darya.gamedrawandguess.ui

import com.darya.gamedrawandguess.drawingpart.Drawing
import com.darya.gamedrawandguess.model.GameEvent
import com.darya.gamedrawandguess.model.ShapeType
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.Button
import javafx.scene.control.ColorPicker
import javafx.scene.control.Slider
import javafx.scene.control.Tooltip
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.shape.StrokeLineCap



object Init {
    fun initCanvas(gameCanvas: Canvas, tempCanvas: Canvas, canvasContainer: StackPane, drawingHistory: MutableList<GameEvent.DrawShape>) {
        gameCanvas.widthProperty().bind(canvasContainer.widthProperty())
        gameCanvas.heightProperty().bind(canvasContainer.heightProperty())
        tempCanvas.widthProperty().bind(canvasContainer.widthProperty())
        tempCanvas.heightProperty().bind(canvasContainer.heightProperty())

        canvasContainer.minWidth = 0.0
        canvasContainer.minHeight = 0.0

        gameCanvas.widthProperty().addListener { _ -> Drawing.redraw(gameCanvas, drawingHistory) }
        gameCanvas.heightProperty().addListener { _ -> Drawing.redraw(gameCanvas, drawingHistory) }
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

    fun initToolButtons(toolsVBox: VBox, undoBtn: Button, redoBtn: Button, onToolSelected: (ShapeType) -> Unit) {
        val toolTypes = ShapeType.entries.filter { !it.isAction }

        for (type in toolTypes) {
            val button = Button().apply {
                configureButton(this, type)

                if (type == ShapeType.PENCIL) {
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
        configureButton(undoBtn, ShapeType.UNDO)
        configureButton(redoBtn, ShapeType.REDO)
    }

    private fun configureButton(button: Button, type: ShapeType) {
        button.maxWidth = Double.MAX_VALUE
        button.styleClass.add("tool-button")
        button.tooltip = Tooltip(type.label)

        // Загрузка иконки
        val stream = ShapeType::class.java.getResourceAsStream(type.imagePath)
        if (stream != null) {
            button.graphic = ImageView(Image(stream)).apply {
                fitWidth = 30.0
                fitHeight = 30.0
                isPreserveRatio = true
            }
        } else {
            button.text = type.label // Если картинки нет, показываем текст
            println("empty image ${type.label}")
        }
    }


}