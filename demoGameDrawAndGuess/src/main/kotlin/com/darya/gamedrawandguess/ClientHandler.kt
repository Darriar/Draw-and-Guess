package com.darya.gamedrawandguess

import com.darya.gamedrawandguess.model.GameEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.PrintWriter
import java.net.Socket
import java.util.*

class ClientHandler(private val socket: Socket, private val server: Server): Thread()  {
    private val input = Scanner(socket.getInputStream())
    private val output = PrintWriter(socket.getOutputStream(), true)
    val id: Int = socket.hashCode()
    var isGuess = false
    var score: Int = 0
    var userName: String = ""

    /*override fun run() {
        if (input.hasNextLine()) {
            userName = input.nextLine()
            server.addClient(this)
            println("Игрок $userName подключился!")
        }

        while (input.hasNextLine()) {
            val jsonMessage = input.nextLine()
            try {
                val event = Json.decodeFromString<GameEvent>(jsonMessage)
                server.handleIncomingEvent(event, this)
            } catch (e: Exception) {
                println("Ошибка парсинга от клиента $userName: $jsonMessage")
            }
        }
        server.removeClient(this)
        socket.close()
    }*/

    fun sendEvent(event: GameEvent) {
        output.println(Json.encodeToString(event))
    }
    override fun run() {
        try {
            println("Новое сырое соединение установлено!") // Увидим это в консоли сразу
            val inputStream = socket.getInputStream()
            val reader = inputStream.bufferedReader()

            val firstLine = reader.readLine()
            if (firstLine != null) {
                userName = firstLine
                server.addClient(this)
                println("Игрок $userName успешно опознан!")
            }

            while (true) {
                val jsonMessage = reader.readLine() ?: break
                val event = Json.decodeFromString<GameEvent>(jsonMessage)
                server.handleIncomingEvent(event, this)
            }
        } catch (e: Exception) {
            println("Ошибка в обработчике клиента: ${e.message}")
        } finally {
            server.removeClient(this)
            socket.close()
        }
    }
}