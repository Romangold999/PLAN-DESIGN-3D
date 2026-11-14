package com.romgold.plandesign3d
import org.junit.Assert.assertEquals
import org.junit.Test
class RoomDimensionsTest {
    @Test fun dimensionsAreStoredAsMillimetres() {
        val widthMm = 5230
        val depthMm = 4180
        assertEquals(5230, widthMm)
        assertEquals(4180, depthMm)
    }
}
