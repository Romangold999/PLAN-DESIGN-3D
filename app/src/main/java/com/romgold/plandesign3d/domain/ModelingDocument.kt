package com.romgold.plandesign3d.domain

/** Core editable 3D data for the Blender-style scene workspace. */
data class Vector3(val x: Double, val y: Double, val z: Double) {
    init { require(x.isFinite() && y.isFinite() && z.isFinite()) }
}

data class Transform3D(
    val location: Vector3 = Vector3(0.0, 0.0, 0.0),
    val rotationDegrees: Vector3 = Vector3(0.0, 0.0, 0.0),
    val scale: Vector3 = Vector3(1.0, 1.0, 1.0)
)

data class MeshData(
    val vertices: List<Vector3>,
    /** Each face stores three or more vertex indices in winding order. */
    val faces: List<List<Int>>
) {
    init {
        require(vertices.isNotEmpty()) { "A mesh must contain vertices" }
        require(faces.isNotEmpty()) { "A mesh must contain faces" }
        faces.forEach { face ->
            require(face.size >= 3) { "A face must contain at least three vertices" }
            require(face.all { it in vertices.indices }) { "Face references a missing vertex" }
            require(face.distinct().size >= 3) { "A face must use at least three distinct vertices" }
        }
    }

    companion object {
        fun cube(size: Double = 2.0): MeshData {
            require(size.isFinite() && size > 0.0)
            val h = size / 2.0
            return MeshData(
                vertices = listOf(
                    Vector3(-h, -h, -h), Vector3(h, -h, -h),
                    Vector3(h, h, -h), Vector3(-h, h, -h),
                    Vector3(-h, -h, h), Vector3(h, -h, h),
                    Vector3(h, h, h), Vector3(-h, h, h)
                ),
                faces = listOf(
                    listOf(0, 3, 2, 1), listOf(4, 5, 6, 7),
                    listOf(0, 1, 5, 4), listOf(1, 2, 6, 5),
                    listOf(2, 3, 7, 6), listOf(3, 0, 4, 7)
                )
            )
        }
    }
}

data class SceneObject(
    val id: String,
    val name: String,
    val mesh: MeshData,
    val transform: Transform3D = Transform3D()
) {
    init { require(id.isNotBlank() && name.isNotBlank()) }
}

data class BlenderSceneDocument(
    val id: String,
    val name: String,
    val objects: List<SceneObject> = emptyList()
) {
    init {
        require(id.isNotBlank() && name.isNotBlank())
        require(objects.map { it.id }.distinct().size == objects.size) { "Scene object IDs must be unique" }
    }
}

/** Geometry records for a FreeCAD-style sketch. Constraint solving is not implemented by this model. */
sealed interface SketchGeometry {
    data class Segment(val start: Vector3, val end: Vector3) : SketchGeometry {
        init { require(start != end) { "A segment must have non-zero length" } }
    }
    data class Circle(val center: Vector3, val radius: Double) : SketchGeometry {
        init { require(radius.isFinite() && radius > 0.0) }
    }
}

enum class SketchConstraintType {
    COINCIDENT, HORIZONTAL, VERTICAL, DISTANCE, RADIUS, ANGLE, FIXED
}

/** Constraint intent and references only; this does not claim to solve or apply constraints. */
data class SketchConstraint(
    val type: SketchConstraintType,
    val firstGeometry: Int,
    val secondGeometry: Int? = null,
    val value: Double? = null
) {
    init {
        require(firstGeometry >= 0)
        require(secondGeometry == null || secondGeometry >= 0)
        when (type) {
            SketchConstraintType.COINCIDENT -> require(secondGeometry != null)
            SketchConstraintType.DISTANCE, SketchConstraintType.RADIUS, SketchConstraintType.ANGLE ->
                require(value != null && value.isFinite() && value > 0.0)
            SketchConstraintType.HORIZONTAL, SketchConstraintType.VERTICAL, SketchConstraintType.FIXED -> Unit
        }
    }
}

data class FreeCADSketchDocument(
    val id: String,
    val name: String,
    val geometry: List<SketchGeometry> = emptyList(),
    val constraints: List<SketchConstraint> = emptyList()
) {
    init {
        require(id.isNotBlank() && name.isNotBlank())
        constraints.forEach { constraint ->
            require(constraint.firstGeometry in geometry.indices) { "Constraint references missing geometry" }
            constraint.secondGeometry?.let { require(it in geometry.indices) { "Constraint references missing geometry" } }
        }
        constraints.forEach { constraint ->
            if (constraint.type == SketchConstraintType.RADIUS) {
                require(geometry[constraint.firstGeometry] is SketchGeometry.Circle) { "Radius constraint requires a circle" }
            }
        }
    }
}