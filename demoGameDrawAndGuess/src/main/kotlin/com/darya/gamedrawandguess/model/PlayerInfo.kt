package com.darya.gamedrawandguess.model

import javafx.beans.property.SimpleIntegerProperty

class PlayerInfo(
    val id: Int,
    val name: String,
    val initialScore: Int = 0)
{
    val scoreProperty = SimpleIntegerProperty(initialScore)

    var score: Int
        get() = scoreProperty.get()
        set(value) = scoreProperty.set(value)
}
