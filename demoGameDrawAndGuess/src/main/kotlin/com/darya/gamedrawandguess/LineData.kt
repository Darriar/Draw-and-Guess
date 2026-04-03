package com.darya.gamedrawandguess

import javafx.scene.paint.Color

data class LineData(val x1: Double, val y1: Double, // Старт (в %)
                    val x2: Double, val y2: Double, // Конец (в %)
                    val color: Color,
                    val size: Double
)
