package com.romgold.plandesign3d.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ModelingDocumentTest {
    @Test fun cubePrimitiveHasEightVerticesAndSixFaces() {
        val cube = MeshData.cube(4.0)
        assertEquals(8, cube.vertices.size)
        assertEquals(6, cube.faces.size)
        assertEquals(4.0, cube.vertices.maxOf { it.x } - cube.vertices.minOf { it.x }, 0.000001)
    }

    @Test fun meshRejectsOutOfRangeFaceIndices() {
        assertThrows(IllegalArgumentException::class.java) {
            MeshData(listOf(Vector3(0.0, 0.0, 0.0)), listOf(listOf(0, 1, 2)))
        }
    }

    @Test fun sceneRejectsDuplicateObjectIds() {
        val mesh = MeshData.cube()
        val first = SceneObject("same", "Cube", mesh)
        val second = SceneObject("same", "Cube.001", mesh)
        assertThrows(IllegalArgumentException::class.java) {
            BlenderSceneDocument("scene", "Scene", listOf(first, second))
        }
    }

    @Test fun sketchAcceptsCoincidentConstraintWithTwoGeometryReferences() {
        val sketch = FreeCADSketchDocument(
            id = "sketch",
            name = "Sketch",
            geometry = listOf(
                SketchGeometry.Segment(Vector3(0.0, 0.0, 0.0), Vector3(10.0, 0.0, 0.0)),
                SketchGeometry.Segment(Vector3(10.0, 0.0, 0.0), Vector3(10.0, 10.0, 0.0))
            ),
            constraints = listOf(SketchConstraint(SketchConstraintType.COINCIDENT, 0, 1))
        )
        assertEquals(1, sketch.constraints.size)
    }

    @Test fun sketchRejectsConstraintReferencesOutsideGeometryList() {
        assertThrows(IllegalArgumentException::class.java) {
            FreeCADSketchDocument(
                id = "sketch", name = "Sketch",
                geometry = listOf(SketchGeometry.Circle(Vector3(0.0, 0.0, 0.0), 5.0)),
                constraints = listOf(SketchConstraint(SketchConstraintType.FIXED, 1))
            )
        }
    }

    @Test fun radiusConstraintCanOnlyTargetCircle() {
        assertThrows(IllegalArgumentException::class.java) {
            FreeCADSketchDocument(
                id = "sketch", name = "Sketch",
                geometry = listOf(SketchGeometry.Segment(Vector3(0.0, 0.0, 0.0), Vector3(1.0, 0.0, 0.0))),
                constraints = listOf(SketchConstraint(SketchConstraintType.RADIUS, 0, value = 2.0))
            )
        }
    }
}