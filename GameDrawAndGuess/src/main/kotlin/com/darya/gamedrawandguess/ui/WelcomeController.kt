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
            val fxmlLoader = FXMLLoader(DrawApplication::class.java.getResource("lobby-view.fxml"))
            val mainRoot = fxmlLoader.load<Parent>()
            val stage = startBtn.scene.window as Stage
            stage.scene = Scene(mainRoot)

            val lobbyController = fxmlLoader.getController<LobbyController>()
            lobbyController.setUserName(userName)
        } else {
            createAlert(Alert.AlertType.INFORMATION, "Ошибка ввода имени игрока!", "Пустое имя", "Введите имя игрока!")
        }
    }

    fun createAlert(type: Alert.AlertType, titleA: String, headerA: String, contentA: String): Alert {
        return  Alert(type).apply {
            title = titleA
            headerText = headerA
            contentText = contentA
            showAndWait()
        }
    }
}