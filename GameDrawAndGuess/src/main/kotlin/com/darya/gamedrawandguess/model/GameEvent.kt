@file:Suppress("PLUGIN_IS_NOT_ENABLED")

package com.darya.gamedrawandguess.model

import javafx.scene.paint.Color
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface GameEvent {
    @Serializable @SerialName("chat")
    data class Chat(val message: String): GameEvent

    @Serializable
    data class Point(
        val x: Double, val y: Double
    )

    @Serializable @SerialName("draw_shape")
    data class DrawShape(
        val shapeType: ShapeType,
        val points: List<Point> = listOf(),
        val color: String = Color.WHITE.toString(),
        val size: Double = 0.0,
        val isPreview: Boolean = false //  временный набросок
    ) : GameEvent

    @Serializable @SerialName("round_start")
    data class RoundStart(val painterName: String,
                          val seconds: Int,
                          val word: String? = null) : GameEvent

    @Serializable @SerialName("round_end")
    data class RoundEnd(val keyWord: String): GameEvent

    @Serializable @SerialName("add_client")
    data class AddClient(val id: Int, val userName: String, val score: Int): GameEvent

    @Serializable @SerialName("remove_client")
    data class RemoveClient(val id: Int, val userName: String): GameEvent

    @Serializable @SerialName("update_score")   // переимееновать в угада слово
    data class UpdateScore(val id: Int, val score: Int): GameEvent
}