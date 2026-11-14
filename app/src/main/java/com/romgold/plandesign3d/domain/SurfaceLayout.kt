package com.romgold.plandesign3d.domain

import kotlin.math.ceil

enum class SurfacePattern(val title: String) {
    STRAIGHT("Прямая"), GRID("Сетка"), BRICK("Вразбежку"), HERRINGBONE("Английская ёлочка"), FRENCH_HERRINGBONE("Французская ёлочка")
}

data class SurfaceLayout(
    val tileWidthMm: Int = 600,
    val tileHeightMm: Int = 600,
    val groutMm: Int = 2,
    val rotationDegrees: Int = 0,
    val wastePercent: Double = 10.0,
    val pattern: SurfacePattern = SurfacePattern.STRAIGHT
) {
    init {
        require(tileWidthMm in 50..3000 && tileHeightMm in 50..3000)
        require(groutMm in 0..30)
        require(rotationDegrees % 90 == 0)
        require(wastePercent in 0.0..100.0)
    }

    fun piecesForArea(areaM2: Double): Int {
        val oneTileArea = (tileWidthMm.toDouble() * tileHeightMm) / 1_000_000.0
        val areaWithWaste = areaM2 * (1.0 + wastePercent / 100.0)
        return ceil(areaWithWaste / oneTileArea).toInt().coerceAtLeast(0)
    }
}
