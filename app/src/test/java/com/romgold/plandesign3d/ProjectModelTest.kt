package com.romgold.plandesign3d

import com.romgold.plandesign3d.domain.*
import com.romgold.plandesign3d.data.EstimateCsvExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectModelTest {
    @Test fun rectangleMetricsUseMillimetresAndReturnSquareMetres() {
        val metrics = RoomPlan.rectangle(5000, 4000).metrics()
        assertEquals(20.0, metrics.floorAreaM2, 0.001)
        assertEquals(18.0, metrics.perimeterM, 0.001)
        assertEquals(48.6, metrics.grossWallAreaM2, 0.001)
    }

    @Test fun openingAreaIsSubtractedFromNetWallArea() {
        val room = RoomPlan(RoomPlan.rectangle(5000, 4000).walls,
            listOf(Opening(0, 1000, 900, 2100, type = OpeningType.DOOR)))
        val metrics = room.metrics()
        assertEquals(1.89, metrics.openingsAreaM2, 0.001)
        assertEquals(metrics.grossWallAreaM2 - 1.89, metrics.netWallAreaM2, 0.001)
    }

    @Test fun surfaceLayoutCalculatesTileCountWithWaste() {
        val layout = SurfaceLayout(tileWidthMm = 600, tileHeightMm = 600, wastePercent = 10.0)
        assertEquals(62, layout.piecesForArea(20.0))
    }

    @Test fun estimateCsvIncludesMaterialsWorkAndTotal() {
        val project = ProjectDocument("test", "Test", RoomPlan.rectangle(5000, 4000))
        val csv = EstimateCsvExporter.encode(project)
        assertTrue(csv.contains("Наименование"))
        assertTrue(csv.contains("Монтаж пола"))
        assertTrue(csv.contains("ИТОГО"))
    }

    @Test fun defaultWorkRatesAreNonNegativeAndUsable() {
        val rates = WorkRates()
        assertEquals(900.0, rates.floorInstallationPerM2, 0.001)
        assertEquals(1500.0, rates.electricalPoint, 0.001)
    }

    @Test fun furnitureHasMillimetreDimensionsAndQuarterTurnRotation() {
        val item = FurnitureItem("bed-1", FurnitureKind.BED, 100, 200, rotationDegrees = 90)
        assertEquals(1600, item.widthMm)
        assertEquals(2000, item.depthMm)
        assertTrue(item.rotationDegrees % 90 == 0)
    }
}
