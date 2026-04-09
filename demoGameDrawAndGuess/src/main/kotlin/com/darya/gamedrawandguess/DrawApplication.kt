package com.darya.gamedrawandguess

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage
import kotlin.system.exitProcess

class DrawApplication : Application() {
    override fun start(stage: Stage) {
        val fxmlLoader = FXMLLoader(DrawApplication::class.java.getResource("welcome-view.fxml"))
        val scene = Scene(fxmlLoader.load())
        stage.title = "Draw && Guess!"
        stage.scene = scene
        stage.show()
    }

    override fun stop() {
        exitProcess(0)
    }
}

fun main() {
    Application.launch(DrawApplication::class.java)
}

// сделать чтобы при входе отображалось  и весь чат до
// заливку
// фигуры линия квадрат круг
// палитра цветов как квадратики слева несколько

// всплывающее окно с тем какое было слово