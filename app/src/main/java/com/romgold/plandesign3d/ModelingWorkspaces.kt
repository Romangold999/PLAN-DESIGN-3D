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
    val objects = remember {
        mutableStateListOf(SceneObject(UUID.randomUUID().toString(), "Cube", MeshData.cube()))
    }
    var selectedId by remember { mutableStateOf(objects.first().id) }
    var rotation by remember { mutableFloatStateOf(0f) }
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
                Canvas(Modifier.fillMaxWidth().weight(1f).background(Color(0xFF303238), RoundedCornerShape(6.dp))) {
                    val step = 28f
                    var gx = 0f
                    while (gx < size.width) { drawLine(Color(0xFF3B3E44), Offset(gx, 0f), Offset(gx, size.height), 1f); gx += step }
                    var gy = 0f
                    while (gy < size.height) { drawLine(Color(0xFF3B3E44), Offset(0f, gy), Offset(size.width, gy), 1f); gy += step }
                    objects.firstOrNull { it.id == selectedId }?.let { item ->
                        val mesh = item.mesh
                        val angle = Math.toRadians(rotation.toDouble())
                        val points = mesh.vertices.map { vertex ->
                            val localX = vertex.x + item.transform.location.x
                            val localY = vertex.y + item.transform.location.y
                            val localZ = vertex.z + item.transform.location.z
                            val x = localX * cos(angle) - localZ * sin(angle)
                            val z = localX * sin(angle) + localZ * cos(angle)
                            val isoX = (x - z) * 0.72
                            val isoY = (x + z) * 0.36 - localY * 0.82
                            Offset(size.width / 2f + isoX.toFloat() * size.minDimension * 0.23f, size.height / 2f - isoY.toFloat() * size.minDimension * 0.23f)
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
                Text("Орбита: ${rotation.toInt()}°", color = Color(0xFFB8BBC0), fontSize = 11.sp)
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
            Text("Location  X ${"%.1f".format(selected.transform.location.x)}   Y ${"%.1f".format(selected.transform.location.y)}   Z ${"%.1f".format(selected.transform.location.z)}", color = Color(0xFFB8BBC0), fontSize = 11.sp)
        } else Text("Scene is empty", color = Color(0xFFB8BBC0), fontSize = 12.sp)
    }
}

@Composable
fun FreeCADWorkspace() {
    var sketch by remember { mutableStateOf(FreeCADSketchDocument(UUID.randomUUID().toString(), "Sketch")) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    Column(Modifier.fillMaxSize().background(CadBackground).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("FreeCAD  •  SKETCHER", color = Color.White, fontSize = 14.sp)
            Text("XY plane", color = Color(0xFFB8C3CC), fontSize = 12.sp)
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = {
                sketch = sketch.copy(geometry = sketch.geometry + SketchGeometry.Segment(Vector3(-45.0, -25.0, 0.0), Vector3(45.0, 25.0, 0.0)))
                selectedIndex = sketch.geometry.lastIndex
            }, colors = ButtonDefaults.buttonColors(containerColor = CadAccent)) { Text("Line") }
            OutlinedButton(onClick = {
                sketch = sketch.copy(geometry = sketch.geometry + SketchGeometry.Circle(Vector3(0.0, 0.0, 0.0), 35.0))
                selectedIndex = sketch.geometry.lastIndex
            }) { Text("Circle") }
            OutlinedButton(enabled = selectedIndex in sketch.geometry.indices, onClick = {
                sketch = sketch.copy(geometry = sketch.geometry.filterIndexed { i, _ -> i != selectedIndex }, constraints = sketch.constraints.filter { it.firstGeometry != selectedIndex && it.secondGeometry != selectedIndex }.map { c -> c.copy(firstGeometry = if (c.firstGeometry > selectedIndex) c.firstGeometry - 1 else c.firstGeometry, secondGeometry = c.secondGeometry?.let { if (it > selectedIndex) it - 1 else it }) })
                selectedIndex = -1
            }) { Text("Delete") }
        }
        Text("Sketch geometry", color = Color(0xFFCBD4DC), fontSize = 12.sp)
        Canvas(Modifier.fillMaxWidth().weight(1f).background(Color(0xFF171C21), RoundedCornerShape(6.dp))) {
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