package com.darya.gamedrawandguess

import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.TextArea
import java.net.InetAddress
import java.net.Socket
import java.util.*

class ToServer {

    companion object {
        fun connect(chat: TextArea, gc: GraphicsContext, gameCanvas: Canvas): Socket? {
            try {
                val localhost = InetAddress.getLocalHost().hostAddress
                println(localhost)
                val socket = Socket("10.177.142.105", 8080)
                startListening(chat, socket, gc, gameCanvas)
                chat.appendText("Система: Вы подключены к серверу!\n")
                return socket
            } catch (e: Exception) {
                chat.appendText("Ошибка: Не удалось подключиться.\n")
                return null
            }
        }

        private fun processMessage(chat: TextArea, message: String, gc: GraphicsContext,gameCanvas: Canvas) {
            try {
                when {
                    message.startsWith("CHAT:") -> {
                        chat.appendText(message.substring(5) + "\n")
                    }
                    message.startsWith("START:") -> {
                        val coords = message.substring(6).split(",")
                        gc.beginPath()
                        gc.moveTo(coords[0].toDouble(), coords[1].toDouble())

                    }
                    message.startsWith("DRAW:") -> {
                        val coords = message.substring(5).split(",")
                        gc.lineTo(coords[0].toDouble(), coords[1].toDouble())
                        gc.stroke()
                    }
                    message == "CLEAR" -> {
                        gc.clearRect(0.0, 0.0, gameCanvas.width, gameCanvas.height)
                    }
                }
            } catch (e: Exception) {
                println("Ошибка парсинга сообщения: $message")
            }
        }

        private fun startListening(chat: TextArea, socket: Socket, gc: GraphicsContext, gameCanvas: Canvas) {
            val input = Scanner(socket.getInputStream())

            Thread {
                try {
                    while (input.hasNextLine()) {
                        val message = input.nextLine()

                        javafx.application.Platform.runLater {
                            processMessage(chat, message, gc, gameCanvas)
                        }
                    }
                } catch (e: Exception) {
                    javafx.application.Platform.runLater {
                        chat.appendText("Система: Соединение разорвано: ${e.message}\n")
                    }
                }
            }.start()
        }
    }
}