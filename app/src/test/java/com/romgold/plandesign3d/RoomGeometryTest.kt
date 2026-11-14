package com.romgold.plandesign3d

import com.romgold.plandesign3d.domain.*
import org.junit.Assert.*
import org.junit.Test

class RoomGeometryTest {
    @Test fun rectangleCreatesFourWallsWithExactMillimetreLengths() {
        val room = RoomPlan.rectangle(5230, 4180)
        assertEquals(4, room.walls.size)
        assertEquals(5230.0, room.walls[0].lengthMm, 0.001)
        assertEquals(4180.0, room.walls[1].lengthMm, 0.001)
    }

    @Test fun wallRejectsZeroLength() {
        try {
            WallSegment(Point2(0, 0), Point2(0, 0))
            fail("Expected invalid wall")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun openingMustFitInsideWall() {
        val walls = RoomPlan.rectangle(3000, 2500).walls
        try {
            RoomPlan(walls, listOf(Opening(0, 2800, 500, 2000)))
            fail("Expected opening validation to fail")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun validDoorOpeningIsAccepted() {
        val room = RoomPlan(RoomPlan.rectangle(4000, 3000).walls,
            listOf(Opening(0, 900, 900, 2100, type = OpeningType.DOOR)))
        assertEquals(1, room.openings.size)
    }
}
