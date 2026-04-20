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

// проблеск между заливкой и контуром


// сети зайти в комнату создать
// из одного дома играть с другим из другого дома не локально


// UI
// кнопки линия заливка стирка
// сверху таймер и состояние (слово/кто рисует)
