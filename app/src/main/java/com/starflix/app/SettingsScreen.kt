package com.starflix.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val COLORS = listOf(0xFF5B7CFF, 0xFFE5484D, 0xFF30A46C, 0xFFF5A524, 0xFFB26CFF, 0xFF18C0D8).map { it.toInt() }

@Composable
fun SettingsScreen() {
    var page by remember { mutableStateOf<String?>(null) }
    BackHandler(page != null) { page = null }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(12.dp).verticalScroll(rememberScrollState())) {
        if (page == null) {
            Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(4.dp, 4.dp, 4.dp, 14.dp))
            Row(Modifier.fillMaxWidth().padding(bottom = 18.dp).glass(RoundedCornerShape(30.dp)).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(58.dp).clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(Accent, Color(0xFF8B3DFF)))), Alignment.Center) {
                    Icon(Icons.Filled.Star, null, Modifier.size(30.dp))
                }
                Column(Modifier.padding(start = 16.dp)) {
                    Text("Star Flix", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Version ${BuildConfig.VERSION_NAME}", fontSize = 13.sp, color = Color.White.copy(alpha = 0.65f))
                }
            }
            val items = listOf(
                MenuItem(Icons.Filled.Palette, "Appearance", "Accent color, text size", Color(0xFF5B7CFF)),
                MenuItem(Icons.Filled.PlayCircleOutline, "Playback", "Preferred quality", Color(0xFF30A46C)),
                MenuItem(Icons.Filled.Shield, "Content", "Adult content filter", Color(0xFFF5A524)),
                MenuItem(Icons.Filled.Info, "About", "Version and credits", Color(0xFFB26CFF)))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { items.forEach { MenuRow(it) { page = it.title } } }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                IconButton({ page = null }) { Icon(Icons.Filled.ArrowBack, "Back") }
                Text(page!!, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.glass(RoundedCornerShape(28.dp)).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when (page) {
                    "Appearance" -> {
                        Text("Accent color", fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            COLORS.forEach { c ->
                                Box(Modifier.size(38.dp).clip(CircleShape).background(Color(c))
                                    .border(if (Prefs.accent == c) BorderStroke(3.dp, Color.White) else BorderStroke(0.dp, Color.Transparent), CircleShape)
                                    .clickable { Prefs.accent = c; Prefs.save() })
                            }
                        }
                        Text("Text size", fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Small" to 0.9f, "Default" to 1f, "Large" to 1.15f).forEach { (l, v) ->
                                FilterChip(Prefs.textScale == v, { Prefs.textScale = v; Prefs.save() }, { Text(l) })
                            }
                        }
                    }
                    "Playback" -> {
                        Text("Preferred quality", fontWeight = FontWeight.SemiBold)
                        listOf(0 to "Auto", 1080 to "1080p", 720 to "720p", 480 to "480p", 360 to "360p").forEach { (q, l) ->
                            Row(Modifier.fillMaxWidth().clickable { Prefs.quality = q; Prefs.save() }, verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(Prefs.quality == q, { Prefs.quality = q; Prefs.save() }); Text(l)
                            }
                        }
                    }
                    "Content" -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Adult content filter", fontWeight = FontWeight.SemiBold)
                            Text("Hide adult titles from all lists", fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f))
                        }
                        Switch(Prefs.adultFilter, { Prefs.adultFilter = it; Prefs.save() })
                    }
                    else -> {
                        Text("Star Flix", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Version ${BuildConfig.VERSION_NAME}", color = Color.White.copy(alpha = 0.7f))
                        Text("Title data and images are provided by TMDB. This product uses the TMDB API but is not endorsed or certified by TMDB.", fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }
        }
        Spacer(Modifier.height(120.dp))
    }
}

private data class MenuItem(val icon: ImageVector, val title: String, val sub: String, val color: Color)

@Composable
private fun MenuRow(m: MenuItem, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().glass(RoundedCornerShape(26.dp)).clickable(onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(m.color, m.color.copy(alpha = 0.45f)))), Alignment.Center) {
            Icon(m.icon, null, Modifier.size(26.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(m.title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Text(m.sub, fontSize = 14.sp, color = Color.White.copy(alpha = 0.65f))
        }
        Box(Modifier.size(34.dp).glass(CircleShape), Alignment.Center) { Icon(Icons.Filled.ChevronRight, null, Modifier.size(20.dp)) }
    }
}
