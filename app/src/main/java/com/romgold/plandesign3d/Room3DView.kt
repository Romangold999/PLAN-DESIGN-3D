package com.romgold.plandesign3d

import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.sceneview.SceneView
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberEngine
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Size as SceneSize
import io.github.sceneview.node.CubeNode
import com.romgold.plandesign3d.domain.ProjectDocument
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private data class WallPiece(
    val wallIndex: Int,
    val fromMm: Int,
    val toMm: Int,
    val bottomMm: Int,
    val topMm: Int
)

private enum class CameraPreset(val label: String) {
    ISOMETRIC("3D"),
    FRONT("Спереди"),
    LEFT("Слева"),
    TOP("Сверху")
}

private fun makeWallPieces(project: ProjectDocument): List<WallPiece> {
    val result = mutableListOf<WallPiece>()
    project.room.walls.forEachIndexed { index, wall ->
        val openings = project.room.openings.filter { it.wallIndex == index }.sortedBy { it.offsetMm }
        var cursor = 0
        openings.forEach { opening ->
            if (opening.offsetMm > cursor) {
                result += WallPiece(index, cursor, opening.offsetMm, 0, wall.heightMm)
            }
            if (opening.sillHeightMm > 0) {
                result += WallPiece(index, opening.offsetMm, opening.offsetMm + opening.widthMm, 0, opening.sillHeightMm)
            }
            val topStart = opening.sillHeightMm + opening.heightMm
            if (topStart < wall.heightMm) {
                result += WallPiece(index, opening.offsetMm, opening.offsetMm + opening.widthMm, topStart, wall.heightMm)
            }
            cursor = opening.offsetMm + opening.widthMm
        }
        if (cursor < wall.lengthMm.toInt()) {
            result += WallPiece(index, cursor, wall.lengthMm.toInt(), 0, wall.heightMm)
        }
    }
    return result
}

@Composable
fun Room3DView(project: ProjectDocument, modifier: Modifier = Modifier) {
    val engine = rememberEngine()
    val pieces = remember(project.room) { makeWallPieces(project) }
    val centerX = project.room.walls.minOf { minOf(it.start.xMm, it.end.xMm) } +
        project.room.boundsWidthMm / 2f
    val centerZ = project.room.walls.minOf { minOf(it.start.yMm, it.end.yMm) } +
        project.room.boundsDepthMm / 2f

    var cameraYaw by remember(project.room) { mutableFloatStateOf(45f) }
    var cameraPitch by remember(project.room) { mutableFloatStateOf(42f) }
    val roomSpanMetres = max(project.room.boundsWidthMm, project.room.boundsDepthMm) / 1000f
    val cameraDistance = (roomSpanMetres * 1.35f + 0.8f).coerceAtLeast(2.5f)
    val yawRadians = Math.toRadians(cameraYaw.toDouble())
    val pitchRadians = Math.toRadians(cameraPitch.toDouble())
    val horizontalDistance = cameraDistance * cos(pitchRadians).toFloat()
    val cameraEye = Position(
        horizontalDistance * sin(yawRadians).toFloat(),
        cameraDistance * sin(pitchRadians).toFloat(),
        horizontalDistance * cos(yawRadians).toFloat()
    )
    val cameraManipulator = key(
        cameraYaw.toInt(),
        cameraPitch.toInt(),
        project.room.boundsWidthMm,
        project.room.boundsDepthMm
    ) {
        rememberCameraManipulator(
            orbitHomePosition = cameraEye,
            targetPosition = Position(0f, 0f, 0f)
        )
    }

    Column(modifier) {
        Text("3D-модель помещения", modifier = Modifier.padding(bottom = 8.dp))
        Text(
            "Вращайте камеру жестом или ползунками на 360° по горизонтали и почти на полный оборот по вертикали. Двумя пальцами — перемещение и масштаб.",
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CameraPreset.values().forEach { preset ->
                OutlinedButton(
                    onClick = {
                        when (preset) {
                            CameraPreset.ISOMETRIC -> { cameraYaw = 45f; cameraPitch = 42f }
                            CameraPreset.FRONT -> { cameraYaw = 0f; cameraPitch = 25f }
                            CameraPreset.LEFT -> { cameraYaw = 90f; cameraPitch = 25f }
                            CameraPreset.TOP -> { cameraYaw = 0f; cameraPitch = 88f }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Text(preset.label, maxLines = 1)
                }
            }
        }
        Text("Поворот вокруг вертикали: ${cameraYaw.toInt()}°")
        Slider(
            value = cameraYaw,
            onValueChange = { cameraYaw = it },
            valueRange = 0f..360f,
            steps = 35
        )
        Text("Наклон камеры: ${cameraPitch.toInt()}°")
        Slider(
            value = cameraPitch,
            onValueChange = { cameraPitch = it },
            valueRange = 5f..175f,
            steps = 33
        )
        SceneView(
            modifier = Modifier.fillMaxWidth().height(420.dp),
            engine = engine,
            cameraManipulator = cameraManipulator
        ) {
            val floorMaterial = remember(materialLoader, project.floorFinish.colorArgb) {
                materialLoader.createColorInstance(Color(project.floorFinish.colorArgb), metallic = 0f, roughness = 0.85f)
            }
            val wallMaterial = remember(materialLoader, project.wallFinish.colorArgb) {
                materialLoader.createColorInstance(Color(project.wallFinish.colorArgb), metallic = 0f, roughness = 0.9f)
            }
            val furnitureMaterial = remember(materialLoader) {
                materialLoader.createColorInstance(Color(0xFFC18A64), metallic = 0f, roughness = 0.75f)
            }
            CubeNode(
                size = SceneSize(project.room.boundsWidthMm / 1000f, 0.025f, project.room.boundsDepthMm / 1000f),
                position = Position(0f, -0.0125f, 0f),
                materialInstance = floorMaterial
            )
            pieces.forEach { piece ->
                val wall = project.room.walls[piece.wallIndex]
                val dx = (wall.end.xMm - wall.start.xMm).toFloat()
                val dz = (wall.end.yMm - wall.start.yMm).toFloat()
                val length = wall.lengthMm.toFloat()
                val ux = dx / length
                val uz = dz / length
                val centerDistance = (piece.fromMm + piece.toMm) / 2f
                val worldX = (wall.start.xMm + ux * centerDistance - centerX) / 1000f
                val worldZ = (wall.start.yMm + uz * centerDistance - centerZ) / 1000f
                val height = (piece.topMm - piece.bottomMm) / 1000f
                if (height > 0f && piece.toMm > piece.fromMm) {
                    CubeNode(
                        size = SceneSize((piece.toMm - piece.fromMm) / 1000f, height, wall.thicknessMm / 1000f),
                        position = Position(worldX, (piece.bottomMm + piece.topMm) / 2000f, worldZ),
                        rotation = Rotation(y = -atan2(uz, ux)),
                        materialInstance = wallMaterial
                    )
                }
            }
            project.furniture.forEach { item ->
                val rotated = item.rotationDegrees % 180 != 0
                val effectiveWidth = if (rotated) item.depthMm else item.widthMm
                val effectiveDepth = if (rotated) item.widthMm else item.depthMm
                CubeNode(
                    size = SceneSize(item.widthMm / 1000f, 0.65f, item.depthMm / 1000f),
                    position = Position(
                        (item.xMm + effectiveWidth / 2f - centerX) / 1000f,
                        0.325f,
                        (item.yMm + effectiveDepth / 2f - centerZ) / 1000f
                    ),
                    rotation = Rotation(y = Math.toRadians(item.rotationDegrees.toDouble()).toFloat()),
                    materialInstance = furnitureMaterial
                )
            }
        }
    }
}
