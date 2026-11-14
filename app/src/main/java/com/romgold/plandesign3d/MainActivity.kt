package com.romgold.plandesign3d

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romgold.plandesign3d.domain.RoomPlan
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("plan_design", MODE_PRIVATE)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF315C78))) {
                var widthMm by remember { mutableStateOf(prefs.getInt("width", 5000).toString()) }
                var depthMm by remember { mutableStateOf(prefs.getInt("depth", 4000).toString()) }
                var saved by remember { mutableStateOf(false) }
                var zoom by remember { mutableFloatStateOf(1f) }
                var pan by remember { mutableStateOf(Offset.Zero) }
                val w = widthMm.toIntOrNull()?.coerceIn(1000, 30000) ?: 5000
                val d = depthMm.toIntOrNull()?.coerceIn(1000, 30000) ?: 4000
                val room = remember(w, d) { RoomPlan.rectangle(w, d) }

                Scaffold(topBar = { TopAppBar(title = { Text("PLAN DESIGN 3D", fontWeight = FontWeight.Bold) }) }) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("План помещения", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("Точные размеры в миллиметрах. Разведите два пальца для масштаба, перетаскивайте для перемещения.")
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            DimensionField("Ширина, мм", widthMm, { widthMm = it; saved = false }, Modifier.weight(1f))
                            DimensionField("Глубина, мм", depthMm, { depthMm = it; saved = false }, Modifier.weight(1f))
                        }
                        Box(
                            Modifier.fillMaxWidth().weight(1f)
                                .background(Color(0xFFF3F5F7), RoundedCornerShape(16.dp)).padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(
                                Modifier.fillMaxSize().pointerInput(w, d) {
                                    detectTransformGestures { _, panChange, zoomChange, _ ->
                                        zoom = (zoom * zoomChange).coerceIn(0.5f, 5f)
                                        pan += panChange
                                    }
                                }
                            ) {
                                val fit = min((size.width - 64f) / w, (size.height - 64f) / d)
                                val scale = fit * zoom
                                val origin = Offset((size.width - w * scale) / 2f, (size.height - d * scale) / 2f) + pan
                                room.walls.forEach { wall ->
                                    val start = Offset(origin.x + wall.start.xMm * scale, origin.y + wall.start.yMm * scale)
                                    val end = Offset(origin.x + wall.end.xMm * scale, origin.y + wall.end.yMm * scale)
                                    drawLine(Color(0xFF315C78), start, end, strokeWidth = 8f)
                                }
                            }
                        }
                        Text("Прямоугольное помещение • ${w} × ${d} мм", fontWeight = FontWeight.Medium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { zoom = 1f; pan = Offset.Zero }, modifier = Modifier.weight(1f)) { Text("Сбросить вид") }
                            Button(onClick = {
                                val wallsJson = JSONArray()
                                room.walls.forEach { wall ->
                                    wallsJson.put(JSONObject()
                                        .put("startXmm", wall.start.xMm).put("startYmm", wall.start.yMm)
                                        .put("endXmm", wall.end.xMm).put("endYmm", wall.end.yMm)
                                        .put("thicknessMm", wall.thicknessMm).put("heightMm", wall.heightMm))
                                }
                                val project = JSONObject().put("schemaVersion", 1)
                                    .put("widthMm", w).put("depthMm", d).put("walls", wallsJson)
                                prefs.edit().putInt("width", w).putInt("depth", d)
                                    .putString("projectJson", project.toString()).apply()
                                saved = true
                            }, modifier = Modifier.weight(1f)) { Text("Сохранить") }
                        }
                        if (saved) Text("Проект сохранён локально", color = Color(0xFF28734A))
                        Text("Этап прототипа: прямоугольное помещение. Добавление отдельных стен, дверей, окон и 3D — следующие этапы.", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
private fun DimensionField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(5)) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier
    )
}
