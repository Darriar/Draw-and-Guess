package com.darya.gamedrawandguess.client.ui

import com.darya.gamedrawandguess.DrawApplication
import com.darya.gamedrawandguess.client.drawingpart.Drawing
import com.darya.gamedrawandguess.client.network.ProcessEvent
import com.darya.gamedrawandguess.client.network.GameClient
import com.darya.gamedrawandguess.model.GameEvent
import com.darya.gamedrawandguess.model.PlayerInfo
import com.darya.gamedrawandguess.model.ShapeType
import com.darya.gamedrawandguess.util.UIUtils
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.stage.Stage
import javafx.util.Duration
import java.io.PrintWriter
import java.net.Socket
import kotlin.math.abs

class DrawController {
    @FXML
    private lateinit var gameCanvas: Canvas
    @FXML
    private lateinit var tempCanvas: Canvas
    @FXML
    private lateinit var canvasContainer: StackPane
    @FXML
    private lateinit var colorPicker: ColorPicker
    @FXML
    private lateinit var sizeSlider: Slider
    @FXML
    private lateinit var messageTextField: TextField
    @FXML
    private lateinit var chatTextArea: TextArea
    @FXML
    private lateinit var timerLabel: Label
    @FXML
    private lateinit var statusLabel: Label
    @FXML
    private lateinit var bottomHBox: HBox
    @FXML
    private lateinit var toolsVBox: VBox
    @FXML
    private lateinit var toolsPane: VBox
    @FXML
    private lateinit var playersPane: VBox
    @FXML
    private lateinit var clearBtn: Button
    @FXML
    private lateinit var undoBtn: Button
    @FXML
    private lateinit var redoBtn: Button
    @FXML
    private lateinit var backBtn: Button

    private lateinit var gc: GraphicsContext
    private var currentTool: ShapeType = ShapeType.PENCIL
    private var userName: String = ""
    private var timeLine: Timeline? = null
    private var playersInfo =  FXCollections.observableArrayList<PlayerInfo>()
    private var gameClient: GameClient? = null

    @FXML
    fun initialize() {
        Init.initCanvas(gameCanvas, tempCanvas, canvasContainer)
        Init.initSizeSlider(sizeSlider)
        Init.initColorPicker(colorPicker)
        Init.configureButton(backBtn, imagePath = "/images/back.png")
        Init.initToolButtons(toolsVBox, undoBtn, redoBtn) { selectedType -> currentTool = selectedType }
        gc = Init.initGraphicContext(gameCanvas, sizeSlider, colorPicker)
        playersInfo.addListener(ListChangeListener {
            val nodes = playersInfo.map { createPlayerRow(it) }
            playersPane.children.remove(1, playersPane.children.size)
            playersPane.children.addAll(nodes)
        })
    }

    fun attemptConnection(ip: String, port: Int): Boolean {
        gameClient = GameClient(this)
        val eventHandler = ProcessEvent(this, chatTextArea, gameCanvas, tempCanvas)
        if (!gameClient!!.connect(ip, port, eventHandler)) return false

        Drawing.setupDrawingEvents(gameCanvas, tempCanvas, colorPicker, sizeSlider, clearBtn, undoBtn, redoBtn, { currentTool }, { event -> gameClient?.sendEvent(event) })
        return true
    }

    fun setUserName(name: String) {
        userName = name
        val joinEvent = GameEvent.AddClient(id = 0, userName = userName, score = 0)
        gameClient?.sendEvent(joinEvent)
    }

    fun setCurrentPainter(name: String) {
        statusLabel.text = "Рисует $name Угадайте слово!"
    }

    fun updateWord(word: String) {
        statusLabel.text = word
    }

    @FXML
    fun onSendBtnClick() {
        val text =messageTextField.text
        if (text.isNotEmpty()) {
            val message: GameEvent = GameEvent.Chat("$userName: $text")
            gameClient?.sendEvent(message)
            messageTextField.clear()
        }
    }

    private fun createPlayerRow(player: PlayerInfo): HBox {
        val nameLabel = Label(player.name).apply {
            styleClass.add("player-name")
            maxWidth = Double.MAX_VALUE
            HBox.setHgrow(this, Priority.ALWAYS)
        }

        val scoreLabel = Label().apply {
            textProperty().bind(player.scoreProperty.asString("%d PTS"))
            styleClass.add("player-score")
        }

        val playerColor = generateColorFromString(player.name)
        val avatar = Circle(16.0, playerColor)

        return HBox(12.0, avatar, nameLabel, scoreLabel).apply {
            alignment = Pos.CENTER_LEFT
            styleClass.add("player-row-container")
        }
    }

    private fun generateColorFromString(input: String): Color {
        val hash = input.hashCode()

        val hue = abs(hash % 360).toDouble()
        return Color.hsb(hue, 0.4, 0.9)
    }

    fun updatePlayerScore(id: Int, score: String) {
        val newScore = score.toIntOrNull() ?: 0
        val player = playersInfo.find { it.id == id }

        player?.scoreProperty?.set(newScore)

        playersInfo.sortByDescending { it.scoreProperty.get() }
    }

    fun createPlayerInfo(id: Int, userName: String, score: String) {
        val p = PlayerInfo(id, userName, score.toIntOrNull() ?: 0)
        playersInfo.add(p)
        playersInfo.sortByDescending { it.scoreProperty.get() }
    }

    fun removePlayerInfo(id: Int) {
        playersInfo.removeIf { it.id == id }
    }

    fun updateTimer(seconds: Int) {
        timeLine?.stop()
        var timeLeft = seconds - 1

        timerLabel.text = "Осталось: $seconds"

        timeLine = Timeline(KeyFrame(Duration.seconds(1.0), {
            timeLeft--
            timerLabel.text = "Осталось: $timeLeft"
            if (timeLeft <= 0) stopTimer()
        })).apply {
            cycleCount = seconds
            play()
        }
    }

    fun stopTimer() {
        timeLine?.stop()
        timerLabel.text = "Конец раунда!"
    }

    fun blockCanvas() {
        tempCanvas.disableProperty().set(true)
        toolsPane.isVisible = false
        playersPane.isVisible = true
        playersPane.isManaged = true
        bottomHBox.isVisible = false
    }

    fun setDrawingMode(isPainterMode: Boolean) {
        tempCanvas.isDisable = !isPainterMode
        messageTextField.isDisable = isPainterMode
        bottomHBox.isVisible = isPainterMode

        toolsPane.isVisible = isPainterMode
        toolsPane.isManaged = isPainterMode

        playersPane.isVisible = !isPainterMode
        playersPane.isManaged = !isPainterMode
    }

    fun onDisconnect(showAlert: Boolean) {
        val stage = gameCanvas.scene?.window as? Stage ?: return

        if (showAlert) {
            UIUtils.createAlert(
                Alert.AlertType.ERROR, "Связь потеряна", "Сервер отключился", "Вы будете возвращены в лобби.")
        }

        val fxmlLoader = FXMLLoader(DrawApplication::class.java.getResource("lobby-view.fxml"))
        val root = fxmlLoader.load<Parent>()

        val lobbyController = fxmlLoader.getController<LobbyController>()
        lobbyController.setUserName(userName)

        stage.scene = Scene(root)
    }


    fun onBackBtnClick() {
        gameClient?.sendEvent(GameEvent.BackToLobby)
        onDisconnect(false)
    }

}