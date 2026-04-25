package com.darya.gamedrawandguess.model

enum class ShapeType(val label: String,
                     val icon: String,
                     val isHandleDrawing: Boolean, // рисуется от руки сразу на gameCanvas
                     val isFloodFill: Boolean) // заливка
{
    PENCIL("Карандаш", "✏️",  true, false),
    LINE("Линия", "➖",  false, false),
    RECT("Прямоугольник", "⬛",  false, false),
    OVAL("Овал", "⚪",  false, false),
    FLOODFILL("Заливка", "🫗",  false, true),
    ERASER("Ластик", "🧼", true, false);



    override fun toString(): String = "$icon $label"
}