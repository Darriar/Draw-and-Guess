package com.darya.gamedrawandguess

import com.darya.gamedrawandguess.ui.DrawController
import javafx.application.Platform
import javafx.scene.canvas.Canvas
import javafx.scene.control.TextArea
import java.net.Socket
import java.util.*

class ToServer(private val controller: DrawController) {
    private var socket: Socket? = null

    fun connect(chat: TextArea, gameCanvas: Canvas, tempCanvas: Canvas, ip: String, port: Int): Socket? {
        try {
            socket = Socket(ip, port)
            startListening(chat, gameCanvas, tempCanvas)
            chat.appendText("Вы подключены к серверу!\n")
            return socket
        } catch (e: Exception) {
            chat.appendText("Ошибка: Не удалось подключиться.\n")
            return null
        }
    }

    private fun startListening(chat: TextArea, gameCanvas: Canvas, tempCanvas: Canvas) {
        val currentSocket = socket ?: return
        val input = Scanner(currentSocket.getInputStream())
        val eventHandler = ProcessEvent(controller, chat, gameCanvas, tempCanvas)

        Thread {
            try {
                while (input.hasNextLine()) {
                    val message = input.nextLine()
                    eventHandler.processMessage(message)
                }
            } catch (e: Exception) {
                Platform.runLater {
                    chat.appendText("Соединение разорвано.\n")
                }
            } finally {
                Platform.runLater {
                    chat.appendText("Соединение с сервером потеряно.\n")
                    controller.onDisconnect()
                }
                socket?.close()
                socket = null
            }
        }.start()
    }
}
