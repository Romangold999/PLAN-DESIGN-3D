package com.romgold.plandesign3d

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import kotlin.math.min

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("plan_design", MODE_PRIVATE)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF315C78))) {
                var widthMm by remember { mutableStateOf(prefs.getInt("width", 5000).toString()) }
                var depthMm by remember { mutableStateOf(prefs.getInt("depth", 4000).toString()) }
                var saved by remember { mutableStateOf(false) }
                val w = widthMm.toIntOrNull()?.coerceIn(1000, 30000) ?: 5000
                val d = depthMm.toIntOrNull()?.coerceIn(1000, 30000) ?: 4000
                Scaffold(topBar = { TopAppBar(title = { Text("PLAN DESIGN 3D", fontWeight = FontWeight.Bold) }) }) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("План помещения", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("Размеры задаются в миллиметрах. Масштаб экрана не меняет геометрию.")
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            DimensionField("Ширина, мм", widthMm, { widthMm = it }, Modifier.weight(1f))
                            DimensionField("Глубина, мм", depthMm, { depthMm = it }, Modifier.weight(1f))
                        }
                        Box(Modifier.fillMaxWidth().weight(1f).background(Color(0xFFF3F5F7), RoundedCornerShape(16.dp)).padding(12.dp), contentAlignment = Alignment.Center) {
                            Canvas(Modifier.fillMaxSize().pointerInput(w, d) { detectTapGestures { saved = false } }) {
                                val scale = min((size.width - 56f) / w, (size.height - 56f) / d)
                                val rw = w * scale
                                val rh = d * scale
                                val left = (size.width - rw) / 2f
                                val top = (size.height - rh) / 2f
                                drawRect(Color.White, Offset(left, top), androidx.compose.ui.geometry.Size(rw, rh))
                                drawRect(Color(0xFF315C78), Offset(left, top), androidx.compose.ui.geometry.Size(rw, rh), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8f))
                                drawLine(Color(0xFF78909C), Offset(left, top + rh / 2f), Offset(left + rw, top + rh / 2f), 1.5f, cap = StrokeCap.Round)
                                drawLine(Color(0xFF78909C), Offset(left + rw / 2f, top), Offset(left + rw / 2f, top + rh), 1.5f, cap = StrokeCap.Round)
                            }
                        }
                        Text("Прямоугольное помещение • ${w} × ${d} мм", fontWeight = FontWeight.Medium)
                        Button(onClick = {
                            val project = JSONObject().put("schemaVersion", 1).put("widthMm", w).put("depthMm", d)
                            prefs.edit().putInt("width", w).putInt("depth", d).putString("projectJson", project.toString()).apply()
                            saved = true
                        }, modifier = Modifier.fillMaxWidth()) { Text("Сохранить проект") }
                        if (saved) Text("Сохранено локально на устройстве", color = Color(0xFF28734A))
                        Text("Первый технический экран. Проёмы, произвольные стены и 3D-редактор добавятся следующими этапами.", fontSize = 12.sp, color = Color.Gray)
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
