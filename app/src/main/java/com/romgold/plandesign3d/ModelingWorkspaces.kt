package com.romgold.plandesign3d

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalContext
import com.romgold.plandesign3d.data.ModelingDocumentStore
import com.romgold.plandesign3d.domain.*
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

private val BlenderBackground = Color(0xFF202124)
private val BlenderPanel = Color(0xFF292B2F)
private val BlenderAccent = Color(0xFFF07832)
private val CadBackground = Color(0xFF20262C)
private val CadGrid = Color(0xFF34414B)
private val CadAccent = Color(0xFF4EA3D8)

@Composable
fun BlenderWorkspace() {
    val context = LocalContext.current
    val store = remember(context) { ModelingDocumentStore(context) }
    val initialScene = remember(store) {
        store.loadScene() ?: BlenderSceneDocument(UUID.randomUUID().toString(), "Scene", listOf(SceneObject(UUID.randomUUID().toString(), "Cube", MeshData.cube())))
    }
    val objects = remember(initialScene) {
        mutableStateListOf<SceneObject>().apply { addAll(initialScene.objects) }
    }
    LaunchedEffect(objects.toList()) { store.saveScene(initialScene.copy(objects = objects.toList())) }
    var selectedId by remember { mutableStateOf(objects.firstOrNull()?.id.orEmpty()) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var pitch by remember { mutableFloatStateOf(25f) }
    var cameraZoom by remember { mutableFloatStateOf(1f) }
    val selected = objects.firstOrNull { it.id == selectedId }

    Column(Modifier.fillMaxSize().background(BlenderBackground).padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("BLENDER  •  SCENE", color = Color.White, fontSize = 14.sp)
            Text("Object Mode", color = Color(0xFFB8BBC0), fontSize = 12.sp)
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = {
                val name = "Cube.${(objects.size).toString().padStart(3, '0')}"
                val item = SceneObject(UUID.randomUUID().toString(), name, MeshData.cube())
                objects.add(item); selectedId = item.id
            }, colors = ButtonDefaults.buttonColors(containerColor = BlenderAccent)) { Text("＋ Cube") }
            OutlinedButton(enabled = selected != null, onClick = {
                selected?.let {
                    val copy = it.copy(id = UUID.randomUUID().toString(), name = it.name + ".copy")
                    objects.add(copy); selectedId = copy.id
                }
            }) { Text("Duplicate") }
            OutlinedButton(enabled = selected != null, onClick = {
                objects.removeAll { it.id == selectedId }; selectedId = objects.firstOrNull()?.id.orEmpty()
            }) { Text("Delete") }
        }
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("3D Viewport", color = Color(0xFFD9DDE1), fontSize = 12.sp)
                Canvas(Modifier.fillMaxWidth().weight(1f).background(Color(0xFF303238), RoundedCornerShape(6.dp)).pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoomChange, _ ->
                        rotation = (rotation + pan.x * 0.6f) % 360f
                        pitch = (pitch + pan.y * 0.6f).coerceIn(-89f, 89f)
                        cameraZoom = (cameraZoom * zoomChange).coerceIn(0.35f, 4f)
                    }
                }) {
                    val step = 28f
                    var gx = 0f
                    while (gx < size.width) { drawLine(Color(0xFF3B3E44), Offset(gx, 0f), Offset(gx, size.height), 1f); gx += step }
                    var gy = 0f
                    while (gy < size.height) { drawLine(Color(0xFF3B3E44), Offset(0f, gy), Offset(size.width, gy), 1f); gy += step }
                    objects.firstOrNull { it.id == selectedId }?.let { item ->
                        val mesh = item.mesh
                        val viewAngle = Math.toRadians(rotation.toDouble())
                        val objectRotation = item.transform.rotationDegrees
                        val rx = Math.toRadians(objectRotation.x)
                        val ry = Math.toRadians(objectRotation.y)
                        val rz = Math.toRadians(objectRotation.z)
                        val cx = cos(rx); val sx = sin(rx)
                        val cy = cos(ry); val sy = sin(ry)
                        val cz = cos(rz); val sz = sin(rz)
                        val points = mesh.vertices.map { vertex ->
                            val scaledX = vertex.x * item.transform.scale.x
                            val scaledY = vertex.y * item.transform.scale.y
                            val scaledZ = vertex.z * item.transform.scale.z
                            val rotatedY = scaledY * cx - scaledZ * sx
                            val rotatedZ = scaledY * sx + scaledZ * cx
                            val rotatedX = scaledX * cy + rotatedZ * sy
                            val depthZ = -scaledX * sy + rotatedZ * cy
                            val objectX = rotatedX * cz - rotatedY * sz + item.transform.location.x
                            val objectY = rotatedX * sz + rotatedY * cz + item.transform.location.y
                            val objectZ = depthZ + item.transform.location.z
                            val yawX = objectX * cos(viewAngle) - objectZ * sin(viewAngle)
                            val yawZ = objectX * sin(viewAngle) + objectZ * cos(viewAngle)
                            val pitchAngle = Math.toRadians(pitch.toDouble())
                            val viewY = objectY * cos(pitchAngle) - yawZ * sin(pitchAngle)
                            val viewZ = objectY * sin(pitchAngle) + yawZ * cos(pitchAngle)
                            val isoX = (yawX - viewZ) * 0.72
                            val isoY = (yawX + viewZ) * 0.36 - viewY * 0.82
                            val scale = size.minDimension * 0.23f * cameraZoom
                            Offset(size.width / 2f + isoX.toFloat() * scale, size.height / 2f - isoY.toFloat() * scale)
                        }
                        val faceColors = listOf(Color(0xFF59636F), Color(0xFF778492), Color(0xFF65717D), Color(0xFF8794A0), Color(0xFF4C5661), Color(0xFF6B7784))
                        mesh.faces.forEachIndexed { index, face ->
                            val path = Path().apply {
                                moveTo(points[face.first()].x, points[face.first()].y)
                                face.drop(1).forEach { lineTo(points[it].x, points[it].y) }
                                close()
                            }
                            drawPath(path, faceColors[index % faceColors.size])
                            drawPath(path, BlenderAccent, style = Stroke(width = 1.8f))
                        }
                    }
                }
                Text("Орбита: X ${rotation.toInt()}°  Y ${pitch.toInt()}°  Масштаб ${"%.1f".format(cameraZoom)}×  •  Перетаскивание: орбита, щипок: масштаб", color = Color(0xFFB8BBC0), fontSize = 10.sp)
                Slider(value = rotation, onValueChange = { rotation = it }, valueRange = 0f..360f)
            }
            Column(Modifier.width(112.dp).fillMaxHeight().background(BlenderPanel, RoundedCornerShape(6.dp)).padding(6.dp)) {
                Text("Outliner", color = Color.White, fontSize = 12.sp)
                objects.forEach { item ->
                    TextButton(onClick = { selectedId = item.id }, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)) {
                        Text(if (item.id == selectedId) "● ${item.name}" else "◇ ${item.name}", color = if (item.id == selectedId) BlenderAccent else Color(0xFFD0D3D7), fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }
        Text("Transform", color = Color.White, fontSize = 13.sp)
        if (selected != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("X", "Y", "Z").forEachIndexed { axis, label ->
                    OutlinedButton(onClick = {
                        val p = selected.transform.location
                        val next = when (axis) { 0 -> p.copy(x = p.x + 0.1); 1 -> p.copy(y = p.y + 0.1); else -> p.copy(z = p.z + 0.1) }
                        val index = objects.indexOfFirst { it.id == selectedId }; if (index >= 0) objects[index] = selected.copy(transform = selected.transform.copy(location = next))
                    }, modifier = Modifier.weight(1f)) { Text("Move +$label") }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf("X", "Y", "Z").forEachIndexed { axis, label ->
                    OutlinedButton(onClick = {
                        val current = selected.transform.rotationDegrees
                        val next = when (axis) {
                            0 -> current.copy(x = current.x + 15.0)
                            1 -> current.copy(y = current.y + 15.0)
                            else -> current.copy(z = current.z + 15.0)
                        }
                        val index = objects.indexOfFirst { it.id == selectedId }
                        if (index >= 0) objects[index] = selected.copy(transform = selected.transform.copy(rotationDegrees = next))
                    }, modifier = Modifier.weight(1f)) { Text("Rotate $label +15°") }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf("X", "Y", "Z").forEachIndexed { axis, label ->
                    OutlinedButton(onClick = {
                        val current = selected.transform.scale
                        val next = when (axis) {
                            0 -> current.copy(x = (current.x + 0.1).coerceAtMost(10.0))
                            1 -> current.copy(y = (current.y + 0.1).coerceAtMost(10.0))
                            else -> current.copy(z = (current.z + 0.1).coerceAtMost(10.0))
                        }
                        val index = objects.indexOfFirst { it.id == selectedId }
                        if (index >= 0) objects[index] = selected.copy(transform = selected.transform.copy(scale = next))
                    }, modifier = Modifier.weight(1f)) { Text("Scale $label +") }
                }
            }
            Text("Rotation  X ${selected.transform.rotationDegrees.x.toInt()}°   Y ${selected.transform.rotationDegrees.y.toInt()}°   Z ${selected.transform.rotationDegrees.z.toInt()}°", color = Color(0xFFB8BBC0), fontSize = 11.sp)
            Text("Scale  X ${"%.1f".format(selected.transform.scale.x)}   Y ${"%.1f".format(selected.transform.scale.y)}   Z ${"%.1f".format(selected.transform.scale.z)}", color = Color(0xFFB8BBC0), fontSize = 11.sp)
            Text("Location  X ${"%.1f".format(selected.transform.location.x)}   Y ${"%.1f".format(selected.transform.location.y)}   Z ${"%.1f".format(selected.transform.location.z)}", color = Color(0xFFB8BBC0), fontSize = 11.sp)
        } else Text("Scene is empty", color = Color(0xFFB8BBC0), fontSize = 12.sp)
    }
}

@Composable
fun FreeCADWorkspace() {
    val context = LocalContext.current
    val store = remember(context) { ModelingDocumentStore(context) }
    var sketch by remember(store) { mutableStateOf(store.loadSketch() ?: FreeCADSketchDocument(UUID.randomUUID().toString(), "Sketch")) }
    LaunchedEffect(sketch) { store.saveSketch(sketch) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var drawingMode by remember { mutableStateOf("Line") }
    Column(Modifier.fillMaxSize().background(CadBackground).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("FreeCAD  •  SKETCHER", color = Color.White, fontSize = 14.sp)
            Text("XY plane", color = Color(0xFFB8C3CC), fontSize = 12.sp)
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = { drawingMode = "Line" }, colors = ButtonDefaults.buttonColors(containerColor = if (drawingMode == "Line") CadAccent else Color(0xFF34414B))) { Text("Line") }
            OutlinedButton(onClick = { drawingMode = "Circle" }) { Text("Circle") }
            OutlinedButton(onClick = { drawingMode = "Select" }) { Text("Select") }
            OutlinedButton(enabled = selectedIndex in sketch.geometry.indices, onClick = {
                sketch = sketch.copy(geometry = sketch.geometry.filterIndexed { i, _ -> i != selectedIndex }, constraints = sketch.constraints.filter { it.firstGeometry != selectedIndex && it.secondGeometry != selectedIndex }.map { c -> c.copy(firstGeometry = if (c.firstGeometry > selectedIndex) c.firstGeometry - 1 else c.firstGeometry, secondGeometry = c.secondGeometry?.let { if (it > selectedIndex) it - 1 else it }) })
                selectedIndex = -1
            }) { Text("Delete") }
        }
        Text("Sketch geometry", color = Color(0xFFCBD4DC), fontSize = 12.sp)
        Canvas(Modifier.fillMaxWidth().weight(1f).background(Color(0xFF171C21), RoundedCornerShape(6.dp))
            .pointerInput(drawingMode, sketch.geometry, selectedIndex) {
                var dragStart: Offset? = null
                var dragEnd: Offset? = null
                detectDragGestures(
                    onDragStart = { dragStart = it; dragEnd = it },
                    onDrag = { change, _ -> dragEnd = change.position },
                    onDragEnd = {
                        val start = dragStart
                        val end = dragEnd
                        if (start != null && end != null) {
                            val unit = minOf(size.width, size.height) / 240f
                            val origin = Offset(size.width / 2f, size.height / 2f)
                            if (drawingMode == "Select" && selectedIndex in sketch.geometry.indices) {
                                val dx = ((end.x - start.x) / unit).toDouble()
                                val dy = ((start.y - end.y) / unit).toDouble()
                                val moved = when (val item = sketch.geometry[selectedIndex]) {
                                    is SketchGeometry.Segment -> item.copy(
                                        start = item.start.copy(x = item.start.x + dx, y = item.start.y + dy),
                                        end = item.end.copy(x = item.end.x + dx, y = item.end.y + dy)
                                    )
                                    is SketchGeometry.Circle -> item.copy(center = item.center.copy(x = item.center.x + dx, y = item.center.y + dy))
                                }
                                sketch = sketch.copy(geometry = sketch.geometry.toMutableList().also { it[selectedIndex] = moved })
                            } else if (drawingMode != "Select") {
                                val worldStart = Vector3(((start.x - origin.x) / unit).toDouble(), ((origin.y - start.y) / unit).toDouble(), 0.0)
                                val worldEnd = Vector3(((end.x - origin.x) / unit).toDouble(), ((origin.y - end.y) / unit).toDouble(), 0.0)
                                val created: SketchGeometry? = when (drawingMode) {
                                    "Line" -> if (worldStart != worldEnd) SketchGeometry.Segment(worldStart, worldEnd) else null
                                    "Circle" -> {
                                        val radius = kotlin.math.hypot((end.x - start.x).toDouble(), (end.y - start.y).toDouble()) / unit.toDouble()
                                        if (radius > 0.5) SketchGeometry.Circle(worldStart, radius) else null
                                    }
                                    else -> null
                                }
                                if (created != null) {
                                    sketch = sketch.copy(geometry = sketch.geometry + created)
                                    selectedIndex = sketch.geometry.lastIndex
                                }
                            }
                        }
                        dragStart = null
                        dragEnd = null
                    }
                )
            }
            .pointerInput(drawingMode, sketch.geometry) {
                detectTapGestures { tap ->
                    if (drawingMode == "Select") {
                        val unit = minOf(size.width, size.height) / 240f
                        val origin = Offset(size.width / 2f, size.height / 2f)
                        fun screen(point: Vector3) = Offset(origin.x + point.x.toFloat() * unit, origin.y - point.y.toFloat() * unit)
                        fun segmentDistance(point: Offset, a: Offset, b: Offset): Float {
                            val dx = b.x - a.x
                            val dy = b.y - a.y
                            val length2 = dx * dx + dy * dy
                            val t = if (length2 == 0f) 0f else (((point.x - a.x) * dx + (point.y - a.y) * dy) / length2).coerceIn(0f, 1f)
                            return kotlin.math.hypot(point.x - (a.x + t * dx), point.y - (a.y + t * dy))
                        }
                        val nearest = sketch.geometry.mapIndexed { index, item ->
                            val distance = when (item) {
                                is SketchGeometry.Segment -> segmentDistance(tap, screen(item.start), screen(item.end))
                                is SketchGeometry.Circle -> kotlin.math.abs(kotlin.math.hypot(tap.x - screen(item.center).x, tap.y - screen(item.center).y) - item.radius.toFloat() * unit)
                            }
                            index to distance
                        }.minByOrNull { it.second }
                        selectedIndex = nearest?.takeIf { it.second <= 24f }?.first ?: -1
                    }
                }
            }) {
            val unit = size.minDimension / 240f
            val origin = Offset(size.width / 2f, size.height / 2f)
            var x = origin.x % (unit * 10f)
            while (x < size.width) { drawLine(CadGrid, Offset(x, 0f), Offset(x, size.height), 1f); x += unit * 10f }
            var y = origin.y % (unit * 10f)
            while (y < size.height) { drawLine(CadGrid, Offset(0f, y), Offset(size.width, y), 1f); y += unit * 10f }
            drawLine(Color(0xFF9A5656), Offset(0f, origin.y), Offset(size.width, origin.y), 1.5f)
            drawLine(Color(0xFF5C9A6C), Offset(origin.x, 0f), Offset(origin.x, size.height), 1.5f)
            sketch.geometry.forEachIndexed { index, geometry ->
                val color = if (index == selectedIndex) Color(0xFFFFB347) else Color(0xFFE2E8ED)
                when (geometry) {
                    is SketchGeometry.Segment -> drawLine(color, Offset(origin.x + geometry.start.x.toFloat() * unit, origin.y - geometry.start.y.toFloat() * unit), Offset(origin.x + geometry.end.x.toFloat() * unit, origin.y - geometry.end.y.toFloat() * unit), strokeWidth = if (index == selectedIndex) 4f else 2.5f)
                    is SketchGeometry.Circle -> drawCircle(color, geometry.radius.toFloat() * unit, Offset(origin.x + geometry.center.x.toFloat() * unit, origin.y - geometry.center.y.toFloat() * unit), style = Stroke(width = if (index == selectedIndex) 4f else 2.5f))
                }
            }
        }
        Text("Constraints", color = Color(0xFFCBD4DC), fontSize = 12.sp)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(enabled = selectedIndex in sketch.geometry.indices && sketch.geometry[selectedIndex] is SketchGeometry.Segment, onClick = { sketch = sketch.copy(constraints = sketch.constraints + SketchConstraint(SketchConstraintType.HORIZONTAL, selectedIndex)) }) { Text("Horizontal") }
            OutlinedButton(enabled = selectedIndex in sketch.geometry.indices && sketch.geometry[selectedIndex] is SketchGeometry.Segment, onClick = { sketch = sketch.copy(constraints = sketch.constraints + SketchConstraint(SketchConstraintType.VERTICAL, selectedIndex)) }) { Text("Vertical") }
            OutlinedButton(enabled = selectedIndex in sketch.geometry.indices, onClick = { sketch = sketch.copy(constraints = sketch.constraints + SketchConstraint(SketchConstraintType.FIXED, selectedIndex)) }) { Text("Fix") }
            OutlinedButton(enabled = selectedIndex in sketch.geometry.indices && sketch.geometry[selectedIndex] is SketchGeometry.Circle, onClick = { sketch = sketch.copy(constraints = sketch.constraints + SketchConstraint(SketchConstraintType.RADIUS, selectedIndex, value = (sketch.geometry[selectedIndex] as SketchGeometry.Circle).radius)) }) { Text("Radius") }
        }
        Text("${sketch.geometry.size} геометрических элементов  •  ${sketch.constraints.size} записей ограничений", color = Color(0xFFB8C3CC), fontSize = 11.sp)
        sketch.geometry.forEachIndexed { index, geometry ->
            TextButton(onClick = { selectedIndex = index }, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                Text((if (index == selectedIndex) "● " else "○ ") + when (geometry) { is SketchGeometry.Segment -> "Line $index"; is SketchGeometry.Circle -> "Circle $index" }, color = if (index == selectedIndex) Color(0xFFFFB347) else Color(0xFFD0D8DE), fontSize = 11.sp)
            }
        }
        Text("Ограничения пока сохраняются как параметры эскиза; геометрический решатель ещё не подключён.", color = Color(0xFF9DAAB4), fontSize = 10.sp)
    }
}