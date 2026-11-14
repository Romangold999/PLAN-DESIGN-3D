@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.romgold.plandesign3d

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.runtime.key
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romgold.plandesign3d.data.ProjectStore
import com.romgold.plandesign3d.data.ProjectCodec
import com.romgold.plandesign3d.data.ProjectSvgExporter
import com.romgold.plandesign3d.data.EstimateCsvExporter
import com.romgold.plandesign3d.domain.*
import java.util.UUID
import kotlin.math.min

private enum class EditorTab(val title: String) {
    PLAN("План"), WALLS("Стены"), OPENINGS("Проёмы"), FURNITURE("Мебель"), FINISHES("Материалы"), ESTIMATE("Расчёт"), THREE_D("3D"), SURFACE("Раскладка"), ELEVATIONS("Развёртки"), ENGINEERING("Инженерия"), EXPORT("Экспорт")
}

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = ProjectStore(this)
        setContent {
            val context = LocalContext.current
            var pendingExport by remember { mutableStateOf("") }
            val svgLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/svg+xml")) { uri ->
                if (uri != null) context.contentResolver.openOutputStream(uri)?.use { it.write(pendingExport.toByteArray(Charsets.UTF_8)) }
            }
            val jsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
                if (uri != null) context.contentResolver.openOutputStream(uri)?.use { it.write(pendingExport.toByteArray(Charsets.UTF_8)) }
            }
            val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
                if (uri != null) context.contentResolver.openOutputStream(uri)?.use { it.write(pendingExport.toByteArray(Charsets.UTF_8)) }
            }
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xFF83B8D8),
                onPrimary = Color(0xFF10212C),
                secondary = Color(0xFF9BC7B1),
                background = Color(0xFF11161B),
                surface = Color(0xFF1B2229),
                surfaceVariant = Color(0xFF29343D),
                onSurface = Color(0xFFE5EBEF),
                onSurfaceVariant = Color(0xFFB4C0C8),
                outline = Color(0xFF53616B)
            )) {
                var project by remember {
                    mutableStateOf(store.loadLast() ?: ProjectDocument(UUID.randomUUID().toString(), "Мой проект", RoomPlan.rectangle(5000, 4000)))
                }
                var tab by remember { mutableStateOf(EditorTab.PLAN) }
                val undo = remember { mutableStateListOf<ProjectDocument>() }
                val redo = remember { mutableStateListOf<ProjectDocument>() }
                val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    if (uri != null) {
                        val imported = runCatching {
                            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { ProjectCodec.decode(it.readText()) }
                        }.getOrNull()
                        if (imported != null) {
                            project = imported
                            undo.clear()
                            redo.clear()
                            tab = EditorTab.PLAN
                        }
                    }
                }
                var saved by remember { mutableStateOf(true) }
                var projectMenu by remember { mutableStateOf(false) }

                val change: (ProjectDocument) -> Unit = { next ->
                    if (next != project) {
                        undo.add(project)
                        if (undo.size > 40) undo.removeAt(0)
                        redo.clear()
                        project = next
                        saved = false
                        store.save(next)
                    }
                }
                LaunchedEffect(project) { store.save(project); saved = true }

                Scaffold(
                    containerColor = Color(0xFF11161B),
                    topBar = {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color(0xFF1B2229),
                                titleContentColor = Color(0xFFE5EBEF),
                                actionIconContentColor = Color(0xFFB8C8D3)
                            ),
                            title = {
                                Column {
                                    Text("PLAN DESIGN 3D", fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 1.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(Modifier.size(6.dp).background(if (saved) Color(0xFF69C18B) else Color(0xFFE3AE58), RoundedCornerShape(50)))
                                        Text(project.name, fontSize = 11.sp, color = Color(0xFFB4C0C8), maxLines = 1)
                                    }
                                }
                            },
                            actions = {
                                Box {
                                    TextButton(onClick = { projectMenu = true }) { Text("ФАЙЛ") }
                                    DropdownMenu(expanded = projectMenu, onDismissRequest = { projectMenu = false }) {
                                        DropdownMenuItem(text = { Text("＋ Новый проект") }, onClick = {
                                            project = ProjectDocument(UUID.randomUUID().toString(), "Новый проект", RoomPlan.rectangle(5000, 4000))
                                            undo.clear(); redo.clear(); tab = EditorTab.PLAN; projectMenu = false
                                        })
                                        HorizontalDivider()
                                        store.list().forEach { savedProject ->
                                            DropdownMenuItem(text = { Text(savedProject.name) }, onClick = {
                                                project = savedProject; undo.clear(); redo.clear(); tab = EditorTab.PLAN; projectMenu = false
                                            })
                                        }
                                    }
                                }
                                TextButton(enabled = undo.isNotEmpty(), onClick = {
                                    if (undo.isNotEmpty()) { redo.add(project); project = undo.removeAt(undo.lastIndex); saved = false }
                                }) { Text("↶") }
                                TextButton(enabled = redo.isNotEmpty(), onClick = {
                                    if (redo.isNotEmpty()) { undo.add(project); project = redo.removeAt(redo.lastIndex); saved = false }
                                }) { Text("↷") }
                                TextButton(onClick = { store.save(project); saved = true }) { Text("СОХР.") }
                            }
                        )
                    }
                ) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding).background(Color(0xFF11161B))) {
                        Row(
                            Modifier.fillMaxWidth().background(Color(0xFF202830)).horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            EditorTab.values().forEach { item ->
                                FilterChip(
                                    selected = tab == item,
                                    onClick = { tab = item },
                                    label = { Text(item.title, fontSize = 12.sp) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF35566B),
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFF29343D),
                                        labelColor = Color(0xFFCFD8DE)
                                    )
                                )
                            }
                        }
                        Column(
                            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            when (tab) {
                                EditorTab.PLAN -> PlanEditor(project, change)
                                EditorTab.WALLS -> WallEditor(project, change)
                                EditorTab.OPENINGS -> OpeningEditor(project, change)
                                EditorTab.FURNITURE -> FurnitureEditor(project, change)
                                EditorTab.FINISHES -> FinishEditor(project, change)
                                EditorTab.ESTIMATE -> EstimateEditor(project, change)
                                EditorTab.THREE_D -> Room3DView(project, Modifier.fillMaxWidth())
                                EditorTab.SURFACE -> SurfaceLayoutEditor(project, change)
                                EditorTab.ELEVATIONS -> WallElevationEditor(project)
                                EditorTab.ENGINEERING -> EngineeringEditor(project, change)
                                EditorTab.EXPORT -> ExportEditor(
                                    project,
                                    onSvg = { pendingExport = ProjectSvgExporter.encode(project); svgLauncher.launch(project.name + ".svg") },
                                    onJson = { pendingExport = ProjectCodec.encode(project); jsonLauncher.launch(project.name + ".json") },
                                    onCsv = { pendingExport = EstimateCsvExporter.encode(project); csvLauncher.launch(project.name + "-smeta.csv") },
                                    onImport = { importLauncher.launch(arrayOf("application/json", "text/plain")) }
                                )
                            }
                            Text(if (saved) "Автосохранение включено • офлайн" else "Есть несохранённые изменения",
                                color = if (saved) Color(0xFF28734A) else MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanEditor(project: ProjectDocument, change: (ProjectDocument) -> Unit) {
    var widthText by remember(project.room.boundsWidthMm) { mutableStateOf(project.room.boundsWidthMm.toString()) }
    var depthText by remember(project.room.boundsDepthMm) { mutableStateOf(project.room.boundsDepthMm.toString()) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var projectName by remember(project.id, project.name) { mutableStateOf(project.name) }
    Text("План помещения", fontSize = 21.sp, fontWeight = FontWeight.Bold)
    OutlinedTextField(value = projectName, onValueChange = { projectName = it }, label = { Text("Название проекта") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedButton(onClick = { if (projectName.isNotBlank()) change(project.copy(name = projectName.trim())) }, modifier = Modifier.fillMaxWidth()) { Text("Переименовать проект") }
    Text("Все размеры хранятся в миллиметрах. Масштабирование не меняет исходную геометрию.")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberInput("Ширина, мм", widthText, { widthText = it }, Modifier.weight(1f))
        NumberInput("Глубина, мм", depthText, { depthText = it }, Modifier.weight(1f))
    }
    Button(onClick = {
        val w = widthText.toIntOrNull()?.coerceIn(1000, 30000) ?: return@Button
        val d = depthText.toIntOrNull()?.coerceIn(1000, 30000) ?: return@Button
        change(project.copy(room = RoomPlan.rectangle(w, d), furniture = emptyList()))
        zoom = 1f
        pan = Offset.Zero
    }, modifier = Modifier.fillMaxWidth()) { Text("Применить размеры") }

    var pointX by remember { mutableStateOf("0") }
    var pointY by remember { mutableStateOf("0") }
    var vertices by remember(project.room) { mutableStateOf(project.room.walls.map { it.start }) }
    Text("Нестандартная геометрия", fontWeight = FontWeight.Bold)
    Text("Задайте углы помещения координатами X/Y в миллиметрах. Точки соединяются по порядку и автоматически замыкаются.")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberInput("X, мм", pointX, { pointX = it }, Modifier.weight(1f))
        NumberInput("Y, мм", pointY, { pointY = it }, Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = {
            val x = pointX.toIntOrNull() ?: return@OutlinedButton
            val y = pointY.toIntOrNull() ?: return@OutlinedButton
            val point = Point2(x, y)
            if (point !in vertices) vertices = vertices + point
        }, modifier = Modifier.weight(1f)) { Text("Добавить угол") }
        OutlinedButton(onClick = { if (vertices.isNotEmpty()) vertices = vertices.dropLast(1) },
            enabled = vertices.isNotEmpty(), modifier = Modifier.weight(1f)) { Text("Убрать последний") }
    }
    Text("Вершины: " + vertices.joinToString("  →  ") { "(" + it.xMm + "; " + it.yMm + ")" }, fontSize = 12.sp)
    Button(onClick = {
        val shape = runCatching { RoomPlan.fromPoints(vertices, project.room.walls.first().thicknessMm, project.room.walls.first().heightMm) }.getOrNull()
            ?: return@Button
        change(project.copy(room = shape, furniture = emptyList()))
    }, enabled = vertices.size >= 3, modifier = Modifier.fillMaxWidth()) { Text("Построить помещение по контуру") }

    Box(Modifier.fillMaxWidth().height(340.dp).background(Color(0xFF151D24), RoundedCornerShape(12.dp)).padding(8.dp)) {
        Canvas(Modifier.fillMaxSize().pointerInput(project.room) {
            detectTransformGestures { _, delta, zoomDelta, _ ->
                zoom = (zoom * zoomDelta).coerceIn(0.45f, 6f)
                pan += delta
            }
        }) {
            val bw = project.room.boundsWidthMm.coerceAtLeast(1)
            val bd = project.room.boundsDepthMm.coerceAtLeast(1)
            val fit = min((size.width - 56f) / bw, (size.height - 56f) / bd)
            val scale = fit * zoom
            val minX = project.room.walls.minOf { min(it.start.xMm, it.end.xMm) }
            val minY = project.room.walls.minOf { min(it.start.yMm, it.end.yMm) }
            val origin = Offset((size.width - bw * scale) / 2f, (size.height - bd * scale) / 2f) + pan
            fun point(x: Int, y: Int) = Offset(origin.x + (x - minX) * scale, origin.y + (y - minY) * scale)
            for (x in minX..(minX + bw) step 500) {
                val major = (x % 1000 == 0)
                drawLine(if (major) Color(0xFF344653) else Color(0xFF26343E), point(x, minY), point(x, minY + bd), strokeWidth = if (major) 1.2f else 0.6f)
            }
            for (y in minY..(minY + bd) step 500) {
                val major = (y % 1000 == 0)
                drawLine(if (major) Color(0xFF344653) else Color(0xFF26343E), point(minX, y), point(minX + bw, y), strokeWidth = if (major) 1.2f else 0.6f)
            }
            val floorPath = Path().apply {
                val first = point(project.room.walls.first().start.xMm, project.room.walls.first().start.yMm)
                moveTo(first.x, first.y)
                project.room.walls.forEach { wall ->
                    val end = point(wall.end.xMm, wall.end.yMm)
                    lineTo(end.x, end.y)
                }
                close()
            }
            drawPath(floorPath, Color(project.floorFinish.colorArgb).copy(alpha = 0.45f))
            clipPath(floorPath) {
                val tileWidth = project.surfaceLayout.tileWidthMm.coerceAtLeast(50)
                val tileHeight = project.surfaceLayout.tileHeightMm.coerceAtLeast(50)
                if (project.surfaceLayout.pattern != SurfacePattern.HERRINGBONE &&
                    project.surfaceLayout.pattern != SurfacePattern.FRENCH_HERRINGBONE) {
                    for (x in minX..(minX + bw) step tileWidth) {
                        drawLine(Color(0x55315C78), point(x, minY), point(x, minY + bd), strokeWidth = 1f)
                    }
                    for (y in minY..(minY + bd) step tileHeight) {
                        drawLine(Color(0x55315C78), point(minX, y), point(minX + bw, y), strokeWidth = 1f)
                    }
                }
            }
            project.room.walls.forEachIndexed { index, wall ->
                val a = point(wall.start.xMm, wall.start.yMm)
                val b = point(wall.end.xMm, wall.end.yMm)
                val stroke = (wall.thicknessMm * scale).coerceAtLeast(5f)
                drawLine(Color(0xFF8FB8D0), a, b, strokeWidth = stroke)
                project.room.openings.filter { it.wallIndex == index }.forEach { opening ->
                    val length = wall.lengthMm.toFloat()
                    val from = opening.offsetMm / length
                    val to = (opening.offsetMm + opening.widthMm) / length
                    val p1 = Offset(a.x + (b.x - a.x) * from, a.y + (b.y - a.y) * from)
                    val p2 = Offset(a.x + (b.x - a.x) * to, a.y + (b.y - a.y) * to)
                    drawLine(Color(0xFF151D24), p1, p2, strokeWidth = stroke + 3f)
                    if (opening.type == OpeningType.WINDOW) drawLine(Color(0xFF38A6C8), p1, p2, strokeWidth = 3f)
                }
            }
            project.furniture.forEach { item ->
                val rotated = item.rotationDegrees % 180 != 0
                val effectiveWidth = if (rotated) item.depthMm else item.widthMm
                val effectiveDepth = if (rotated) item.widthMm else item.depthMm
                val center = point(item.xMm + effectiveWidth / 2, item.yMm + effectiveDepth / 2)
                val s = Size(item.widthMm * scale, item.depthMm * scale)
                val p = Offset(center.x - s.width / 2f, center.y - s.height / 2f)
                rotate(item.rotationDegrees.toFloat(), center) {
                    drawRect(Color(0xFFB77B55), p, s)
                    drawRect(Color(0xFFE0B08D), p, s, style = Stroke(2f))
                }
            }
            project.annotations.forEach { annotation ->
                val center = point(annotation.xMm, annotation.yMm)
                val color = when (annotation.kind) {
                    AnnotationKind.SOCKET -> Color(0xFF2D8A57)
                    AnnotationKind.SWITCH -> Color(0xFFDA9A27)
                    AnnotationKind.LIGHT -> Color(0xFFE8C547)
                    AnnotationKind.WATER -> Color(0xFF2D9CCB)
                    AnnotationKind.DRAIN -> Color(0xFF536B9B)
                    AnnotationKind.VENTILATION -> Color(0xFF8B65A8)
                }
                drawCircle(color, radius = 9f, center = center)
                drawCircle(Color(0xFF263238), radius = 9f, center = center, style = Stroke(2f))
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { zoom = 1f; pan = Offset.Zero }) { Text("Сбросить вид") }
        Text(project.room.boundsWidthMm.toString() + " × " + project.room.boundsDepthMm + " мм", fontWeight = FontWeight.SemiBold)
    }
    val metrics = project.room.metrics()
    Text("Площадь пола: %.2f м² • Периметр: %.2f м".format(metrics.floorAreaM2, metrics.perimeterM))
}


@Composable
private fun WallEditor(project: ProjectDocument, change: (ProjectDocument) -> Unit) {
    Text("Конструкция стен", fontSize = 21.sp, fontWeight = FontWeight.Bold)
    Text("Толщина и высота задаются отдельно для каждой стены. Длина определяется геометрией контура.")
    project.room.walls.forEachIndexed { index, wall ->
        key(index) {
            var thickness by remember(index, wall.thicknessMm) { mutableStateOf(wall.thicknessMm.toString()) }
            var height by remember(index, wall.heightMm) { mutableStateOf(wall.heightMm.toString()) }
            Card {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Стена " + (index + 1) + " • длина " + wall.lengthMm.toInt() + " мм", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInput("Толщина, мм", thickness, { thickness = it }, Modifier.weight(1f))
                        NumberInput("Высота, мм", height, { height = it }, Modifier.weight(1f))
                    }
                    OutlinedButton(onClick = {
                        val t = thickness.toIntOrNull() ?: return@OutlinedButton
                        val h = height.toIntOrNull() ?: return@OutlinedButton
                        val walls = project.room.walls.toMutableList()
                        walls[index] = wall.copy(thicknessMm = t, heightMm = h)
                        val room = runCatching { RoomPlan(walls, project.room.openings) }.getOrNull() ?: return@OutlinedButton
                        change(project.copy(room = room))
                    }, modifier = Modifier.fillMaxWidth()) { Text("Применить к стене " + (index + 1)) }
                }
            }
        }
    }
}

@Composable
private fun OpeningEditor(project: ProjectDocument, change: (ProjectDocument) -> Unit) {
    var wall by remember { mutableIntStateOf(0) }
    var kind by remember { mutableStateOf(OpeningType.DOOR) }
    var offset by remember { mutableStateOf("900") }
    var width by remember { mutableStateOf("900") }
    var height by remember { mutableStateOf("2100") }
    var sill by remember { mutableStateOf("0") }
    var menu by remember { mutableStateOf(false) }
    Text("Двери и окна", fontSize = 21.sp, fontWeight = FontWeight.Bold)
    ExposedDropdownMenuBox(expanded = menu, onExpandedChange = { menu = it }) {
        OutlinedTextField(value = "Стена " + (wall + 1), onValueChange = {}, readOnly = true, label = { Text("Стена") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(menu) }, modifier = Modifier.menuAnchor().fillMaxWidth())
        ExposedDropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            project.room.walls.forEachIndexed { i, item ->
                DropdownMenuItem(text = { Text("Стена " + (i + 1) + " • " + item.lengthMm.toInt() + " мм") }, onClick = { wall = i; menu = false })
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = kind == OpeningType.DOOR, onClick = { kind = OpeningType.DOOR; height = "2100"; sill = "0" }, label = { Text("Дверь") })
        FilterChip(selected = kind == OpeningType.WINDOW, onClick = { kind = OpeningType.WINDOW; height = "1400"; sill = "900" }, label = { Text("Окно") })
    }
    NumberInput("Отступ от начала стены, мм", offset, { offset = it })
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberInput("Ширина, мм", width, { width = it }, Modifier.weight(1f))
        NumberInput("Высота, мм", height, { height = it }, Modifier.weight(1f))
    }
    NumberInput("Высота подоконника, мм", sill, { sill = it })
    Button(onClick = {
        val o = offset.toIntOrNull() ?: return@Button
        val w = width.toIntOrNull() ?: return@Button
        val h = height.toIntOrNull() ?: return@Button
        val s = sill.toIntOrNull() ?: 0
        val opening = runCatching { Opening(wall, o, w, h, s, kind) }.getOrNull() ?: return@Button
        val room = runCatching { RoomPlan(project.room.walls, project.room.openings + opening) }.getOrNull() ?: return@Button
        change(project.copy(room = room))
    }, modifier = Modifier.fillMaxWidth()) { Text("Добавить проём") }
    Text("Проёмы проекта: " + project.room.openings.size, fontWeight = FontWeight.Bold)
    project.room.openings.forEachIndexed { i, item ->
        Card {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text((if (item.type == OpeningType.DOOR) "Дверь" else "Окно") + " • стена " + (item.wallIndex + 1), fontWeight = FontWeight.SemiBold)
                    Text(item.widthMm.toString() + " × " + item.heightMm + " мм • отступ " + item.offsetMm + " мм")
                }
                TextButton(onClick = {
                    change(project.copy(room = RoomPlan(project.room.walls, project.room.openings.filterIndexed { index, _ -> index != i })))
                }) { Text("Удалить") }
            }
        }
    }
}

@Composable
private fun FurnitureEditor(project: ProjectDocument, change: (ProjectDocument) -> Unit) {
    var selected by remember { mutableStateOf(FurnitureKind.SOFA) }
    var menu by remember { mutableStateOf(false) }
    Text("Мебель и оборудование", fontSize = 21.sp, fontWeight = FontWeight.Bold)
    Text("Добавляйте типовые объекты, задавайте координаты и поворачивайте их на 90 градусов.")
    ExposedDropdownMenuBox(expanded = menu, onExpandedChange = { menu = it }) {
        OutlinedTextField(value = selected.title, onValueChange = {}, readOnly = true, label = { Text("Объект") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(menu) }, modifier = Modifier.menuAnchor().fillMaxWidth())
        ExposedDropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            FurnitureKind.values().forEach { item ->
                DropdownMenuItem(text = { Text(item.title + " • " + item.widthMm + "×" + item.depthMm) },
                    onClick = { selected = item; menu = false })
            }
        }
    }
    Button(onClick = {
        val rotated = selected.widthMm > project.room.boundsWidthMm || selected.depthMm > project.room.boundsDepthMm
        if (rotated) return@Button
        val x = ((project.room.boundsWidthMm - selected.widthMm) / 2).coerceAtLeast(0)
        val y = ((project.room.boundsDepthMm - selected.depthMm) / 2).coerceAtLeast(0)
        val item = FurnitureItem(UUID.randomUUID().toString(), selected, x, y)
        change(project.copy(furniture = project.furniture + item))
    }, modifier = Modifier.fillMaxWidth()) { Text("Добавить объект на план") }

    project.furniture.forEach { item ->
        key(item.id) {
            var xText by remember(item.id, item.xMm) { mutableStateOf(item.xMm.toString()) }
            var yText by remember(item.id, item.yMm) { mutableStateOf(item.yMm.toString()) }
            Card {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.kind.title + " • " + item.widthMm + " × " + item.depthMm + " мм", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInput("X, мм", xText, { xText = it }, Modifier.weight(1f))
                        NumberInput("Y, мм", yText, { yText = it }, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = {
                            val x = xText.toIntOrNull() ?: return@OutlinedButton
                            val y = yText.toIntOrNull() ?: return@OutlinedButton
                            val updated = project.furniture.map { if (it.id == item.id) it.copy(xMm = x, yMm = y) else it }
                            val valid = runCatching { project.copy(furniture = updated) }.getOrNull() ?: return@OutlinedButton
                            change(valid)
                        }, modifier = Modifier.weight(1f)) { Text("Переместить") }
                        OutlinedButton(onClick = {
                            val rotation = (item.rotationDegrees + 90) % 360
                            val rotated = rotation % 180 != 0
                            val w = if (rotated) item.depthMm else item.widthMm
                            val d = if (rotated) item.widthMm else item.depthMm
                            val x = item.xMm.coerceIn(0, (project.room.boundsWidthMm - w).coerceAtLeast(0))
                            val y = item.yMm.coerceIn(0, (project.room.boundsDepthMm - d).coerceAtLeast(0))
                            val updated = project.furniture.map { if (it.id == item.id) it.copy(xMm = x, yMm = y, rotationDegrees = rotation) else it }
                            change(project.copy(furniture = updated))
                        }, modifier = Modifier.weight(1f)) { Text("Поворот 90°") }
                        TextButton(onClick = { change(project.copy(furniture = project.furniture.filterNot { it.id == item.id })) }) { Text("Удалить") }
                    }
                }
            }
        }
    }
}

@Composable
private fun FinishEditor(project: ProjectDocument, change: (ProjectDocument) -> Unit) {
    Text("Отделочные материалы", fontSize = 21.sp, fontWeight = FontWeight.Bold)
    Text("Выбор влияет на цвет пола на плане и предварительный расчёт стоимости.")
    MaterialMenu("Покрытие пола", project.floorFinish, defaultFloorMaterials) { change(project.copy(floorFinish = it)) }
    MaterialMenu("Отделка стен", project.wallFinish, defaultWallMaterials) { change(project.copy(wallFinish = it)) }
    var floorColor by remember(project.floorFinish.id) { mutableStateOf("%08X".format(project.floorFinish.colorArgb)) }
    var wallColor by remember(project.wallFinish.id) { mutableStateOf("%08X".format(project.wallFinish.colorArgb)) }
    var floorPrice by remember(project.floorFinish.id) { mutableStateOf(project.floorFinish.pricePerSquareMetre.toInt().toString()) }
    var wallPrice by remember(project.wallFinish.id) { mutableStateOf(project.wallFinish.pricePerSquareMetre.toInt().toString()) }
    Text("Собственные цвета и цены", fontWeight = FontWeight.Bold)
    OutlinedTextField(value = floorColor, onValueChange = { floorColor = it.filter { ch -> ch.isDigit() || ch.lowercaseChar() in 'a'..'f' }.take(8) },
        label = { Text("Цвет пола ARGB (HEX)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    NumberInput("Цена пола, ₽/м²", floorPrice, { floorPrice = it })
    OutlinedButton(onClick = {
        val rawColor = floorColor.toLongOrNull(16) ?: return@OutlinedButton
        val color = if (floorColor.length <= 6) (0xFF000000L or rawColor) else rawColor
        val price = floorPrice.toDoubleOrNull()?.coerceAtLeast(0.0) ?: return@OutlinedButton
        change(project.copy(floorFinish = project.floorFinish.copy(colorArgb = color, pricePerSquareMetre = price)))
    }, modifier = Modifier.fillMaxWidth()) { Text("Применить цвет и цену пола") }
    OutlinedTextField(value = wallColor, onValueChange = { wallColor = it.filter { ch -> ch.isDigit() || ch.lowercaseChar() in 'a'..'f' }.take(8) },
        label = { Text("Цвет стен ARGB (HEX)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    NumberInput("Цена стен, ₽/м²", wallPrice, { wallPrice = it })
    OutlinedButton(onClick = {
        val rawColor = wallColor.toLongOrNull(16) ?: return@OutlinedButton
        val color = if (wallColor.length <= 6) (0xFF000000L or rawColor) else rawColor
        val price = wallPrice.toDoubleOrNull()?.coerceAtLeast(0.0) ?: return@OutlinedButton
        change(project.copy(wallFinish = project.wallFinish.copy(colorArgb = color, pricePerSquareMetre = price)))
    }, modifier = Modifier.fillMaxWidth()) { Text("Применить цвет и цену стен") }
    NumberInput("Запас пола, %", project.floorWastePercent.toInt().toString(), change = { input ->
        input.toDoubleOrNull()?.coerceIn(0.0, 100.0)?.let { value -> change(project.copy(floorWastePercent = value)) }
    })
    NumberInput("Запас стен, %", project.wallWastePercent.toInt().toString(), change = { input ->
        input.toDoubleOrNull()?.coerceIn(0.0, 100.0)?.let { value -> change(project.copy(wallWastePercent = value)) }
    })
}

@Composable
private fun MaterialMenu(label: String, current: FinishMaterial, options: List<FinishMaterial>, select: (FinishMaterial) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = menu, onExpandedChange = { menu = it }) {
        OutlinedTextField(value = current.name, onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(menu) }, modifier = Modifier.menuAnchor().fillMaxWidth())
        ExposedDropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            options.forEach { item ->
                DropdownMenuItem(text = { Text(item.name + " • " + item.pricePerSquareMetre.toInt() + " ₽/м²") },
                    onClick = { select(item); menu = false })
            }
        }
    }
}

@Composable
private fun EstimateEditor(project: ProjectDocument, change: (ProjectDocument) -> Unit) {
    val m = project.room.metrics()
    val floorQty = m.floorAreaM2 * (1 + project.floorWastePercent / 100.0)
    val wallQty = m.netWallAreaM2 * (1 + project.wallWastePercent / 100.0)
    val floorMaterialCost = floorQty * project.floorFinish.pricePerSquareMetre
    val wallMaterialCost = wallQty * project.wallFinish.pricePerSquareMetre
    val rates = project.workRates
    var floorRate by remember(rates.floorInstallationPerM2) { mutableStateOf(rates.floorInstallationPerM2.toInt().toString()) }
    var prepRate by remember(rates.wallPreparationPerM2) { mutableStateOf(rates.wallPreparationPerM2.toInt().toString()) }
    var wallRate by remember(rates.wallFinishingPerM2) { mutableStateOf(rates.wallFinishingPerM2.toInt().toString()) }
    var ceilingRate by remember(rates.ceilingPerM2) { mutableStateOf(rates.ceilingPerM2.toInt().toString()) }
    var electricalRate by remember(rates.electricalPoint) { mutableStateOf(rates.electricalPoint.toInt().toString()) }
    var plumbingRate by remember(rates.plumbingPoint) { mutableStateOf(rates.plumbingPoint.toInt().toString()) }
    val electricalCount = project.annotations.count { it.kind == AnnotationKind.SOCKET || it.kind == AnnotationKind.SWITCH || it.kind == AnnotationKind.LIGHT }
    val plumbingCount = project.annotations.count { it.kind == AnnotationKind.WATER || it.kind == AnnotationKind.DRAIN }
    val floorWork = m.floorAreaM2 * rates.floorInstallationPerM2
    val wallPrep = m.netWallAreaM2 * rates.wallPreparationPerM2
    val wallWork = m.netWallAreaM2 * rates.wallFinishingPerM2
    val ceilingWork = m.floorAreaM2 * rates.ceilingPerM2
    val electricalWork = electricalCount * rates.electricalPoint
    val plumbingWork = plumbingCount * rates.plumbingPoint
    val workTotal = floorWork + wallPrep + wallWork + ceilingWork + electricalWork + plumbingWork
    val total = floorMaterialCost + wallMaterialCost + workTotal

    Text("Предварительная смета", fontSize = 21.sp, fontWeight = FontWeight.Bold)
    Text("Материалы и ориентировочная стоимость работ. Все расценки можно изменить под свой регион и подрядчика.")
    EstimateLine("Площадь пола", "%.2f м²".format(m.floorAreaM2))
    EstimateLine("Периметр", "%.2f м".format(m.perimeterM))
    EstimateLine("Площадь стен до вычета проёмов", "%.2f м²".format(m.grossWallAreaM2))
    EstimateLine("Площадь дверей и окон", "%.2f м²".format(m.openingsAreaM2))
    EstimateLine("Чистая площадь стен", "%.2f м²".format(m.netWallAreaM2))
    Divider()
    Text("Материалы", fontWeight = FontWeight.Bold)
    EstimateLine(project.floorFinish.name + " с запасом", "%.2f м²".format(floorQty))
    EstimateLine("Материал пола", "%.0f ₽".format(floorMaterialCost))
    EstimateLine(project.wallFinish.name + " с запасом", "%.2f м²".format(wallQty))
    EstimateLine("Материал стен", "%.0f ₽".format(wallMaterialCost))
    Divider()
    Text("Расценки работ • ₽ за единицу", fontWeight = FontWeight.Bold)
    NumberInput("Монтаж пола ₽/м²", floorRate, { floorRate = it })
    NumberInput("Подготовка стен ₽/м²", prepRate, { prepRate = it })
    NumberInput("Отделка стен ₽/м²", wallRate, { wallRate = it })
    NumberInput("Потолок ₽/м²", ceilingRate, { ceilingRate = it })
    NumberInput("Электроточка ₽/шт.", electricalRate, { electricalRate = it })
    NumberInput("Сантехническая точка ₽/шт.", plumbingRate, { plumbingRate = it })
    Button(onClick = {
        val newRates = runCatching {
            WorkRates(floorRate.toDouble(), prepRate.toDouble(), wallRate.toDouble(), ceilingRate.toDouble(),
                electricalRate.toDouble(), plumbingRate.toDouble())
        }.getOrNull() ?: return@Button
        change(project.copy(workRates = newRates))
    }, modifier = Modifier.fillMaxWidth()) { Text("Сохранить расценки") }
    Divider()
    Text("Работы", fontWeight = FontWeight.Bold)
    EstimateLine("Монтаж пола", "%.0f ₽".format(floorWork))
    EstimateLine("Подготовка стен", "%.0f ₽".format(wallPrep))
    EstimateLine("Отделка стен", "%.0f ₽".format(wallWork))
    EstimateLine("Потолок", "%.0f ₽".format(ceilingWork))
    EstimateLine("Электроточки (" + electricalCount + " шт.)", "%.0f ₽".format(electricalWork))
    EstimateLine("Сантехнические точки (" + plumbingCount + " шт.)", "%.0f ₽".format(plumbingWork))
    EstimateLine("Итого работы", "%.0f ₽".format(workTotal))
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Материалы + работы", fontWeight = FontWeight.SemiBold)
            Text("%.0f ₽".format(total), fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
    }
    Text("Оценка не является договорной сметой. Не включены доставка, демонтаж, расходники, налоги и непредусмотренные работы.", fontSize = 12.sp)
}


@Composable
private fun SurfaceLayoutEditor(project: ProjectDocument, change: (ProjectDocument) -> Unit) {
    val current = project.surfaceLayout
    var width by remember(current.tileWidthMm) { mutableStateOf(current.tileWidthMm.toString()) }
    var height by remember(current.tileHeightMm) { mutableStateOf(current.tileHeightMm.toString()) }
    var grout by remember(current.groutMm) { mutableStateOf(current.groutMm.toString()) }
    var waste by remember(current.wastePercent) { mutableStateOf(current.wastePercent.toInt().toString()) }
    var pattern by remember(current.pattern) { mutableStateOf(current.pattern) }
    Text("Раскладка плитки и покрытий", fontSize = 21.sp, fontWeight = FontWeight.Bold)
    Text("Формат элемента, ширина шва, схема и запас. Количество рассчитывается по площади.")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberInput("Ширина, мм", width, { width = it }, Modifier.weight(1f))
        NumberInput("Длина, мм", height, { height = it }, Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberInput("Шов, мм", grout, { grout = it }, Modifier.weight(1f))
        NumberInput("Запас, %", waste, { waste = it }, Modifier.weight(1f))
    }
    Text("Схема раскладки", fontWeight = FontWeight.SemiBold)
    SurfacePattern.values().forEach { option ->
        FilterChip(selected = pattern == option, onClick = { pattern = option }, label = { Text(option.title) })
    }
    Button(onClick = {
        val w = width.toIntOrNull() ?: return@Button
        val h = height.toIntOrNull() ?: return@Button
        val g = grout.toIntOrNull() ?: return@Button
        val wasteValue = waste.toDoubleOrNull() ?: return@Button
        val layout = runCatching { SurfaceLayout(w, h, g, current.rotationDegrees, wasteValue, pattern) }.getOrNull() ?: return@Button
        change(project.copy(surfaceLayout = layout))
    }, modifier = Modifier.fillMaxWidth()) { Text("Применить раскладку") }
    val metrics = project.room.metrics()
    EstimateLine("Площадь пола", "%.2f м²".format(metrics.floorAreaM2))
    EstimateLine("Элементов на пол с запасом", current.piecesForArea(metrics.floorAreaM2).toString() + " шт.")
    EstimateLine("Площадь стен", "%.2f м²".format(metrics.netWallAreaM2))
    EstimateLine("Элементов на стены с запасом", current.piecesForArea(metrics.netWallAreaM2).toString() + " шт.")
    Text("Ориентировочное количество: точная подрезка зависит от раскладки и фактических размеров.")
}

@Composable
private fun WallElevationEditor(project: ProjectDocument) {
    var wallIndex by remember { mutableIntStateOf(0) }
    var menu by remember { mutableStateOf(false) }
    val wall = project.room.walls[wallIndex.coerceIn(project.room.walls.indices)]
    val openings = project.room.openings.filter { it.wallIndex == wallIndex }
    Text("Развёртки стен", fontSize = 21.sp, fontWeight = FontWeight.Bold)
    Text("Фасад выбранной стены с дверями и окнами.")
    ExposedDropdownMenuBox(expanded = menu, onExpandedChange = { menu = it }) {
        OutlinedTextField(value = "Стена " + (wallIndex + 1), onValueChange = {}, readOnly = true,
            label = { Text("Выбрать стену") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(menu) },
            modifier = Modifier.menuAnchor().fillMaxWidth())
        ExposedDropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            project.room.walls.forEachIndexed { index, item ->
                DropdownMenuItem(text = { Text("Стена " + (index + 1) + " • " + item.lengthMm.toInt() + " мм") },
                    onClick = { wallIndex = index; menu = false })
            }
        }
    }
    Canvas(Modifier.fillMaxWidth().height(260.dp).background(Color(0xFF151D24), RoundedCornerShape(12.dp)).padding(12.dp)) {
        val scale = min((size.width - 32f) / wall.lengthMm.toFloat(), (size.height - 32f) / wall.heightMm.toFloat())
        val wallWidth = wall.lengthMm.toFloat() * scale
        val wallHeight = wall.heightMm * scale
        val left = (size.width - wallWidth) / 2f
        val top = (size.height - wallHeight) / 2f
        drawRect(Color(project.wallFinish.colorArgb), Offset(left, top), Size(wallWidth, wallHeight))
        drawRect(Color(0xFF8FB8D0), Offset(left, top), Size(wallWidth, wallHeight), style = Stroke(3f))
        openings.forEach { opening ->
            val x = left + opening.offsetMm * scale
            val h = opening.heightMm * scale
            val y = top + (wall.heightMm - opening.sillHeightMm - opening.heightMm) * scale
            val color = if (opening.type == OpeningType.WINDOW) Color(0xFF77C8DE) else Color(0xFFF0F2F2)
            drawRect(color, Offset(x, y), Size(opening.widthMm * scale, h))
            drawRect(Color(0xFF8FB8D0), Offset(x, y), Size(opening.widthMm * scale, h), style = Stroke(2f))
        }
    }
    EstimateLine("Длина", wall.lengthMm.toInt().toString() + " мм")
    EstimateLine("Высота", wall.heightMm.toString() + " мм")
    EstimateLine("Толщина", wall.thicknessMm.toString() + " мм")
    EstimateLine("Проёмов", openings.size.toString())
}

@Composable
private fun EngineeringEditor(project: ProjectDocument, change: (ProjectDocument) -> Unit) {
    var kind by remember { mutableStateOf(AnnotationKind.SOCKET) }
    var x by remember { mutableStateOf("500") }
    var y by remember { mutableStateOf("500") }
    var label by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    Text("Инженерные схемы", fontSize = 21.sp, fontWeight = FontWeight.Bold)
    Text("Точки электрики, освещения, водоснабжения, канализации и вентиляции. Они отображаются поверх плана.")
    ExposedDropdownMenuBox(expanded = menu, onExpandedChange = { menu = it }) {
        OutlinedTextField(value = kind.title, onValueChange = {}, readOnly = true, label = { Text("Тип точки") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(menu) }, modifier = Modifier.menuAnchor().fillMaxWidth())
        ExposedDropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            AnnotationKind.values().forEach { option ->
                DropdownMenuItem(text = { Text(option.title) }, onClick = { kind = option; menu = false })
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberInput("X, мм", x, { x = it }, Modifier.weight(1f))
        NumberInput("Y, мм", y, { y = it }, Modifier.weight(1f))
    }
    OutlinedTextField(value = label, onValueChange = { label = it.take(40) },
        label = { Text("Подпись (необязательно)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Button(onClick = {
        val px = x.toIntOrNull() ?: return@Button
        val py = y.toIntOrNull() ?: return@Button
        val annotation = runCatching { PlanAnnotation(UUID.randomUUID().toString(), kind, px, py, label) }.getOrNull() ?: return@Button
        val updated = runCatching { project.copy(annotations = project.annotations + annotation) }.getOrNull() ?: return@Button
        change(updated)
    }, modifier = Modifier.fillMaxWidth()) { Text("Добавить точку на план") }
    project.annotations.forEach { item ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(item.kind.title + " • X " + item.xMm + " • Y " + item.yMm, Modifier.weight(1f))
            TextButton(onClick = { change(project.copy(annotations = project.annotations.filterNot { it.id == item.id })) }) { Text("Удалить") }
        }
    }
}

@Composable
private fun ExportEditor(project: ProjectDocument, onSvg: () -> Unit, onJson: () -> Unit, onCsv: () -> Unit, onImport: () -> Unit) {
    Text("Экспорт проекта", fontSize = 21.sp, fontWeight = FontWeight.Bold)
    Text("SVG — чертёж с контурами, проёмами, мебелью и инженерными символами. JSON — редактируемый проект. CSV — предварительная смета.")
    Button(onClick = onSvg, modifier = Modifier.fillMaxWidth()) { Text("Сохранить чертёж SVG") }
    OutlinedButton(onClick = onJson, modifier = Modifier.fillMaxWidth()) { Text("Сохранить проект JSON") }
    OutlinedButton(onClick = onCsv, modifier = Modifier.fillMaxWidth()) { Text("Экспортировать смету CSV") }
    OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) { Text("Импортировать проект JSON") }
    Text("Сохранение и импорт выполняются через системный диалог Android.")
}

@Composable
private fun EstimateLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun NumberInput(label: String, value: String, change: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(value = value, onValueChange = { change(it.filter { c -> c.isDigit() || c == '.' }.take(8)) },
        label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true, modifier = modifier)
}
