@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.romgold.plandesign3d

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romgold.plandesign3d.data.ProjectStore
import com.romgold.plandesign3d.domain.*
import java.util.UUID
import kotlin.math.min

private enum class EditorTab(val title: String) {
    PLAN("План"), OPENINGS("Проёмы"), FURNITURE("Мебель"), FINISHES("Материалы"), ESTIMATE("Расчёт")
}

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = ProjectStore(this)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF315C78), secondary = Color(0xFF668A78))) {
                var project by remember {
                    mutableStateOf(store.loadLast() ?: ProjectDocument(UUID.randomUUID().toString(), "Мой проект", RoomPlan.rectangle(5000, 4000)))
                }
                var tab by remember { mutableStateOf(EditorTab.PLAN) }
                val undo = remember { mutableStateListOf<ProjectDocument>() }
                val redo = remember { mutableStateListOf<ProjectDocument>() }
                var saved by remember { mutableStateOf(true) }

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

                Scaffold(topBar = {
                    TopAppBar(title = {
                        Column {
                            Text("PLAN DESIGN 3D", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(project.name, fontSize = 11.sp)
                        }
                    }, actions = {
                        TextButton(enabled = undo.isNotEmpty(), onClick = {
                            if (undo.isNotEmpty()) { redo.add(project); project = undo.removeAt(undo.lastIndex); saved = false }
                        }) { Text("↶") }
                        TextButton(enabled = redo.isNotEmpty(), onClick = {
                            if (redo.isNotEmpty()) { undo.add(project); project = redo.removeAt(redo.lastIndex); saved = false }
                        }) { Text("↷") }
                        TextButton(onClick = { store.save(project); saved = true }) { Text("Сохранить") }
                    })
                }) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding)) {
                        ScrollableTabRow(selectedTabIndex = tab.ordinal) {
                            EditorTab.values().forEach { item ->
                                Tab(selected = tab == item, onClick = { tab = item }, text = { Text(item.title) })
                            }
                        }
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            when (tab) {
                                EditorTab.PLAN -> PlanEditor(project, change)
                                EditorTab.OPENINGS -> OpeningEditor(project, change)
                                EditorTab.FURNITURE -> FurnitureEditor(project, change)
                                EditorTab.FINISHES -> FinishEditor(project, change)
                                EditorTab.ESTIMATE -> EstimateEditor(project)
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
    Text("План помещения", fontSize = 21.sp, fontWeight = FontWeight.Bold)
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
    Box(Modifier.fillMaxWidth().height(300.dp).background(Color(0xFFF0F2F2), RoundedCornerShape(16.dp)).padding(8.dp)) {
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
            drawRect(Color(project.floorFinish.colorArgb).copy(alpha = 0.45f), origin, Size(bw * scale, bd * scale))
            project.room.walls.forEachIndexed { index, wall ->
                val a = point(wall.start.xMm, wall.start.yMm)
                val b = point(wall.end.xMm, wall.end.yMm)
                val stroke = (wall.thicknessMm * scale).coerceAtLeast(5f)
                drawLine(Color(0xFF315C78), a, b, strokeWidth = stroke)
                project.room.openings.filter { it.wallIndex == index }.forEach { opening ->
                    val length = wall.lengthMm.toFloat()
                    val from = opening.offsetMm / length
                    val to = (opening.offsetMm + opening.widthMm) / length
                    val p1 = Offset(a.x + (b.x - a.x) * from, a.y + (b.y - a.y) * from)
                    val p2 = Offset(a.x + (b.x - a.x) * to, a.y + (b.y - a.y) * to)
                    drawLine(Color(0xFFF0F2F2), p1, p2, strokeWidth = stroke + 3f)
                    if (opening.type == OpeningType.WINDOW) drawLine(Color(0xFF38A6C8), p1, p2, strokeWidth = 3f)
                }
            }
            project.furniture.forEach { item ->
                val p = point(item.xMm, item.yMm)
                val s = Size(item.widthMm * scale, item.depthMm * scale)
                drawRect(Color(0xFFB77B55), p, s)
                drawRect(Color(0xFF704D3A), p, s, style = Stroke(2f))
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
    Text("Добавляйте типовые объекты. Их габариты и положение отображаются на плане.")
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
        val x = ((project.room.boundsWidthMm - selected.widthMm) / 2).coerceAtLeast(0)
        val y = ((project.room.boundsDepthMm - selected.depthMm) / 2).coerceAtLeast(0)
        val item = FurnitureItem(UUID.randomUUID().toString(), selected, x, y)
        change(project.copy(furniture = project.furniture + item))
    }, modifier = Modifier.fillMaxWidth()) { Text("Добавить объект на план") }
    project.furniture.forEach { item ->
        Card {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.kind.title, fontWeight = FontWeight.SemiBold)
                    Text(item.widthMm.toString() + " × " + item.depthMm + " мм • X " + item.xMm + ", Y " + item.yMm)
                }
                TextButton(onClick = { change(project.copy(furniture = project.furniture.filterNot { it.id == item.id })) }) { Text("Удалить") }
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
    NumberInput("Запас пола, %", project.floorWastePercent.toInt().toString()) {
        it.toDoubleOrNull()?.coerceIn(0.0, 100.0)?.let { value -> change(project.copy(floorWastePercent = value)) }
    }
    NumberInput("Запас стен, %", project.wallWastePercent.toInt().toString()) {
        it.toDoubleOrNull()?.coerceIn(0.0, 100.0)?.let { value -> change(project.copy(wallWastePercent = value)) }
    }
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
private fun EstimateEditor(project: ProjectDocument) {
    val m = project.room.metrics()
    val floorQty = m.floorAreaM2 * (1 + project.floorWastePercent / 100.0)
    val wallQty = m.netWallAreaM2 * (1 + project.wallWastePercent / 100.0)
    val floorCost = floorQty * project.floorFinish.pricePerSquareMetre
    val wallCost = wallQty * project.wallFinish.pricePerSquareMetre
    Text("Предварительный расчёт", fontSize = 21.sp, fontWeight = FontWeight.Bold)
    Text("Расчёт количества материалов по геометрии. Цены ориентировочные и редактируются в каталоге.")
    EstimateLine("Площадь пола", "%.2f м²".format(m.floorAreaM2))
    EstimateLine("Периметр", "%.2f м".format(m.perimeterM))
    EstimateLine("Площадь стен до вычета проёмов", "%.2f м²".format(m.grossWallAreaM2))
    EstimateLine("Площадь дверей и окон", "%.2f м²".format(m.openingsAreaM2))
    EstimateLine("Чистая площадь стен", "%.2f м²".format(m.netWallAreaM2))
    Divider()
    EstimateLine(project.floorFinish.name + " с запасом", "%.2f м²".format(floorQty))
    EstimateLine("Стоимость пола", "%.0f ₽".format(floorCost))
    EstimateLine(project.wallFinish.name + " с запасом", "%.2f м²".format(wallQty))
    EstimateLine("Стоимость стен", "%.0f ₽".format(wallCost))
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Итого по выбранным материалам", fontWeight = FontWeight.SemiBold)
            Text("%.0f ₽".format(floorCost + wallCost), fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
    }
    Text("Не включены работы, доставка, подготовка поверхностей, расходники и инженерные системы.", fontSize = 12.sp)
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
