package com.starflix.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

private val Glass = Color.White.copy(alpha = 0.05f)
private val GlassLine = Color.White.copy(alpha = 0.10f)
private val Accent = Color(0xFF5B7CFF)

private class Rail(val icon: ImageVector, val title: String, val items: List<Title>, val ranked: Boolean = false)

@Composable
fun HomeScreen(onPlay: (Title) -> Unit) {
    var reload by remember { mutableIntStateOf(0) }
    var hero by remember { mutableStateOf<List<Title>>(emptyList()) }
    var rails by remember { mutableStateOf<List<Rail>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(reload) {
        error = null
        val k = BuildConfig.TMDB_API_KEY
        if (k.isBlank()) { error = "Add your TMDB_API_KEY (see README) and rebuild."; return@LaunchedEffect }
        try {
            val t = api.trending(k).results.filter { it.poster != null }
            hero = t.take(8)
            rails = listOf(
                Rail(Icons.Filled.BarChart, "Trending Now", t.take(20), ranked = true),
                Rail(Icons.Filled.Whatshot, "Popular Movies", api.popular(k).results),
                Rail(Icons.Filled.LiveTv, "Popular Series", api.popularTv(k).results),
                Rail(Icons.Filled.Star, "Top Rated", api.topRated(k).results),
            )
        } catch (e: Exception) { error = "Couldn't load titles. Check your connection and refresh." }
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1B2040), Color(0xFF0B0D14))))) {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Star Flix", fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = { reload++ }, modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.1f))) {
                        Icon(Icons.Filled.Refresh, "Refresh")
                    }
                }
            }
            error?.let { item { Text(it, Modifier.padding(16.dp), color = Color(0xFFFF8A80)) } }
            if (hero.isNotEmpty()) item { Hero(hero, onPlay) }
            itemsIndexed(rails) { _, r -> RailSection(r, onPlay) }
        }
        BottomNav(Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun Hero(items: List<Title>, onPlay: (Title) -> Unit) {
    val pager = rememberPagerState { items.size }
    Column {
        HorizontalPager(pager, contentPadding = PaddingValues(horizontal = 28.dp), pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth().height(460.dp)) { i ->
            val t = items[i]
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(32.dp))) {
                AsyncImage(t.posterLarge, t.displayTitle, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f)), startY = 500f)))
                Column(Modifier.align(Alignment.BottomCenter).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(t.genres, fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                    Text(t.displayTitle, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${t.year}   ${t.kind}", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(vertical = 6.dp))
                    Button(onClick = { onPlay(t) }, Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) {
                        Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Play", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.Center) {
            repeat(items.size) { i ->
                val on = pager.currentPage == i
                Box(Modifier.padding(3.dp).height(6.dp).width(if (on) 22.dp else 6.dp).clip(CircleShape)
                    .background(if (on) Accent else Color.White.copy(alpha = 0.25f)))
            }
        }
    }
}

@Composable
private fun RailSection(r: Rail, onPlay: (Title) -> Unit) {
    Column(Modifier.padding(horizontal = 12.dp).clip(RoundedCornerShape(28.dp)).background(Glass)
        .border(BorderStroke(1.dp, GlassLine), RoundedCornerShape(28.dp)).padding(vertical = 14.dp)) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(r.icon, null, Modifier.size(20.dp), tint = Color.White.copy(alpha = 0.7f))
            Spacer(Modifier.width(8.dp))
            Text(r.title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.width(10.dp))
            Text("${r.items.size}", fontSize = 12.sp, modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).padding(horizontal = 9.dp, vertical = 3.dp))
        }
        LazyRow(contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            itemsIndexed(r.items.filter { it.poster != null }) { i, t ->
                Column(Modifier.width(146.dp).clip(RoundedCornerShape(20.dp)).clickableNoRipple { onPlay(t) }) {
                    Box {
                        AsyncImage(t.poster, t.displayTitle, Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(22.dp)), contentScale = ContentScale.Crop)
                        if (r.ranked && i < 10) Text("${i + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(8.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)).padding(horizontal = 9.dp, vertical = 4.dp))
                    }
                    Text(t.displayTitle, Modifier.padding(top = 8.dp), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(Icons.Filled.Star, null, Modifier.size(13.dp), tint = Color(0xFFB39DFF))
                        Text("  ${t.rating}   ${t.year}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNav(modifier: Modifier) {
    var sel by remember { mutableIntStateOf(0) }
    val tabs = listOf(Icons.Filled.Movie to "Home", Icons.Filled.Search to "Search", Icons.Filled.VideoLibrary to "Library", Icons.Filled.Settings to "Settings")
    Row(modifier.navigationBarsPadding().padding(bottom = 12.dp).clip(RoundedCornerShape(36.dp))
        .background(Color(0xFF1C2033).copy(alpha = 0.88f)).border(BorderStroke(1.dp, GlassLine), RoundedCornerShape(36.dp)).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        tabs.forEachIndexed { i, (icon, label) ->
            val on = sel == i
            Row(Modifier.clip(CircleShape).background(if (on) Accent.copy(alpha = 0.22f) else Color.Transparent)
                .clickableNoRipple { sel = i }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, label, tint = if (on) Accent else Color.White.copy(alpha = 0.8f))
                if (on) { Spacer(Modifier.width(8.dp)); Text(label, color = Accent, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = onClick))
