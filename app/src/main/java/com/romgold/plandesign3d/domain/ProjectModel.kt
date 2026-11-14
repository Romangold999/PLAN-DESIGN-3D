package com.romgold.plandesign3d.domain

import kotlin.math.abs

enum class FurnitureKind(val title: String, val widthMm: Int, val depthMm: Int) {
    SOFA("Диван", 2200, 900),
    BED("Кровать", 1600, 2000),
    TABLE("Стол", 1200, 700),
    WARDROBE("Шкаф", 1800, 600),
    KITCHEN("Кухонный блок", 2400, 600),
    BATH("Ванна", 1700, 700),
    TOILET("Унитаз", 400, 700),
    SINK("Раковина", 600, 500)
}

data class FurnitureItem(
    val id: String,
    val kind: FurnitureKind,
    val xMm: Int,
    val yMm: Int,
    val widthMm: Int = kind.widthMm,
    val depthMm: Int = kind.depthMm,
    val rotationDegrees: Int = 0
) {
    init {
        require(widthMm > 0 && depthMm > 0)
        require(rotationDegrees % 90 == 0)
    }
}

enum class FinishCategory { FLOOR, WALL }

data class FinishMaterial(
    val id: String,
    val name: String,
    val category: FinishCategory,
    val colorArgb: Long,
    val pricePerSquareMetre: Double
) {
    init { require(pricePerSquareMetre >= 0.0) }
}

data class ProjectDocument(
    val id: String,
    val name: String,
    val room: RoomPlan,
    val furniture: List<FurnitureItem> = emptyList(),
    val floorFinish: FinishMaterial = FinishMaterial("floor-neutral", "Нейтральный пол", FinishCategory.FLOOR, 0xFFE8E1D5, 1800.0),
    val wallFinish: FinishMaterial = FinishMaterial("wall-white", "Белая краска", FinishCategory.WALL, 0xFFF4F2EC, 650.0),
    val floorWastePercent: Double = 10.0,
    val wallWastePercent: Double = 10.0
) {
    init {
        require(name.isNotBlank())
        require(floorWastePercent in 0.0..100.0 && wallWastePercent in 0.0..100.0)
        furniture.forEach { item ->
            require(item.xMm >= 0 && item.yMm >= 0)
            val rotated = item.rotationDegrees % 180 != 0
            val effectiveWidth = if (rotated) item.depthMm else item.widthMm
            val effectiveDepth = if (rotated) item.widthMm else item.depthMm
            require(item.xMm + effectiveWidth <= room.boundsWidthMm + 1)
            require(item.yMm + effectiveDepth <= room.boundsDepthMm + 1)
        }
    }
}

data class RoomMetrics(
    val floorAreaM2: Double,
    val perimeterM: Double,
    val grossWallAreaM2: Double,
    val openingsAreaM2: Double,
    val netWallAreaM2: Double
)

fun RoomPlan.metrics(): RoomMetrics {
    val perimeter = walls.sumOf { it.lengthMm } / 1000.0
    val height = (walls.map { it.heightMm }.averageOrNull() ?: 2700.0) / 1000.0
    val floorArea = polygonAreaSquareMetres(walls)
    val gross = walls.sumOf { it.lengthMm * it.heightMm } / 1_000_000.0
    val openingsArea = openings.sumOf { it.widthMm.toDouble() * it.heightMm / 1_000_000.0 }
    return RoomMetrics(floorArea, perimeter, gross, openingsArea, (gross - openingsArea).coerceAtLeast(0.0))
}

private fun List<Int>.averageOrNull(): Double? = if (isEmpty()) null else average()

private fun polygonAreaSquareMetres(walls: List<WallSegment>): Double {
    var twiceArea = 0.0
    walls.forEach { wall -> twiceArea += wall.start.xMm.toDouble() * wall.end.yMm - wall.end.xMm.toDouble() * wall.start.yMm }
    return abs(twiceArea) / 2_000_000.0
}

val defaultFloorMaterials = listOf(
    FinishMaterial("tile", "Керамогранит", FinishCategory.FLOOR, 0xFFD9D5CE, 2200.0),
    FinishMaterial("laminate", "Ламинат", FinishCategory.FLOOR, 0xFFB58B61, 1500.0),
    FinishMaterial("parquet", "Паркет", FinishCategory.FLOOR, 0xFF93643F, 3200.0),
    FinishMaterial("concrete", "Бетон", FinishCategory.FLOOR, 0xFF9B9B96, 1100.0)
)

val defaultWallMaterials = listOf(
    FinishMaterial("paint", "Краска", FinishCategory.WALL, 0xFFF2F0E9, 650.0),
    FinishMaterial("plaster", "Декоративная штукатурка", FinishCategory.WALL, 0xFFD8C8B6, 1900.0),
    FinishMaterial("wallpaper", "Обои", FinishCategory.WALL, 0xFFD7DCE0, 900.0),
    FinishMaterial("stone", "Камень", FinishCategory.WALL, 0xFFAAA39A, 2800.0)
)
