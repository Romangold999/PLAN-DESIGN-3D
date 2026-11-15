package com.romgold.plandesign3d

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class ModelingTab(val title: String) {
    BLENDER("Blender"),
    FREECAD("FreeCAD")
}

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF83B8D8),
                    onPrimary = Color(0xFF10212C),
                    secondary = Color(0xFF9BC7B1),
                    background = Color(0xFF11161B),
                    surface = Color(0xFF1B2229),
                    surfaceVariant = Color(0xFF29343D),
                    onSurface = Color(0xFFE5EBEF),
                    onSurfaceVariant = Color(0xFFB4C0C8),
                    outline = Color(0xFF53616B)
                )
            ) {
                var tab by remember { mutableStateOf(ModelingTab.BLENDER) }
                Scaffold(
                    containerColor = Color(0xFF11161B),
                    topBar = {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color(0xFF1B2229),
                                titleContentColor = Color(0xFFE5EBEF)
                            ),
                            title = {
                                Column {
                                    Text("BLENDER  /  FreeCAD", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Android modeling workspaces", fontSize = 11.sp, color = Color(0xFFB4C0C8))
                                }
                            }
                        )
                    }
                ) { padding ->
                    Column(
                        Modifier.fillMaxSize().padding(padding).background(Color(0xFF11161B))
                    ) {
                        Row(
                            Modifier.fillMaxWidth()
                                .background(Color(0xFF202830))
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ModelingTab.values().forEach { item ->
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
                            Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            when (tab) {
                                ModelingTab.BLENDER -> BlenderWorkspace()
                                ModelingTab.FREECAD -> FreeCADWorkspace()
                            }
                        }
                    }
                }
            }
        }
    }
}
