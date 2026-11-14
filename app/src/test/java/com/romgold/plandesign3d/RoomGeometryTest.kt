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

    @Test fun customPolygonSupportsNonRectangularRooms() {
        val room = RoomPlan.fromPoints(listOf(Point2(0, 0), Point2(4000, 0), Point2(4000, 2500), Point2(2500, 2500), Point2(2500, 4000), Point2(0, 4000)))
        assertEquals(6, room.walls.size)
        assertEquals(13.75, room.metrics().floorAreaM2, 0.001)
    }

    @Test fun selfIntersectingRoomIsRejected() {
        try {
            RoomPlan.fromPoints(listOf(Point2(0, 0), Point2(4000, 3000), Point2(0, 3000), Point2(4000, 0)))
            fail("Expected self-intersection to fail")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun overlappingOpeningsAreRejected() {
        try {
            RoomPlan(RoomPlan.rectangle(4000, 3000).walls, listOf(
                Opening(0, 500, 1000, 2100),
                Opening(0, 1200, 900, 2100)
            ))
            fail("Expected overlapping openings to fail")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun validDoorOpeningIsAccepted() {
        val room = RoomPlan(RoomPlan.rectangle(4000, 3000).walls,
            listOf(Opening(0, 900, 900, 2100, type = OpeningType.DOOR)))
        assertEquals(1, room.openings.size)
    }
}
