package com.darya.gamedrawandguess

import com.darya.gamedrawandguess.ui.DrawController
import javafx.application.Platform
import javafx.scene.canvas.Canvas
import javafx.scene.control.TextArea
import java.net.InetAddress
import java.net.Socket
import java.util.*

class ToServer(private val controller: DrawController) {

    private var socket: Socket? = null


    fun connect(chat: TextArea, gameCanvas: Canvas): Socket? {
        try {
            val localhost = InetAddress.getLocalHost().hostAddress
            println(localhost)
            socket = Socket("localhost", 8080)     // 10.177.142.105    192.168.100.11
            startListening(chat, gameCanvas)
            chat.appendText("Система: Вы подключены к серверу!\n")
            return socket
        } catch (e: Exception) {
            chat.appendText("Ошибка: Не удалось подключиться.\n")
            return null
        }
    }

    private fun startListening(chat: TextArea, gameCanvas: Canvas) {
        val currentSocket = socket ?: return
        val input = Scanner(currentSocket.getInputStream())
        val eventHandler = ProcessEvent(controller, chat, gameCanvas)

        Thread {
            try {
                while (input.hasNextLine()) {
                    val message = input.nextLine()

                    Platform.runLater {
                        eventHandler.processMessage(message)//processMessage(chat, message, gameCanvas)
                    }
                }
            } catch (e: Exception) {
                Platform.runLater {
                    chat.appendText("Система: Соединение разорвано.\n")
                }
            }
        }.start()
    }
}
