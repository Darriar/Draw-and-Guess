package com.darya.gamedrawandguess

import com.darya.gamedrawandguess.model.GameEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.PrintWriter
import java.net.Socket
import java.util.*

class ClientHandler(private val socket: Socket, private val server: Server): Thread()  {
    private val output = PrintWriter(socket.getOutputStream(), true)
    val id: Int = socket.hashCode()
    var isGuess = false
    var score: Int = 0
    var userName: String = ""

    override fun run() {
        try {
            val input = Scanner(socket.getInputStream())
            userName = input.nextLine()
            server.addClient(this)
            println("Игрок $userName подключился!")

            while (input.hasNextLine()) {
                val jsonMessage = input.nextLine()

                val event = Json.decodeFromString<GameEvent>(jsonMessage)
                server.handleIncomingEvent(event, this)

            }
        } catch (e: Exception) {
            println("Ошибка обработчика клиента $userName:")
        } finally {
            server.removeClient(this)
            socket.close()
        }

    }

    fun sendEvent(event: GameEvent) {
        output.println(Json.encodeToString(event))
        println(event.javaClass)
    }
}
