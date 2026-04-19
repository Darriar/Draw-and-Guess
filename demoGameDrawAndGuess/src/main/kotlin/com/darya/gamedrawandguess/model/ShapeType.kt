package com.darya.gamedrawandguess.model

enum class ShapeType(val label: String,
                     val icon: String,
                     val isHandleDrawing: Boolean, // рисуется от руки сразу на gameCanvas
                     val isFloodFill: Boolean) // заливка
{
    LINE("Линия", "➖",  false, false),
    RECT("Прямоугольник", "⬛",  false, false),
    OVAL("Овал", "⚪",  false, false),
    FREEHAND("Карандаш", "✏️",  true, false),
    ERASER("Ластик", "🧼", true, false),
    FLOODFILL("Заливка", "🫗",  false, true);


    override fun toString(): String = "$icon $label"
}