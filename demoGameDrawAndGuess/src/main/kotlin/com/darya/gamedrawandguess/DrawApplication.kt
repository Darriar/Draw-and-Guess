package com.darya.gamedrawandguess

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage
import kotlin.system.exitProcess

class DrawApplication : Application() {
    override fun start(stage: Stage) {
        val fxmlLoader = FXMLLoader(DrawApplication::class.java.getResource("welcome-view.fxml"))
        val scene = Scene(fxmlLoader.load())

        stage.minWidth = 1000.0
        stage.minHeight = 700.0

        stage.title = "Draw && Guess!"
        stage.icons.add(Image(javaClass.getResourceAsStream("/icons/app_icon.png")))
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


// конец  раунда и таймер пошел раньше чем слово сменилось

// кнопки красивые сделать картинки

// если сервер отключился отключать всех
// сети зайти в комнату создать показывать доступные создать порт занимать

// playerinfo initialscore зачем


// забить
// чтобы имя игрока нормально помещалось и переносилось в результатах
// если пишу в чате длинное сообщение то сразу на новую строку переносит
// имя игрока в чате выделять цветом