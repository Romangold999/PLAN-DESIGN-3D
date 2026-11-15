package com.romgold.plandesign3d.data

import android.content.Context
import com.romgold.plandesign3d.domain.*
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/** Local binary persistence for the editable Blender scene and FreeCAD sketch documents. */
class ModelingDocumentStore(context: Context) {
    private val directory = File(context.filesDir, "modeling-documents").apply { mkdirs() }
    private val sceneFile = File(directory, "blender-scene.pd3d")
    private val sketchFile = File(directory, "freecad-sketch.pd3d")

    fun loadScene(): BlenderSceneDocument? = read(sceneFile) { input ->
        val id = input.readUTF()
        val name = input.readUTF()
        val count = input.readInt().checkedCount(5000)
        val objects = List(count) {
            val objectId = input.readUTF()
            val objectName = input.readUTF()
            val vertexCount = input.readInt().checkedCount(1_000_000)
            val vertices = List(vertexCount) { input.readVector() }
            val faceCount = input.readInt().checkedCount(1_000_000)
            val faces = List(faceCount) {
                val size = input.readInt().checkedCount(100_000)
                require(size >= 3)
                List(size) { input.readInt() }
            }
            val transform = Transform3D(input.readVector(), input.readVector(), input.readVector())
            SceneObject(objectId, objectName, MeshData(vertices, faces), transform)
        }
        BlenderSceneDocument(id, name, objects)
    }

    fun saveScene(document: BlenderSceneDocument) {
        write(sceneFile) { output ->
            output.writeUTF(document.id)
            output.writeUTF(document.name)
            output.writeInt(document.objects.size)
            document.objects.forEach { item ->
                output.writeUTF(item.id)
                output.writeUTF(item.name)
                output.writeInt(item.mesh.vertices.size)
                item.mesh.vertices.forEach(output::writeVector)
                output.writeInt(item.mesh.faces.size)
                item.mesh.faces.forEach { face ->
                    output.writeInt(face.size)
                    face.forEach(output::writeInt)
                }
                output.writeVector(item.transform.location)
                output.writeVector(item.transform.rotationDegrees)
                output.writeVector(item.transform.scale)
            }
        }
    }

    fun loadSketch(): FreeCADSketchDocument? = read(sketchFile) { input ->
        val id = input.readUTF()
        val name = input.readUTF()
        val geometryCount = input.readInt().checkedCount(1_000_000)
        val geometry = List(geometryCount) {
            when (input.readUnsignedByte()) {
                0 -> SketchGeometry.Segment(input.readVector(), input.readVector())
                1 -> SketchGeometry.Circle(input.readVector(), input.readDouble())
                else -> error("Unknown sketch geometry type")
            }
        }
        val constraintCount = input.readInt().checkedCount(1_000_000)
        val constraints = List(constraintCount) {
            val ordinal = input.readUnsignedByte()
            val type = SketchConstraintType.entries.getOrNull(ordinal) ?: error("Unknown constraint type")
            val first = input.readInt()
            val second = input.readInt().let { if (it < 0) null else it }
            val value = if (input.readBoolean()) input.readDouble() else null
            SketchConstraint(type, first, second, value)
        }
        FreeCADSketchDocument(id, name, geometry, constraints)
    }

    fun saveSketch(document: FreeCADSketchDocument) {
        write(sketchFile) { output ->
            output.writeUTF(document.id)
            output.writeUTF(document.name)
            output.writeInt(document.geometry.size)
            document.geometry.forEach { geometry ->
                when (geometry) {
                    is SketchGeometry.Segment -> { output.writeByte(0); output.writeVector(geometry.start); output.writeVector(geometry.end) }
                    is SketchGeometry.Circle -> { output.writeByte(1); output.writeVector(geometry.center); output.writeDouble(geometry.radius) }
                }
            }
            output.writeInt(document.constraints.size)
            document.constraints.forEach { constraint ->
                output.writeByte(constraint.type.ordinal)
                output.writeInt(constraint.firstGeometry)
                output.writeInt(constraint.secondGeometry ?: -1)
                output.writeBoolean(constraint.value != null)
                constraint.value?.let(output::writeDouble)
            }
        }
    }

    private fun <T> read(file: File, block: (DataInputStream) -> T): T? = runCatching {
        DataInputStream(BufferedInputStream(FileInputStream(file))).use(block)
    }.getOrNull()

    private fun write(file: File, block: (DataOutputStream) -> Unit) {
        runCatching {
            val temp = File(directory, file.name + ".tmp")
            DataOutputStream(BufferedOutputStream(FileOutputStream(temp))).use(block)
            if (!temp.renameTo(file)) {
                file.delete()
                check(temp.renameTo(file)) { "Unable to replace modeling document" }
            }
        }
    }

    private fun Int.checkedCount(max: Int): Int { require(this in 0..max); return this }
    private fun DataInputStream.readVector() = Vector3(readDouble(), readDouble(), readDouble())
    private fun DataOutputStream.writeVector(value: Vector3) { writeDouble(value.x); writeDouble(value.y); writeDouble(value.z) }
}