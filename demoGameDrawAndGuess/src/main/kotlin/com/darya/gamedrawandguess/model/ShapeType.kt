package com.darya.gamedrawandguess.model

enum class ShapeType(val label: String,
                     val imagePath: String,
                     val isHandleDrawing: Boolean, // рисуется от руки сразу на gameCanvas
                     val isFloodFill: Boolean,
                     val isAction: Boolean)
{
    PENCIL("Карандаш",  "/images/pencil.png",  true, false, false),
    LINE("Линия",   "/images/line.png",false, false, false),
    RECT("Прямоугольник",  "/images/rect.png", false, false, false),
    OVAL("Овал",   "/images/oval.png",false, false, false),
    FLOODFILL("Заливка",   "/images/floodfill.png",false, true, false),
    ERASER("Ластик",  "/images/eraser.png",true, false, false),

    CLEAR("Очистить", "", false, false, true)
}