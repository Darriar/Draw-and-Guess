package com.darya.gamedrawandguess

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage

class DrawApplication : Application() {
    override fun start(stage: Stage) {
        val fxmlLoader = FXMLLoader(DrawApplication::class.java.getResource("draw-view.fxml"))
        val scene = Scene(fxmlLoader.load())
        stage.title = "Draw && Guess!"
        stage.scene = scene
        stage.show()
    }
}

fun main() {
    Application.launch(DrawApplication::class.java)
}