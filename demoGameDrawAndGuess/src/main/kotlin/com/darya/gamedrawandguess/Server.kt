package com.darya.gamedrawandguess

import com.darya.gamedrawandguess.model.GameEvent
import com.darya.gamedrawandguess.util.FileManager
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class Server {
    private val clients = CopyOnWriteArrayList<ClientHandler>()

    @Volatile private var currentPainter: ClientHandler? = null
    private var currentPainterIndex: Int = -1

    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private var currentRoundTask: ScheduledFuture<*>? = null

    private val ROUND_TIME_IN_SECONDS = 1000
    private val MAX_NUMBER_OF_SCORES = 100

    @Volatile private var isGameStarted = false

    private val fileManager = FileManager
    private var keyWord: String? = null
    private var roundStartTime: Long = 0

    private val drawingHistory = CopyOnWriteArrayList<GameEvent>()

    @Synchronized
    private fun broadcast(event: GameEvent, sender: ClientHandler? = null) {
        when (event) {
            is GameEvent.DrawShape -> { if (!event.isPreview) drawingHistory.add(event) }
            is GameEvent.Clear -> drawingHistory.clear()
            else -> {}
        }

        clients.forEach { client ->
            if (!(client == sender && event !is GameEvent.Chat))    // рисование отправлять или нет
                client.sendEvent(event)
        }

    }

    @Synchronized
    fun handleIncomingEvent(event: GameEvent, sender: ClientHandler) {
        if (event is GameEvent.Chat) {

            val message = event.message.substringAfter(":").trim()
            if (message.equals(keyWord, ignoreCase = true)) {

                if (sender.isGuess) return

                val timePassed = (System.currentTimeMillis() - roundStartTime) / 1000
                sender.score += (((ROUND_TIME_IN_SECONDS - timePassed) / ROUND_TIME_IN_SECONDS.toDouble()) * MAX_NUMBER_OF_SCORES).toInt()

                broadcast(GameEvent.Chat("Игрок ${sender.userName} отгадал слово!"))
                broadcast(GameEvent.UpdateScore(sender.id, sender.score))
                sender.isGuess = true

                val guessers = clients.count { it.isGuess }
                if (guessers == clients.size - 1)
                    forceStopRound()

                return
            }
        }
        broadcast(event)
    }

    @Synchronized
    fun addClient(client: ClientHandler) {
        drawingHistory.forEach { client.sendEvent(it) }
        clients.forEach { client.sendEvent(GameEvent.AddClient(it.id, it.userName, it.score)) }

        clients.add(client)
        broadcast(GameEvent.AddClient(client.id, client.userName, client.score))

        if (clients.size > 0 && !isGameStarted) {
            isGameStarted = true
            startRound()
        } else {
            val leftTime = (ROUND_TIME_IN_SECONDS - (System.currentTimeMillis() - roundStartTime) / 1000).toInt()
            client.sendEvent(GameEvent.RoundStart(currentPainter!!.userName, leftTime, null))
        }
    }

    @Synchronized
    fun removeClient(client: ClientHandler) {
        clients.remove(client)
        broadcast(GameEvent.RemoveClient(client.id, client.userName))

        if (client == currentPainter) {
            broadcast(GameEvent.Chat("CHAT:Художник отключился. Начинаем новый раунд..."))
            currentPainterIndex--
            forceStopRound()
        } else {
            currentPainterIndex = clients.indexOf(currentPainter)
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

        currentRoundTask?.cancel(false)

        currentPainter = setPainter()
        keyWord = fileManager.getNextWord()
        roundStartTime = System.currentTimeMillis()

        broadcast(GameEvent.Clear)
        clients.forEach { client ->
            client.isGuess = false
            val word = if (client == currentPainter) keyWord else null
            client.sendEvent(GameEvent.RoundStart(currentPainter?.userName ?: "", ROUND_TIME_IN_SECONDS, word))
        }

        currentRoundTask = scheduler.schedule({ stopRound() }, ROUND_TIME_IN_SECONDS.toLong(), TimeUnit.SECONDS)
    }

    private fun forceStopRound() {
        val canceled = currentRoundTask?.cancel(false)

        if (canceled == true || currentRoundTask == null) {
            println("Раунд завершен досрочно.")
            stopRound()
        }
    }

    private fun stopRound() {
        broadcast(GameEvent.RoundEnd(keyWord!!))
        keyWord = null
        scheduler.schedule({ startRound() }, 3, TimeUnit.SECONDS)
    }
}

fun main() {
    val server = Server()
    val serverSocket = java.net.ServerSocket(8080)
   // val serverSocket = java.net.ServerSocket(8080, 50, java.net.InetAddress.getByName("0.0.0.0"))
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
