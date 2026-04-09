@file:Suppress("PLUGIN_IS_NOT_ENABLED")

package com.darya.gamedrawandguess.model

import javafx.scene.paint.Color
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface GameEvent {
    @Serializable @SerialName("chat")
    data class Chat(val userName: String, val message: String): GameEvent

    @Serializable @SerialName("draw")
    data class Draw(val x1: Double, val y1: Double, // Старт (от 0 до 1)
                    val x2: Double, val y2: Double, // Конец (от 0 до 1)
                    val color: String,
                    val size: Double): GameEvent

    @Serializable @SerialName("round_start")
    data class RoundStart(val painterName: String,
                          val seconds: Int,
                          val word: String? = null) : GameEvent

    @Serializable @SerialName("round_end")
    data object RoundEnd: GameEvent

    @Serializable @SerialName("add_client")
    data class AddClient(val id: Int, val userName: String, val score: Int): GameEvent

    @Serializable @SerialName("remove_client")
    data class RemoveClient(val id: Int, val userName: String): GameEvent

    @Serializable @SerialName("update_score")
    data class UpdateScore(val id: Int, val score: Int): GameEvent

    @Serializable @SerialName("next_word")
    data class NextWord(val word: String): GameEvent

    @Serializable @SerialName("clear")
    data object Clear : GameEvent
}