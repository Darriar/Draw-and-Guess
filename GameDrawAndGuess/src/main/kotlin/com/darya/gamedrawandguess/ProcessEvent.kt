package com.darya.gamedrawandguess

import com.darya.gamedrawandguess.drawingpart.Drawing
import com.darya.gamedrawandguess.model.GameEvent
import com.darya.gamedrawandguess.model.ShapeType
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
            Drawing.clearCanvas(tempCanvas)
            if (drawEvent.isPreview) {
                Drawing.drawShape(drawEvent, tempCanvas)
            } else {
                Drawing.drawShape(drawEvent, gameCanvas)
                if (drawEvent.shapeType != ShapeType.UNDO && drawEvent.shapeType != ShapeType.REDO)
                Drawing.addShapeToDrawingHistory(drawEvent)
            }
        }

        events[GameEvent.Chat::class] = { event ->
            val chatEvent = event as GameEvent.Chat
            chat.appendText("${chatEvent.message}\n")
        }

        events[GameEvent.RoundStart::class] = { event ->
            Drawing.clearDrawingHistory()
            Drawing.clearCanvas(gameCanvas)
            Drawing.clearCanvas(tempCanvas)
            val roundInfo = event as GameEvent.RoundStart
            controller.updateTimer(roundInfo.seconds)

            if (roundInfo.word != null) {
                controller.updateWord(roundInfo.word)
                controller.setDrawingMode(true)
            } else {
                controller.setCurrentPainter(roundInfo.painterName)
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