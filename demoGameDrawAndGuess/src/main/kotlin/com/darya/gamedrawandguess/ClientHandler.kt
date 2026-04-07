package com.darya.gamedrawandguess

import java.io.PrintWriter
import java.net.Socket
import java.util.*

class ClientHandler(private val socket: Socket, private val server: Server): Thread()  {
    private val input = Scanner(socket.getInputStream())
    private val output = PrintWriter(socket.getOutputStream(), true)
    val id: Int = socket.hashCode()
    var score: Int = 0
    var isDrawing = false
    var userName: String = ""

    override fun run() {
        if (input.hasNextLine()) {
            userName = input.nextLine()
            server.addClient(this)
            println("Игрок $userName подключился!")
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