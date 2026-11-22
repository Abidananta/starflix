package com.starflix.app

import android.app.PictureInPictureParams
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// Replace with your own licensed stream / your backend's URL for a title.
const val DEMO_STREAM = "https://cdn1.discoveryftp.net/Movies/Hindi/2026/Bhooth%20Bangla/Bhooth.Bangla.2026.1080p.NF.WEB-DL.Hindi-Dual.x264.mkv"
@Volatile var pipEligible = false

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        enableEdgeToEdge()
        setContent { App(this) }
    }
    fun enterPip() { enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build()) }
    override fun onUserLeaveHint() { if (pipEligible) enterPip() }
}

@Composable
fun App(activity: MainActivity) {
    val d = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(d.density, Prefs.textScale)) {
        MaterialTheme(colorScheme = darkColorScheme(primary = Accent, background = Color(0xFF0B0D14))) {
            var tab by remember { mutableIntStateOf(0) }
            var selected by remember { mutableStateOf<Title?>(null) }
            BackHandler(selected != null) { selected = null }
            Surface(Modifier.fillMaxSize().aurora(), color = Color.Transparent, contentColor = Color.White) {
            Box(Modifier.fillMaxSize()) {
                when (tab) {
                    0 -> HomeScreen { selected = it }
                    1 -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Search is coming soon", color = Color.White.copy(alpha = 0.6f)) }
                    2 -> LibraryScreen { selected = it }
                    else -> SettingsScreen()
                }
                BottomNav(tab, { tab = it }, Modifier.align(Alignment.BottomCenter))
                selected?.let { t -> key(t.id) { DetailsScreen(t, activity, { selected = null }, { selected = it }) } }
            }
            }
        }
    }
}

@Composable
fun LibraryScreen(onOpen: (Title) -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
        Text("Library", fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 12.dp))
        if (Prefs.favorites.isEmpty()) Text("Tap the heart on any title to save it here.", color = Color.White.copy(alpha = 0.6f))
        LazyVerticalGrid(GridCells.Fixed(2), contentPadding = PaddingValues(bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(Prefs.favorites.toList()) { t ->
                Column(Modifier.clickableNoRipple { onOpen(t) }) {
                    AsyncImage(t.poster, t.displayTitle, Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(22.dp)), contentScale = ContentScale.Crop)
                    Text(t.displayTitle, Modifier.padding(top = 6.dp), fontWeight = FontWeight.SemiBold, maxLines = 2)
                }
            }
        }
    }
}
