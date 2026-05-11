package com.darya.gamedrawandguess.client.ui

import com.darya.gamedrawandguess.server.Server
import com.darya.gamedrawandguess.server.ClientHandler
import com.darya.gamedrawandguess.DrawApplication
import javafx.scene.Scene
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.layout.VBox
import javafx.stage.Stage
import java.net.*

class LobbyController {
    @FXML
    private lateinit var lobbyVBox: VBox

    private val roomLastSeen = mutableMapOf<String, Long>()
    private var userName: String = "Игрок"

    @FXML
    fun initialize() {
        startSearchingRooms()
        startCleanupTask()
    }

    fun setUserName(name: String) {
        this.userName = name
    }

    private fun startSearchingRooms() {
        Thread {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(8888))
                }
                val buffer = ByteArray(1024)
                println("Поиск комнат запущен на порту 8888...")

                while (!Thread.currentThread().isInterrupted) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet) // Ожидание пакета

                    val message = String(packet.data, 0, packet.length)
                    val parts = message.split("|")

                    if (parts.size == 3) {
                        val roomName = parts[0]
                        val ip = parts[1]
                        val port = parts[2].toInt()
                        val roomKey = "$ip:$port"
                        
                        if (!roomLastSeen.containsKey(roomKey)) {
                            Platform.runLater {
                                addRoomToUI(ip, roomName, port)
                            }
                        }

                        roomLastSeen[roomKey] = System.currentTimeMillis()
                    }
                }
            } catch (e: Exception) {
                println("Ошибка поиска комнат: ${e.message}")
            } finally {
                socket?.close()
                println("Поток поиска комнат остановлен.")
            }
        }.apply {
            isDaemon = true
        }.start()
    }

    private fun startCleanupTask() {
        Thread {
            while (true) {
                Thread.sleep(5000)
                val now = System.currentTimeMillis()

                val expiredRooms = roomLastSeen.filter { now - it.value > 10000 }.keys

                if (expiredRooms.isNotEmpty()) {
                    Platform.runLater {
                        expiredRooms.forEach { key ->
                            lobbyVBox.children.removeIf { node ->
                                (node as? Button)?.text?.contains(key) == true
                            }
                            roomLastSeen.remove(key)
                        }
                    }
                }
            }
        }.apply { isDaemon = true }.start()
    }

    private fun addRoomToUI(ip: String, name: String, port: Int) {
        val roomButton = Button("Подключиться к $name ($ip:$port)")
        roomButton.maxWidth = Double.MAX_VALUE
        roomButton.setOnAction {
            val fxmlLoader = FXMLLoader(DrawApplication::class.java.getResource("draw-view.fxml"))
            val mainRoot = fxmlLoader.load<Parent>()
            val drawController = fxmlLoader.getController<DrawController>()

            if (drawController.attemptConnection(ip, port)) {
                drawController.setUserName(userName)
                val stage = lobbyVBox.scene.window as Stage
                stage.scene = Scene(mainRoot)
            } else {
                println("Ошибка: не удалось подключиться к $ip:$port")
            }
        }
        lobbyVBox.children.add(roomButton)
    }

    @FXML
    fun onCreateLobbyClick() {
        val localhost = InetAddress.getLocalHost()
        if (localhost.isLoopbackAddress) {
            println("Вы не подключены к локальной сети")
            return
        }

        Thread {
            try {
                val server = Server()
                val serverSocket = ServerSocket(0)
                val port = serverSocket.localPort
                val ip = localhost.hostAddress

                startBroadcasting(ip, port)
                println("Сервер запущен на $ip:$port")

                while (true) {
                    val socket = serverSocket.accept()
                    ClientHandler(socket, server).start()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun startBroadcasting(ip: String, port: Int) {
        Thread {
            val socket = DatagramSocket()
            socket.broadcast = true
            val message = "Комната ${userName}|$ip|$port".toByteArray()

            while (true) {
                val packet = DatagramPacket(
                    message, message.size,
                    InetAddress.getByName("255.255.255.255"), 8888
                )
                socket.send(packet)
                Thread.sleep(3000)
            }
        }.apply { isDaemon = true }.start()
    }
}