package com.darya.gamedrawandguess

import com.darya.gamedrawandguess.drawingpart.DrawingHistory
import com.darya.gamedrawandguess.model.LineData
import com.darya.gamedrawandguess.ui.DrawController
import javafx.application.Platform
import javafx.scene.canvas.Canvas
import javafx.scene.control.TextArea
import javafx.scene.paint.Color
import javafx.scene.shape.StrokeLineCap
import java.net.InetAddress
import java.net.Socket
import java.util.*

class ToServer(private val controller: DrawController) {

    private var socket: Socket? = null

    fun connect(chat: TextArea, gameCanvas: Canvas): Socket? {
        try {
            val localhost = InetAddress.getLocalHost().hostAddress
            println(localhost)
            socket = Socket("localhost", 8080)     // 10.177.142.105
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

        Thread {
            try {
                while (input.hasNextLine()) {
                    val message = input.nextLine()

                    Platform.runLater {
                        processMessage(chat, message, gameCanvas)
                    }
                }
            } catch (e: Exception) {
                Platform.runLater {
                    chat.appendText("Система: Соединение разорвано.\n")
                }
            }
        }.start()
    }

    private fun processMessage(chat: TextArea, message: String, gameCanvas: Canvas) {
        try {
            when {
                message.startsWith("CHAT:") -> {
                    chat.appendText("${message.substringAfter(":")}\n")
                }
                message.startsWith("DRAW:") -> {
                    drawFromNetwork(message, gameCanvas)
                }
                message == "CLEAR" -> {
                    gameCanvas.graphicsContext2D.clearRect(0.0, 0.0, gameCanvas.width, gameCanvas.height)
                }
                message.startsWith("ROUND_START:") -> {
                    val seconds = message.substringAfter(":").toInt()
                    controller.updateTimer(seconds)
                }
                message == "ROUND_END" -> {
                    controller.stopTimer()
                    controller.updatePlayersInfo()
                }
                message.startsWith("NEXT_WORD:") -> {
                    val word = message.substringAfter(":")
                    controller.updateWord(word)
                }
                message.startsWith("ADD_CLIENT:") -> {
                    val info = message.substringAfter(":").split(",") // client.id, client.userName, client.score
                    controller.createPlayerInfo(id = info[0].toInt(), userName =  info[1], score =  info[2])
                    chat.appendText("Игрок ${info[1]} присоединился к игре\n")
                }
                message.startsWith("REMOVE_CLIENT:") -> {
                    val info = message.substringAfter(":").split(",") // client.id, client.userName
                    controller.removePlayerInfo(info[0].toInt())
                    chat.appendText("Игрок ${info[1]} покинул игру\n")
                }
                message == "SET_DEFAULT_MODE" -> {
                    controller.setMode(false)
                }
                message == "SET_PAINTER_MODE" -> {
                    controller.setMode(true)
                }
                message.startsWith("CURRENT_PAINTER:") -> {
                    val painterName = message.substringAfter(":")
                    controller.setCurrentPainter(painterName)
                }
                message.startsWith("UPDATE_SCORE:") -> {
                    val info = message.substringAfter(":").split(",")   // client.id, client.score
                    controller.updatePlayerScore(id = info[0].toInt(), score = info[1])
                }
            }
        } catch (e: Exception) {
            println("Ошибка парсинга сообщения: $message")
        }
    }

    private fun drawFromNetwork(message: String, gameCanvas: Canvas) {
        val data = message.substringAfter(":").split(",")
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

        val drawingHistory = DrawingHistory
        val line = LineData(lastX, lastY, currentX, currentY, color, size)
        drawingHistory.add(line)
    }
}