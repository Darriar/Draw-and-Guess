package com.darya.gamedrawandguess

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
    private val ROUND_TIME_IN_SECONDS = 5
    private var isGameStarted = false

    fun broadcast(message: String, sender: ClientHandler?) {
        for (client in clients) {
                client.sendMessage(message)
        }
    }

    fun addClient(client: ClientHandler) {
        clients.add(client)

        if (clients.size > 0 && !isGameStarted) {
            isGameStarted = true
            startRound()
        }
    }

    fun removeClient(client: ClientHandler) {
        clients.remove(client)
        broadcast("CHAT:Игрок ${client.userName} покинул игру.", null)

        if (client.isDrawing) {
            broadcast("SYSTEM:Художник отключился. Начинаем новый раунд...", null)
            forceStopRound()
        }
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

    private fun startRound() {
        if (clients.isEmpty()) {
            isGameStarted = false
            return
        }

        currentRoundTask?.cancel(false)

        currentPainter = setPainter()
        giveRoot()

        val fileManager = FileManager
        val word = fileManager.getNextWord()
        broadcast("CLEAR", null)
        currentPainter?.sendMessage("NEXT_WORD:$word")
        broadcast("CURRENT_PAINTER:${currentPainter?.userName}", null)
        broadcast("ROUND_START:$ROUND_TIME_IN_SECONDS", null)

        /*scheduler.schedule({
            stopRound()
        }, ROUND_TIME_IN_SECONDS.toLong(), TimeUnit.SECONDS)*/
        currentRoundTask = scheduler.schedule({
            stopRound()
        }, ROUND_TIME_IN_SECONDS.toLong(), TimeUnit.SECONDS)
    }

    fun forceStopRound() {
        // true — прервать поток, если он занят (для простых задач ставим false)
        val canceled = currentRoundTask?.cancel(false)

        if (canceled == true || currentRoundTask == null) {
            println("Раунд завершен досрочно.")
            stopRound()
        }
    }

    private fun stopRound() {
        broadcast("ROUND_END", null)
        println("Сервер: Время вышло, раунд окончен")
        scheduler.schedule({
            startRound()
        }, 3, TimeUnit.SECONDS)
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