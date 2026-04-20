package com.darya.gamedrawandguess.ui

import com.darya.gamedrawandguess.DrawApplication
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.TextField
import javafx.stage.Stage

class WelcomeController {
    @FXML
    private lateinit var startBtn: Button
    @FXML
    private lateinit var inputNameText: TextField

    @FXML
    fun onStartBtnClick() {
        val userName = inputNameText.text
        if (userName.trim().isNotEmpty()) {
            val fxmlLoader = FXMLLoader(DrawApplication::class.java.getResource("draw-view.fxml"))
            val mainRoot = fxmlLoader.load<Parent>()

            val drawController = fxmlLoader.getController<DrawController>()

            val isConnected = drawController.attemptConnection()

            if (isConnected) {
                drawController.setUserName(userName) // Теперь out точно инициализирован
                val stage = startBtn.scene.window as Stage
                stage.scene = Scene(mainRoot)
            } else {
                createAlert(Alert.AlertType.ERROR, "Ошибка сети",
                    "Не удалось подключиться к серверу", "Проверьте, запущен ли сервер.")
            }
        } else {
            createAlert(Alert.AlertType.INFORMATION, "Ошибка ввода имени игрока!", "Пустое имя", "Введите имя игрока!")
        }
    }

    private fun createAlert(type: Alert.AlertType, titleA: String, headerA: String, contentA: String): Alert {
        return  Alert(type).apply {
            title = titleA
            headerText = headerA
            contentText = contentA
            showAndWait()
        }
    }
}