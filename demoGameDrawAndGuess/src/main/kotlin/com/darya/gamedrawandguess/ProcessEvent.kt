package com.darya.gamedrawandguess

import com.darya.gamedrawandguess.drawingpart.Drawing
import com.darya.gamedrawandguess.model.GameEvent
import com.darya.gamedrawandguess.ui.DrawController
import javafx.application.Platform
import javafx.scene.canvas.Canvas
import javafx.scene.control.TextArea
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass


class ProcessEvent(private val controller: DrawController,
                   private val chat: TextArea,
                   private val gameCanvas: Canvas,
                   private val tempCanvas: Canvas) {

    private val events = mutableMapOf<KClass<out GameEvent>, (GameEvent) -> Unit>()

    init {
        setUpEventsMap()
    }

    private fun setUpEventsMap() {
        events[GameEvent.DrawShape::class] = { event ->
            val drawEvent = event as GameEvent.DrawShape
            Drawing.clearTempCanvas(tempCanvas)
            if (drawEvent.isPreview) {
                Drawing.drawShape(drawEvent, tempCanvas)
            } else {
                Drawing.drawShape(drawEvent, gameCanvas)
                controller.addLineToDrawingHistory(drawEvent)
            }
        }

        events[GameEvent.Chat::class] = { event ->
            val chatEvent = event as GameEvent.Chat
            chat.appendText("${chatEvent.message}\n")
        }

        events[GameEvent.Clear::class] = {
            Drawing.clearGameCanvasToWhite(gameCanvas)
            Drawing.clearTempCanvas(tempCanvas)
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

        events[GameEvent.RoundEnd::class] = {event ->
            val word = event as GameEvent.RoundEnd
            controller.updateWord(word.keyWord)
            controller.blockCanvas()
            controller.stopTimer()
        }

        events[GameEvent.UpdateScore::class] = { event ->
            val scoreData = event as GameEvent.UpdateScore
            controller.updatePlayerScore(scoreData.id, scoreData.score.toString())  // испр стринг
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
}