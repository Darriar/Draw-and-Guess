package com.darya.gamedrawandguess

import com.darya.gamedrawandguess.model.GameEvent
import com.darya.gamedrawandguess.util.FileManager
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
    private val ROUND_TIME_IN_SECONDS = 20
    private val MAX_NUMBER_OF_SCORES = 100
    private var isGameStarted = false
    private var isRoundStarted = false
    private val fileManager = FileManager
    private var keyWord: String = ""
    private var roundStartTime: Long = 0
    private var drawingHistory = mutableListOf<GameEvent.Draw>()


    private fun broadcast(event: GameEvent) {
        when (event) {
            is GameEvent.Draw -> drawingHistory.add(event)
            is GameEvent.Clear -> drawingHistory.clear()
            else -> {}
        }

        for (client in clients)
            client.sendEvent(event)
    }

    fun handleIncomingEvent(event: GameEvent, sender: ClientHandler) {
        if (event is GameEvent.Chat) {
            if (isRoundStarted && event.message.trim().equals(keyWord, ignoreCase = true)) {
                val timePassed = (System.currentTimeMillis() - roundStartTime) / 1000
                sender.score += (((ROUND_TIME_IN_SECONDS - timePassed) / ROUND_TIME_IN_SECONDS.toDouble()) * MAX_NUMBER_OF_SCORES).toInt()

                broadcast(GameEvent.Chat("","Игрок ${sender.userName} отгадал слово!"))
                return
            }
        }

        broadcast(event)
    }

    fun addClient(client: ClientHandler) {
        clients.add(client)

        broadcast(GameEvent.AddClient(client.id, client.userName, client.score))

        drawingHistory.forEach { client.sendEvent(it) }

        clients.forEach {
            client.sendEvent(GameEvent.AddClient(it.id, it.userName, it.score))
        }

        if (clients.size > 0 && !isGameStarted) {
            isGameStarted = true
            startRound()
        } else {
            val leftTime = (ROUND_TIME_IN_SECONDS - (System.currentTimeMillis() - roundStartTime) / 1000).toInt()
            client.sendEvent(GameEvent.RoundStart(currentPainter?.userName ?: "", leftTime, null))
        }
    }

    fun removeClient(client: ClientHandler) {
        clients.remove(client)
        broadcast(GameEvent.RemoveClient(client.id, client.userName))

        if (client == currentPainter) {
            broadcast(GameEvent.Chat("","CHAT:Художник отключился. Начинаем новый раунд..."))
            forceStopRound()
        }
    }

    private fun setPainter(): ClientHandler? {
        if (clients.isEmpty()) return null
        currentPainterIndex = (currentPainterIndex + 1) % clients.size
        return clients[currentPainterIndex]
    }

    private fun startRound() {
        if (clients.isEmpty()) {
            isGameStarted = false
            return
        }
        drawingHistory.clear()
        currentRoundTask?.cancel(false)
        currentPainter = setPainter()
        keyWord = fileManager.getNextWord()
        isRoundStarted = true   // было false и все работало
        roundStartTime = System.currentTimeMillis()

        broadcast(GameEvent.Clear)
        clients.forEach { client ->
            val word = if (client == currentPainter) keyWord else null
            client.sendEvent(GameEvent.RoundStart(currentPainter?.userName ?: "", ROUND_TIME_IN_SECONDS, word))
        }
        clients.forEach { broadcast(GameEvent.UpdateScore(it.id, it.score)) }




        currentRoundTask = scheduler.schedule({
            stopRound()
        }, ROUND_TIME_IN_SECONDS.toLong(), TimeUnit.SECONDS)
    }

    private fun forceStopRound() {
        val canceled = currentRoundTask?.cancel(false)

        if (canceled == true || currentRoundTask == null) {
            println("Раунд завершен досрочно.")
            stopRound()
        }
    }

    private fun stopRound() {
        broadcast(GameEvent.RoundEnd)
        isRoundStarted = false
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