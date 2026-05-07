package com.darya.gamedrawandguess.util

import javafx.scene.control.Alert

object UIUtils {
    fun createAlert(type: Alert.AlertType, titleA: String, headerA: String, contentA: String): Alert {
        return  Alert(type).apply {
            title = titleA
            headerText = headerA
            contentText = contentA
            showAndWait()
        }
    }
}