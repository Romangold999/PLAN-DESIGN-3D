package com.romgold.plandesign3d.domain

import kotlin.math.abs
import kotlin.math.hypot

/** All plan coordinates and dimensions are stored as integer millimetres. */
data class Point2(val xMm: Int, val yMm: Int)

data class WallSegment(
    val start: Point2,
    val end: Point2,
    val thicknessMm: Int = 120,
    val heightMm: Int = 2700
) {
    init {
        require(start != end) { "A wall must have non-zero length" }
        require(thicknessMm in 40..1000) { "Wall thickness must be 40..1000 mm" }
        require(heightMm in 100..10000) { "Wall height must be 100..10000 mm" }
    }

    val lengthMm: Double
        get() = hypot((end.xMm - start.xMm).toDouble(), (end.yMm - start.yMm).toDouble())
}

enum class OpeningType { DOOR, WINDOW }

data class Opening(
    val wallIndex: Int,
    val offsetMm: Int,
    val widthMm: Int,
    val heightMm: Int,
    val sillHeightMm: Int = 0,
    val type: OpeningType = OpeningType.DOOR
) {
    init {
        require(wallIndex >= 0)
        require(offsetMm >= 0)
        require(widthMm > 0)
        require(heightMm > 0)
        require(sillHeightMm >= 0)
    }
}

data class RoomPlan(
    val walls: List<WallSegment>,
    val openings: List<Opening> = emptyList()
) {
    init {
        require(walls.size >= 3) { "A room needs at least three walls" }
        require(walls.indices.all { index ->
            walls[index].end == walls[(index + 1) % walls.size].start
        }) { "Room walls must form one connected closed loop" }
        openings.forEach { opening ->
            require(opening.wallIndex in walls.indices) { "Opening references a missing wall" }
            require(opening.offsetMm + opening.widthMm <= walls[opening.wallIndex].lengthMm) {
                "Opening extends beyond its wall"
            }
            require(opening.sillHeightMm + opening.heightMm <= walls[opening.wallIndex].heightMm) {
                "Opening extends beyond wall height"
            }
        }
    }

    companion object {
        fun rectangle(widthMm: Int, depthMm: Int, thicknessMm: Int = 120, heightMm: Int = 2700): RoomPlan {
            require(widthMm > 0 && depthMm > 0)
            val a = Point2(0, 0)
            val b = Point2(widthMm, 0)
            val c = Point2(widthMm, depthMm)
            val d = Point2(0, depthMm)
            return RoomPlan(
                listOf(
                    WallSegment(a, b, thicknessMm, heightMm),
                    WallSegment(b, c, thicknessMm, heightMm),
                    WallSegment(c, d, thicknessMm, heightMm),
                    WallSegment(d, a, thicknessMm, heightMm)
                )
            )
        }
    }
}
