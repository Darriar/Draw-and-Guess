package com.darya.gamedrawandguess

import javafx.scene.canvas.Canvas
import javafx.scene.control.TextArea
import javafx.scene.paint.Color
import javafx.scene.shape.StrokeLineCap
import java.net.InetAddress
import java.net.Socket
import java.util.*

class ToServer {

    companion object {
        fun connect(chat: TextArea, gameCanvas: Canvas): Socket? {
            try {
                val localhost = InetAddress.getLocalHost().hostAddress
                println(localhost)
                val socket = Socket("localhost", 8080)     // 10.177.142.105
                startListening(chat, socket, gameCanvas)
                chat.appendText("Система: Вы подключены к серверу!\n")
                return socket
            } catch (e: Exception) {
                chat.appendText("Ошибка: Не удалось подключиться.\n")
                return null
            }
        }

        private fun processMessage(chat: TextArea, message: String, gameCanvas: Canvas) {
            try {
                when {
                    message.startsWith("CHAT:") -> {
                        chat.appendText(message.substring(5) + "\n")
                    }
                    message.startsWith("DRAW:") -> {
                        val data = message.removePrefix("DRAW").split(",")
                        val lastX = data[0].toDouble(); val lastY = data[1].toDouble()
                        val currentX = data[2].toDouble(); val currentY = data[3].toDouble()
                        val color = Color.web(data[4])
                        val size = data[5].toDouble()


                        val gc = gameCanvas.graphicsContext2D
                        gc.stroke = color
                        gc.lineWidth = size
                        gc.lineCap = StrokeLineCap.ROUND

                        gc.strokeLine(lastX * gameCanvas.width, lastY * gameCanvas.height,
                                      currentX * gameCanvas.width, currentY * gameCanvas.height)
                    }
                    message == "CLEAR" -> {
                        val gc = gameCanvas.graphicsContext2D
                        gc.clearRect(0.0, 0.0, gameCanvas.width, gameCanvas.height)
                    }
                }
            } catch (e: Exception) {
                println("Ошибка парсинга сообщения: $message")
            }
        }

        private fun startListening(chat: TextArea, socket: Socket, gameCanvas: Canvas) {
            val input = Scanner(socket.getInputStream())

            Thread {
                try {
                    while (input.hasNextLine()) {
                        val message = input.nextLine()

                        javafx.application.Platform.runLater {
                            processMessage(chat, message, gameCanvas)
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

// один рисует у другого при масштабировании все слетает так как не записывается в историю