package com.darya.gamedrawandguess.drawingpart

import com.darya.gamedrawandguess.model.LineData

object DrawingHistory: Iterable<LineData> {
    private val drawHistory = mutableListOf<LineData>()

    fun add(data: LineData) {
        drawHistory.add(data)
    }

    fun clear() {
        drawHistory.clear()
    }

    override fun iterator(): Iterator<LineData> {
        return drawHistory.iterator()
    }
}