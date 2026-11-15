package com.romgold.plandesign3d

import com.romgold.plandesign3d.domain.*
import com.romgold.plandesign3d.data.EstimateCsvExporter
import com.romgold.plandesign3d.data.ProjectSvgExporter
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
    @Test fun narrowFurniturePassagesReportClearanceAndIgnoreOverlaps() {
        val sofa = FurnitureItem("sofa", FurnitureKind.SOFA, 0, 0, widthMm = 2000, depthMm = 900)
        val table = FurnitureItem("table", FurnitureKind.TABLE, 2500, 0, widthMm = 1000, depthMm = 700)
        val bed = FurnitureItem("bed", FurnitureKind.BED, 1000, 200, widthMm = 1600, depthMm = 2000)
        val results = findNarrowFurniturePassages(listOf(sofa, table, bed), 600)
        assertEquals(1, results.size)
        assertEquals(500, results.first().distanceMm)
        assertEquals("sofa", results.first().first.id)
        assertEquals("table", results.first().second.id)
    }

    @Test fun rotatedFurnitureUsesSwappedDimensionsForClearance() {
        val first = FurnitureItem("first", FurnitureKind.TABLE, 0, 0, widthMm = 1200, depthMm = 600)
        val rotated = FurnitureItem("rotated", FurnitureKind.TABLE, 0, 600,
            widthMm = 1200, depthMm = 600, rotationDegrees = 90)
        val results = findNarrowFurniturePassages(listOf(first, rotated), 700)
        assertEquals(1, results.size)
        assertEquals(0, results.first().distanceMm)
    }

    @Test fun furnitureNearWallIsReportedButDoorOpeningIsNotTreatedAsSolidWall() {
        val room = RoomPlan.rectangle(5000, 4000)
        val nearWall = FurnitureItem("near", FurnitureKind.TABLE, 100, 1000, 1000, 600)
        val result = findFurnitureWallClearances(room, listOf(nearWall), 600)
        assertEquals(1, result.size)
        assertEquals(40, result.first().distanceMm)

        val roomWithDoor = RoomPlan(
            room.walls,
            listOf(Opening(wallIndex = 0, offsetMm = 1000, widthMm = 900, heightMm = 2100, type = OpeningType.DOOR))
        )
        val inDoorway = FurnitureItem("doorway", FurnitureKind.TABLE, 1100, 0, 700, 600)
        assertTrue(findFurnitureWallClearances(roomWithDoor, listOf(inDoorway), 600).isEmpty())
    }

    @Test fun rotatedFurnitureFootprintIsUsedForWallClearance() {
        val room = RoomPlan.rectangle(5000, 4000)
        val item = FurnitureItem("rotated", FurnitureKind.TABLE, 100, 1000,
            widthMm = 1200, depthMm = 600, rotationDegrees = 90)
        val result = findFurnitureWallClearances(room, listOf(item), 600)
        assertEquals(1, result.size)
        assertEquals(40, result.first().distanceMm)
    }


    @Test fun svgExportUsesRotatedFurnitureFootprintAndEscapesLabels() {
        val room = RoomPlan.rectangle(5000, 4000)
        val furniture = FurnitureItem("rotated", FurnitureKind.TABLE, 100, 200,
            widthMm = 1200, depthMm = 600, rotationDegrees = 90)
        val annotation = PlanAnnotation("note", AnnotationKind.SOCKET, 300, 400, "A&B <test>")
        val project = ProjectDocument("svg-test", "SVG test", room,
            furniture = listOf(furniture), annotations = listOf(annotation))

        val svg = ProjectSvgExporter.encode(project)

        assertTrue(svg.contains("""transform="translate(400.0,800.0) rotate(90)""""))
        assertTrue(svg.contains("""x="-600.0" y="-300.0" width="1200" height="600""""))
        assertTrue(svg.contains("A&amp;B &lt;test&gt;"))
        assertTrue(!svg.contains("A&B <test>"))
    }

}
