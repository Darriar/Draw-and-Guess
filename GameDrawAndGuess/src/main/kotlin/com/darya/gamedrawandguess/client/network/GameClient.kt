package com.darya.gamedrawandguess.client.network

import com.darya.gamedrawandguess.client.ui.DrawController
import com.darya.gamedrawandguess.model.GameEvent
import javafx.application.Platform
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.PrintWriter
import java.net.Socket
import java.util.*

class GameClient(private val controller: DrawController) {
    private var socket: Socket? = null
    private var out: PrintWriter? = null

    fun connect(ip: String, port: Int, eventHandler: ProcessEvent): Boolean {
        try {
            socket = Socket(ip, port)
            out = PrintWriter(socket!!.getOutputStream(), true)
            startListening(eventHandler)
            return true
        } catch (e: Exception) {
            println("Ошибка GameClient.connect: Не удалось подключиться.\n")
            return false
        }
    }

    private fun startListening(eventHandler: ProcessEvent) {
        val currentSocket = socket ?: return
        val input = Scanner(currentSocket.getInputStream())

        Thread {
            try {
                while (input.hasNextLine()) {
                    val message = input.nextLine()
                    eventHandler.processMessage(message)
                }
            } catch (e: Exception) {
                Platform.runLater{ println("Соединение разорвано.\n") }
            } finally {
                Platform.runLater { controller.onDisconnect(true) }
                close()
            }
        }.apply { isDaemon = true }.start()
    }

    fun sendEvent(event: GameEvent) {
        out?.println(Json.encodeToString<GameEvent>(event))
    }

    private fun close() {
        socket?.close()
        socket = null
        out = null
    }
}
