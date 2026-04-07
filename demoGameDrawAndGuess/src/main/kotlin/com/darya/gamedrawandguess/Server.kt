package com.darya.gamedrawandguess

import com.darya.gamedrawandguess.model.LineData
import com.darya.gamedrawandguess.util.FileManager
import javafx.scene.paint.Color
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class Server {
    private val clients = CopyOnWriteArrayList<ClientHandler>()
    private var currentPainter: ClientHandler? = null
    private var currentPainterIndex: Int = -1
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private var currentRoundTask: ScheduledFuture<*>? = null
    private val ROUND_TIME_IN_SECONDS = 35
    private val MAX_NUMBER_OF_SCORES = 100
    private var isGameStarted = false
    private val fileManager = FileManager
    private var keyWord: String = ""
    private var roundStartTime: Long = 0
    private var drawingHistory = mutableListOf<LineData>()


    fun broadcast(message: String, sender: ClientHandler?) {
        if (message.startsWith("DRAW:")) {
            val line = ToServer.parseStringToLineData(message.substringAfter(":"))
            drawingHistory.add(line)
        } else if (message == "CLEAR")
            drawingHistory.clear()


        if (!checkWord(message, sender))
            for (client in clients)
                client.sendMessage(message)
    }

    private fun checkWord(message: String, sender: ClientHandler?): Boolean {
        if (message.startsWith("CHAT:")) {
            val answer = message.substringAfter(":").substringAfter(":").trim()
            if (answer.equals(keyWord, ignoreCase = true)) {
                broadcast("CHAT:Ирок ${sender!!.userName} отгадал слово!", null)

                val timePassedMillis = System.currentTimeMillis() - roundStartTime
                val secondsPassed = (timePassedMillis / 1000).toInt()
                sender.score += (((ROUND_TIME_IN_SECONDS - secondsPassed) / ROUND_TIME_IN_SECONDS.toDouble()) * MAX_NUMBER_OF_SCORES).toInt()
                println("Количество очков ${sender.userName}: ${sender.score}")
                return true
            }
        }
        return false
    }

    fun addClient(client: ClientHandler) {
        clients.add(client)
        broadcast("ADD_CLIENT:${client.id},${client.userName},${client.score}", null)
        drawingHistory.forEach { client.sendMessage("DRAW:${it.x1},${it.y1},${it.x2},${it.y2},${it.color},${it.size}") }
        if (clients.size > 0 && !isGameStarted) {
            isGameStarted = true
            startRound()
        }
    }

    fun removeClient(client: ClientHandler) {
        clients.remove(client)
        broadcast("REMOVE_CLIENT:${client.id},${client.userName}", null)

        if (client.isDrawing) {
            broadcast("CHAT:Художник отключился. Начинаем новый раунд...", null)
            forceStopRound()
        }
    }

    private fun startRound() {
        if (clients.isEmpty()) {
            isGameStarted = false
            return
        }
        drawingHistory.clear()
        currentRoundTask?.cancel(false)

        currentPainter = setPainter()
        giveRoot()
        keyWord = fileManager.getNextWord()

        broadcast("CLEAR", null)
        clients.forEach { it.sendMessage("UPDATE_SCORE:${it.id},${it.score}") }
        broadcast("SET_DEFAULT_MODE", null)
        currentPainter?.sendMessage("SET_PAINTER_MODE")
        currentPainter?.sendMessage("NEXT_WORD:$keyWord")
        broadcast("CURRENT_PAINTER:${currentPainter?.userName}", null)
        broadcast("ROUND_START:$ROUND_TIME_IN_SECONDS", null)
        roundStartTime = System.currentTimeMillis()

        currentRoundTask = scheduler.schedule({
            stopRound()
        }, ROUND_TIME_IN_SECONDS.toLong(), TimeUnit.SECONDS)
    }

    private fun setPainter(): ClientHandler? {
        if (clients.isEmpty()) return null
        currentPainterIndex = (currentPainterIndex + 1) % clients.size
        return clients[currentPainterIndex]
    }

    private fun giveRoot() {
        clients.forEach { it.isDrawing = false }
        currentPainter?.isDrawing = true
    }

    private fun forceStopRound() {
        val canceled = currentRoundTask?.cancel(false)

        if (canceled == true || currentRoundTask == null) {
            println("Раунд завершен досрочно.")
            stopRound()
        }
    }

    private fun stopRound() {
        broadcast("ROUND_END", null)
        scheduler.schedule({
            startRound()
        }, 3, TimeUnit.SECONDS) // жду 3 секунды перед след раундом
    }
}

fun main() {
    val server = Server()
    val serverSocket = java.net.ServerSocket(8080)
    println("Сервер запущен на порту 8080...")

    try {
        while (true) {
            val socket = serverSocket.accept()
            val handler = ClientHandler(socket, server)

            handler.start()
        }
    }catch (e: Exception) {
        println("Ошибка сервера: ${e.message}")
    } finally {
        serverSocket.close()
    }

}

// если 4ый нарисовал. вышел 2ой. опять 4ый будет или 6ой