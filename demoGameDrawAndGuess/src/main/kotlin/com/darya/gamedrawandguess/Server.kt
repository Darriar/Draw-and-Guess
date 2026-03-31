package com.darya.gamedrawandguess

import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

class Server {
    private val clients = CopyOnWriteArrayList<ClientHandler>()
    private var currentPainter: ClientHandler? = null
    private var timer: Timer? = null

    fun broadcast(message: String, sender: ClientHandler?) {
        for (client in clients) {
            //if (client != sender || !message.startsWith("CHAT:"))
                client.sendMessage(message)
        }
    }

    fun addClient(client: ClientHandler) {
        clients.add(client)
    }

    fun removeClient(client: ClientHandler) {
        clients.remove(client)
        broadcast("CHAT:Игрок ${client.playerName} покинул игру.", null)

        if (client.isDrawing) {
           // stopTimer()
            broadcast("SYSTEM:Художник отключился. Начинаем новый раунд...", null)
            //nextRound()
        }
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

            server.addClient(handler)
            handler.start()
        }
    }catch (e: Exception) {
        println("Ошибка сервера: ${e.message}")
    } finally {
        serverSocket.close()
    }

}