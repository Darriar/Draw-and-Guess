package com.darya.gamedrawandguess

import java.io.PrintWriter
import java.net.Socket
import java.util.*

class ClientHandler(private val socket: Socket, private val server: Server): Thread()  {
    private val input = Scanner(socket.getInputStream())
    private val output = PrintWriter(socket.getOutputStream(), true)
    var playerName = ""
    var isDrawing = false

    override fun run() {
        if (input.hasNextLine()) {
            playerName = input.nextLine()
            println("Игрок $playerName подключился!")
        }

        while (input.hasNextLine()) {
            val message = input.nextLine()
            server.broadcast(message, this)
        }
        server.removeClient(this)
        socket.close()
    }

    fun sendMessage(message: String) {
        output.println(message)
    }
}