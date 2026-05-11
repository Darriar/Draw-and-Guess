package com.darya.gamedrawandguess.client.ui

import com.darya.gamedrawandguess.client.drawingpart.Drawing
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
import java.io.InputStream


object Init {
    fun initCanvas(gameCanvas: Canvas, tempCanvas: Canvas, canvasContainer: StackPane) {
        gameCanvas.widthProperty().bind(canvasContainer.widthProperty())
        gameCanvas.heightProperty().bind(canvasContainer.heightProperty())
        tempCanvas.widthProperty().bind(canvasContainer.widthProperty())
        tempCanvas.heightProperty().bind(canvasContainer.heightProperty())

        canvasContainer.minWidth = 0.0
        canvasContainer.minHeight = 0.0

        gameCanvas.widthProperty().addListener { _ -> Drawing.redraw(gameCanvas) }
        gameCanvas.heightProperty().addListener { _ -> Drawing.redraw(gameCanvas) }
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

    fun configureButton(button: Button, type: ShapeType? = null, imagePath: String? = null) {
        button.maxWidth = Double.MAX_VALUE
        button.styleClass.add("tool-button")

        val imageUrl: String? = when {
            type != null -> {
                button.tooltip = Tooltip(type.label)
                ShapeType::class.java.getResource(type.imagePath)?.toExternalForm()
            }
            imagePath != null -> {
                this::class.java.getResource(imagePath)?.toExternalForm()
            }
            else -> null
        }

        if (imageUrl != null) {
            button.graphic = ImageView(Image(imageUrl)).apply {
                fitWidth = 30.0
                fitHeight = 30.0
                isPreserveRatio = true
            }
        } else {
            println("Ошибка: Изображение не найдено для кнопки ${button.id ?: "без ID"}")
        }
    }


}