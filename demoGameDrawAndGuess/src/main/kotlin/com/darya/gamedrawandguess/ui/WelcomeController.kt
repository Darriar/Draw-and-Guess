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
        if (userName.isNotEmpty()) {
            val fxmlLoader = FXMLLoader(DrawApplication::class.java.getResource("draw-view.fxml"))
            val mainRoot = fxmlLoader.load<Parent>()

            val drawController = fxmlLoader.getController<DrawController>()
            drawController.setUserName(userName)

            val stage = startBtn.scene.window as Stage
            stage.scene = Scene(mainRoot)
        } else {
            val alert =Alert(Alert.AlertType.INFORMATION)
            alert.title ="Ошибка!"
            alert.headerText = "Введите имя игрока!"
            alert.showAndWait()
        }
    }
}