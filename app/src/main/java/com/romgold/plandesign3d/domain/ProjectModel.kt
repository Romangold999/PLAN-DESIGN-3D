package com.romgold.plandesign3d.domain

import kotlin.math.abs
import kotlin.math.sqrt

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
    val wallWastePercent: Double = 10.0,
    val surfaceLayout: SurfaceLayout = SurfaceLayout(),
    val annotations: List<PlanAnnotation> = emptyList(),
    val workRates: WorkRates = WorkRates()
) {
    init {
        require(name.isNotBlank())
        require(floorWastePercent in 0.0..100.0 && wallWastePercent in 0.0..100.0)
        annotations.forEach { annotation ->
            require(annotation.xMm <= room.boundsWidthMm && annotation.yMm <= room.boundsDepthMm)
        }
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


data class FurnitureClearance(
    val first: FurnitureItem,
    val second: FurnitureItem,
    val distanceMm: Int
)

/**
 * Finds non-overlapping furniture pairs with a narrow gap.
 * Rectangles are axis-aligned; quarter-turn rotations swap width and depth.
 */
fun findNarrowFurniturePassages(
    furniture: List<FurnitureItem>,
    minimumClearanceMm: Int = 600
): List<FurnitureClearance> {
    require(minimumClearanceMm >= 0)
    return buildList {
        furniture.forEachIndexed { index, first ->
            furniture.drop(index + 1).forEach { second ->
                val firstRotated = first.rotationDegrees % 180 != 0
                val secondRotated = second.rotationDegrees % 180 != 0
                val firstWidth = if (firstRotated) first.depthMm else first.widthMm
                val firstDepth = if (firstRotated) first.widthMm else first.depthMm
                val secondWidth = if (secondRotated) second.depthMm else second.widthMm
                val secondDepth = if (secondRotated) second.widthMm else second.depthMm

                val overlapX = first.xMm < second.xMm + secondWidth &&
                    first.xMm + firstWidth > second.xMm
                val overlapY = first.yMm < second.yMm + secondDepth &&
                    first.yMm + firstDepth > second.yMm
                if (overlapX && overlapY) return@forEach

                val gapX = maxOf(
                    first.xMm - (second.xMm + secondWidth),
                    second.xMm - (first.xMm + firstWidth),
                    0
                )
                val gapY = maxOf(
                    first.yMm - (second.yMm + secondDepth),
                    second.yMm - (first.yMm + firstDepth),
                    0
                )
                val distance = sqrt(gapX.toDouble() * gapX + gapY.toDouble() * gapY).toInt()
                if (distance < minimumClearanceMm) {
                    add(FurnitureClearance(first, second, distance))
                }
            }
        }
    }.sortedBy { it.distanceMm }
}


data class FurnitureWallClearance(
    val furniture: FurnitureItem,
    val wallIndex: Int,
    val distanceMm: Int
)

/** Estimates the shortest clearance from each furniture footprint to the room's wall faces. */
fun findFurnitureWallClearances(
    room: RoomPlan,
    furniture: List<FurnitureItem>,
    minimumClearanceMm: Int = 600
): List<FurnitureWallClearance> {
    require(minimumClearanceMm >= 0)
    val minX = room.walls.minOf { minOf(it.start.xMm, it.end.xMm) }
    val minY = room.walls.minOf { minOf(it.start.yMm, it.end.yMm) }
    fun pointSegmentDistance(px: Double, py: Double, ax: Double, ay: Double, bx: Double, by: Double): Double {
        val dx = bx - ax
        val dy = by - ay
        val lengthSquared = dx * dx + dy * dy
        if (lengthSquared == 0.0) return hypot(px - ax, py - ay)
        val t = (((px - ax) * dx + (py - ay) * dy) / lengthSquared).coerceIn(0.0, 1.0)
        return hypot(px - (ax + t * dx), py - (ay + t * dy))
    }
    fun pointInRect(px: Double, py: Double, left: Double, top: Double, right: Double, bottom: Double) =
        px >= left && px <= right && py >= top && py <= bottom
    fun orientation(ax: Double, ay: Double, bx: Double, by: Double, cx: Double, cy: Double) =
        (bx - ax) * (cy - ay) - (by - ay) * (cx - ax)
    fun segmentsCross(ax: Double, ay: Double, bx: Double, by: Double, cx: Double, cy: Double, dx: Double, dy: Double): Boolean {
        val o1 = orientation(ax, ay, bx, by, cx, cy)
        val o2 = orientation(ax, ay, bx, by, dx, dy)
        val o3 = orientation(cx, cy, dx, dy, ax, ay)
        val o4 = orientation(cx, cy, dx, dy, bx, by)
        fun onSegment(px: Double, py: Double, qx: Double, qy: Double, rx: Double, ry: Double) =
            qx >= minOf(px, rx) && qx <= maxOf(px, rx) &&
                qy >= minOf(py, ry) && qy <= maxOf(py, ry)
        if (((o1 > 0.0 && o2 < 0.0) || (o1 < 0.0 && o2 > 0.0)) &&
            ((o3 > 0.0 && o4 < 0.0) || (o3 < 0.0 && o4 > 0.0))) return true
        return (o1 == 0.0 && onSegment(ax, ay, cx, cy, bx, by)) ||
            (o2 == 0.0 && onSegment(ax, ay, dx, dy, bx, by)) ||
            (o3 == 0.0 && onSegment(cx, cy, ax, ay, dx, dy)) ||
            (o4 == 0.0 && onSegment(cx, cy, bx, by, dx, dy))
    }
    fun segmentRectDistance(ax: Double, ay: Double, bx: Double, by: Double,
                            left: Double, top: Double, right: Double, bottom: Double): Double {
        if (pointInRect(ax, ay, left, top, right, bottom) || pointInRect(bx, by, left, top, right, bottom)) return 0.0
        val corners = listOf(left to top, right to top, right to bottom, left to bottom)
        for (i in corners.indices) {
            val a = corners[i]
            val b = corners[(i + 1) % corners.size]
            if (segmentsCross(ax, ay, bx, by, a.first, a.second, b.first, b.second)) return 0.0
        }
        val cornerDistance = corners.minOf { pointSegmentDistance(it.first, it.second, ax, ay, bx, by) }
        fun pointRectDistance(px: Double, py: Double): Double {
            val dx = maxOf(left - px, 0.0, px - right)
            val dy = maxOf(top - py, 0.0, py - bottom)
            return hypot(dx, dy)
        }
        return minOf(cornerDistance, pointRectDistance(ax, ay), pointRectDistance(bx, by))
    }

    buildList {
        furniture.forEach { item ->
            val rotated = item.rotationDegrees % 180 != 0
            val width = if (rotated) item.depthMm else item.widthMm
            val depth = if (rotated) item.widthMm else item.depthMm
            val left = item.xMm.toDouble()
            val top = item.yMm.toDouble()
            val right = left + width
            val bottom = top + depth
            val candidates = mutableListOf<FurnitureWallClearance>()
            room.walls.forEachIndexed { wallIndex, wall ->
                val dx = (wall.end.xMm - wall.start.xMm).toDouble()
                val dy = (wall.end.yMm - wall.start.yMm).toDouble()
                val length = hypot(dx, dy)
                val ux = dx / length
                val uy = dy / length
                val doorIntervals = room.openings.filter {
                    it.wallIndex == wallIndex && it.type == OpeningType.DOOR
                }.sortedBy { it.offsetMm }
                val solidRanges = mutableListOf<Pair<Double, Double>>()
                var cursor = 0.0
                doorIntervals.forEach { door ->
                    if (door.offsetMm > cursor) solidRanges += cursor to door.offsetMm.toDouble()
                    cursor = (door.offsetMm + door.widthMm).toDouble()
                }
                if (cursor < length) solidRanges += cursor to length
                solidRanges.forEach { (from, to) ->
                    val ax = wall.start.xMm - minX + ux * from
                    val ay = wall.start.yMm - minY + uy * from
                    val bx = wall.start.xMm - minX + ux * to
                    val by = wall.start.yMm - minY + uy * to
                    val centerlineDistance = segmentRectDistance(ax, ay, bx, by, left, top, right, bottom)
                    val clearance = (centerlineDistance - wall.thicknessMm / 2.0).toInt()
                    if (clearance < minimumClearanceMm) {
                        candidates += FurnitureWallClearance(item, wallIndex, clearance)
                    }
                }
            }
            candidates.minByOrNull { it.distanceMm }?.let(::add)
        }
    }.sortedBy { it.distanceMm }
}
