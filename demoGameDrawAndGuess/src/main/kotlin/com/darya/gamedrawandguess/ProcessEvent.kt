package com.darya.gamedrawandguess

import com.darya.gamedrawandguess.model.GameEvent
import com.darya.gamedrawandguess.ui.DrawController
import javafx.application.Platform
import javafx.scene.canvas.Canvas
import javafx.scene.control.TextArea
import javafx.scene.paint.Color
import javafx.scene.shape.StrokeLineCap
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass


class ProcessEvent(private val controller: DrawController,
                   private val chat: TextArea,
                   private val gameCanvas: Canvas) {

    private val events = mutableMapOf<KClass<out GameEvent>, (GameEvent) -> Unit>()

    init {
        setUpEventsMap()
    }

    private fun setUpEventsMap() {
        events[GameEvent.Draw::class] = {event ->
            val drawEvent = event as GameEvent.Draw
            drawFromNetwork(drawEvent, gameCanvas)
            controller.addLineToDrawingHistory(drawEvent)
        }

        events[GameEvent.Chat::class] = { event ->
            val chatEvent = event as GameEvent.Chat
            chat.appendText("${chatEvent.message}\n")
        }

        events[GameEvent.Clear::class] = {
            gameCanvas.graphicsContext2D.clearRect(0.0, 0.0, gameCanvas.width, gameCanvas.height)
            controller.clearDrawingHistory()
        }

        events[GameEvent.RoundStart::class] = { event ->
            val roundInfo = event as GameEvent.RoundStart
            controller.updateTimer(roundInfo.seconds)
            controller.setCurrentPainter(roundInfo.painterName)
            if (roundInfo.word != null) {
                controller.updateWord(roundInfo.word)
                controller.setDrawingMode(true)
            } else {
                controller.updateWord("???")
                controller.setDrawingMode(false)
            }
        }

        events[GameEvent.RoundEnd::class] = {
            controller.stopTimer()
            controller.updatePlayersInfo()
        }

        events[GameEvent.UpdateScore::class] = { event ->
            val scoreData = event as GameEvent.UpdateScore
            controller.updatePlayerScore(scoreData.id, scoreData.score.toString())  // испр стринг
        }

        events[GameEvent.NextWord::class] = { event ->
            val nextWord = event as GameEvent.NextWord
            controller.updateWord(nextWord.word)
        }

        events[GameEvent.AddClient::class] = { event ->
            val client = event as GameEvent.AddClient
            controller.createPlayerInfo(client.id, client.userName, client.score.toString())    // не строка
        }

        events[GameEvent.RemoveClient::class] = { event ->
            val client = event as GameEvent.RemoveClient
            controller.removePlayerInfo(client.id)
            chat.appendText("Игрок ${client.userName} покинул игру\n")
        }
    }

    fun processMessage(jsonString: String) {
        try {
            val event = Json.decodeFromString<GameEvent>(jsonString)

            Platform.runLater {
                events[event::class]?.invoke(event)
            }
        }catch (e: Exception) {
            println("Ошибка парсинга JSON: ${e.message}")
        }
    }

    private fun drawFromNetwork(line: GameEvent.Draw, gameCanvas: Canvas) {
        val gc = gameCanvas.graphicsContext2D
        gc.stroke = Color.web(line.color)
        gc.lineWidth = line.size
        gc.lineCap = StrokeLineCap.ROUND

        gc.strokeLine(line.x1 * gameCanvas.width, line.y1 * gameCanvas.height,
            line.x2 * gameCanvas.width, line.y2 * gameCanvas.height)
    }


}