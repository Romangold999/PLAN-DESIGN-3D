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
    val boundsWidthMm: Int get() = walls.maxOf { maxOf(it.start.xMm, it.end.xMm) } - walls.minOf { minOf(it.start.xMm, it.end.xMm) }
    val boundsDepthMm: Int get() = walls.maxOf { maxOf(it.start.yMm, it.end.yMm) } - walls.minOf { minOf(it.start.yMm, it.end.yMm) }
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
        openings.groupBy { it.wallIndex }.values.forEach { wallOpenings ->
            val sorted = wallOpenings.sortedBy { it.offsetMm }
            sorted.zipWithNext().forEach { (a, b) ->
                require(a.offsetMm + a.widthMm <= b.offsetMm) { "Openings on one wall must not overlap" }
            }
        }
        require(polygonAreaMm2(walls) > 0.0) { "Room polygon must have non-zero area" }
        for (i in walls.indices) {
            for (j in i + 1 until walls.size) {
                if (j == i + 1 || (i == 0 && j == walls.lastIndex)) continue
                require(!segmentsIntersect(walls[i], walls[j])) { "Room boundary must not self-intersect" }
            }
        }
    }

    companion object {
        fun fromPoints(points: List<Point2>, thicknessMm: Int = 120, heightMm: Int = 2700): RoomPlan {
            require(points.size >= 3) { "A room needs at least three corner points" }
            require(points.distinct().size == points.size) { "Room corner points must be unique" }
            val walls = points.indices.map { index ->
                WallSegment(points[index], points[(index + 1) % points.size], thicknessMm, heightMm)
            }
            return RoomPlan(walls)
        }

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

private fun polygonAreaMm2(walls: List<WallSegment>): Double {
    var sum = 0.0
    walls.forEach { sum += it.start.xMm.toDouble() * it.end.yMm - it.end.xMm.toDouble() * it.start.yMm }
    return kotlin.math.abs(sum) / 2.0
}

private fun segmentsIntersect(a: WallSegment, b: WallSegment): Boolean {
    fun cross(p: Point2, q: Point2, r: Point2): Long =
        (q.xMm - p.xMm).toLong() * (r.yMm - p.yMm) - (q.yMm - p.yMm).toLong() * (r.xMm - p.xMm)
    fun onSegment(p: Point2, q: Point2, r: Point2): Boolean =
        q.xMm in minOf(p.xMm, r.xMm)..maxOf(p.xMm, r.xMm) &&
            q.yMm in minOf(p.yMm, r.yMm)..maxOf(p.yMm, r.yMm)
    val o1 = cross(a.start, a.end, b.start)
    val o2 = cross(a.start, a.end, b.end)
    val o3 = cross(b.start, b.end, a.start)
    val o4 = cross(b.start, b.end, a.end)
    if ((o1 > 0 && o2 < 0 || o1 < 0 && o2 > 0) && (o3 > 0 && o4 < 0 || o3 < 0 && o4 > 0)) return true
    return (o1 == 0L && onSegment(a.start, b.start, a.end)) ||
        (o2 == 0L && onSegment(a.start, b.end, a.end)) ||
        (o3 == 0L && onSegment(b.start, a.start, b.end)) ||
        (o4 == 0L && onSegment(b.start, a.end, b.end))
}
