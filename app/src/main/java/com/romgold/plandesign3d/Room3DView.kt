package com.romgold.plandesign3d

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.romgold.plandesign3d.domain.OpeningType
import kotlin.math.atan2

private data class WallPiece(
    val wallIndex: Int,
    val fromMm: Int,
    val toMm: Int,
    val bottomMm: Int,
    val topMm: Int
)

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

    Column(modifier) {
        Text("3D-модель помещения", modifier = Modifier.padding(bottom = 8.dp))
        Text("Проведите пальцем для вращения, двумя пальцами — для масштаба и перемещения.",
            modifier = Modifier.padding(bottom = 8.dp))
        SceneView(
            modifier = Modifier.fillMaxWidth().height(420.dp),
            engine = engine,
            cameraManipulator = rememberCameraManipulator()
        ) {
            CubeNode(
                size = SceneSize(project.room.boundsWidthMm / 1000f, 0.025f, project.room.boundsDepthMm / 1000f),
                position = Position(0f, -0.0125f, 0f)
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
                        rotation = Rotation(y = -atan2(uz, ux))
                    )
                }
            }
            project.furniture.forEach { item ->
                CubeNode(
                    size = SceneSize(item.widthMm / 1000f, 0.65f, item.depthMm / 1000f),
                    position = Position(
                        (item.xMm + item.widthMm / 2f - centerX) / 1000f,
                        0.325f,
                        (item.yMm + item.depthMm / 2f - centerZ) / 1000f
                    )
                )
            }
        }
    }
}
