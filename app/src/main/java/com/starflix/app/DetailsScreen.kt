package com.starflix.app

import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
fun DetailsScreen(t: Title, activity: MainActivity, onBack: () -> Unit, onOpen: (Title) -> Unit) {
    val ctx = LocalContext.current
    val exo = remember { ExoPlayer.Builder(ctx).build().apply { setMediaItem(MediaItem.fromUri(DEMO_STREAM)); prepare(); playWhenReady = true } }
    var playing by remember { mutableStateOf(true) }
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf(false) }
    var speed by remember { mutableFloatStateOf(1f) }
    var cc by remember { mutableStateOf(false) }
    var quality by remember { mutableIntStateOf(Prefs.quality) }
    var fullscreen by remember { mutableStateOf(false) }
    var audio by remember { mutableStateOf<List<Tracks.Group>>(emptyList()) }
    var audioName by remember { mutableStateOf("Original") }
    var recs by remember { mutableStateOf<List<Title>>(emptyList()) }
    var showInfo by remember { mutableStateOf(false) }
    var qMenu by remember { mutableStateOf(false) }
    var aMenu by remember { mutableStateOf(false) }

    DisposableEffect(exo) {
        pipEligible = true
        val l = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
            override fun onTracksChanged(tracks: Tracks) { audio = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO } }
        }
        exo.addListener(l)
        onDispose {
            pipEligible = false; exo.removeListener(l); exo.release()
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    LaunchedEffect(exo) { while (true) { if (!dragging) pos = exo.currentPosition; dur = exo.duration.coerceAtLeast(0L); delay(400) } }
    LaunchedEffect(quality) {
        exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon()
            .setMaxVideoSize(Int.MAX_VALUE, if (quality == 0) Int.MAX_VALUE else quality).build()
    }
    LaunchedEffect(cc) { exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !cc).build() }
    LaunchedEffect(fullscreen) {
        activity.requestedOrientation = if (fullscreen) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
    LaunchedEffect(t.id) { recs = runCatching { api.recs(t.recType, t.id).results.clean().filter { it.poster != null } }.getOrDefault(emptyList()) }
    BackHandler(fullscreen) { fullscreen = false }

    Column(Modifier.fillMaxSize().aurora()
        .then(if (fullscreen) Modifier else Modifier.statusBarsPadding())) {
        // ---- Player ----
        Box(Modifier.fillMaxWidth().then(if (fullscreen) Modifier.weight(1f) else Modifier.aspectRatio(16f / 9f)).background(Color.Black)) {
            AndroidView(factory = { PlayerView(it).apply { useController = false; resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT; player = exo } }, Modifier.fillMaxSize())
            Row(Modifier.align(Alignment.TopStart).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton({ if (fullscreen) fullscreen = false else onBack() }) { Icon(Icons.Filled.ArrowBack, "Back") }
                Text(t.displayTitle, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(36.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundIcon(Icons.Filled.Replay10, 44.dp, Color.Transparent) { exo.seekTo((exo.currentPosition - 10_000).coerceAtLeast(0)) }
                RoundIcon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, 66.dp, Accent) { if (exo.isPlaying) exo.pause() else exo.play() }
                RoundIcon(Icons.Filled.Forward10, 44.dp, Color.Transparent) { exo.seekTo(exo.currentPosition + 10_000) }
            }
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(fmt(pos), fontSize = 12.sp)
                    Slider(if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f,
                        { dragging = true; pos = (it * dur).toLong() }, Modifier.weight(1f).height(24.dp),
                        onValueChangeFinished = { exo.seekTo(pos); dragging = false },
                        colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent, inactiveTrackColor = Color.White.copy(alpha = 0.3f)))
                    Text(fmt(dur), fontSize = 12.sp)
                }
                Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.SkipNext, "Next", Modifier.clickableNoRipple { exo.seekTo(0) })
                    Text("CC", Modifier.clip(RoundedCornerShape(6.dp)).background(if (cc) Accent else Color.White).padding(horizontal = 7.dp, vertical = 2.dp)
                        .clickableNoRipple { cc = !cc }, color = if (cc) Color.White else Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("${if (speed % 1f == 0f) speed.toInt().toString() else speed.toString()}x", Modifier.clickableNoRipple {
                        val l = listOf(0.5f, 1f, 1.25f, 1.5f, 2f); speed = l[(l.indexOf(speed) + 1) % l.size]; exo.setPlaybackSpeed(speed) }, fontWeight = FontWeight.SemiBold)
                    Box {
                        Text(if (quality == 0) "Auto" else "${quality}P", Modifier.clickableNoRipple { qMenu = true }, fontWeight = FontWeight.SemiBold)
                        DropdownMenu(qMenu, { qMenu = false }) {
                            listOf(0 to "Auto", 1080 to "1080P", 720 to "720P", 480 to "480P", 360 to "360P").forEach { (q, l) ->
                                DropdownMenuItem({ Text(l) }, { quality = q; qMenu = false })
                            }
                        }
                    }
                    Icon(if (fullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen, "Fullscreen", Modifier.clickableNoRipple { fullscreen = !fullscreen })
                }
            }
        }
        // ---- Details ----
        if (!fullscreen) LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text(t.displayTitle, fontSize = 26.sp, fontWeight = FontWeight.Bold) }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0x33F5A524)).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, null, Modifier.size(16.dp), tint = Color(0xFFF5A524)); Text("  ${t.rating}", fontWeight = FontWeight.Bold)
                    }
                    Text("   ${t.year}  •  ${t.kind.lowercase().replaceFirstChar { it.uppercase() }}", color = Color.White.copy(alpha = 0.75f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    t.genreList.forEach { Text(it, Modifier.clip(CircleShape).background(Accent.copy(alpha = 0.18f)).border(1.dp, Accent.copy(alpha = 0.4f), CircleShape).padding(horizontal = 16.dp, vertical = 8.dp), color = Accent, fontWeight = FontWeight.SemiBold) }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().glass(RoundedCornerShape(24.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(Accent), Alignment.Center) { Icon(Icons.Filled.PlayArrow, null) }
                    Column(Modifier.padding(start = 14.dp)) {
                        Text("NOW PLAYING", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                        Text(if (t.kind == "SERIES") "Latest episode" else "Full movie", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().glass(RoundedCornerShape(26.dp)).padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly) {
                    val fav = Prefs.isFav(t)
                    Action(if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "Favorite", if (fav) Accent else Color.White) { Prefs.toggleFav(t) }
                    Action(Icons.Filled.Download, "Download") { Toast.makeText(ctx, "Downloads aren't available yet", Toast.LENGTH_SHORT).show() }
                    Action(Icons.Filled.PictureInPictureAlt, "PiP") { activity.enterPip() }
                    Action(Icons.Filled.Info, "Info") { showInfo = true }
                }
            }
            item {
                Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(26.dp)).padding(18.dp)) {
                    Text("Audio", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Box(Modifier.padding(top = 12.dp)) {
                        Row(Modifier.glass(CircleShape).clickableNoRipple { aMenu = true }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.GraphicEq, null, tint = Accent); Text("  Audio: $audioName  ", fontWeight = FontWeight.SemiBold); Icon(Icons.Filled.ExpandMore, null)
                        }
                        DropdownMenu(aMenu, { aMenu = false }) {
                            if (audio.size < 2) DropdownMenuItem({ Text("Original") }, { aMenu = false })
                            audio.forEachIndexed { i, g ->
                                val f = g.getTrackFormat(0); val name = f.label ?: f.language ?: if (i == 0) "Original" else "Track ${i + 1}"
                                DropdownMenuItem({ Text(name) }, {
                                    exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon().setOverrideForType(TrackSelectionOverride(g.mediaTrackGroup, 0)).build()
                                    audioName = name; aMenu = false })
                            }
                        }
                    }
                }
            }
            if (recs.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(4.dp).height(24.dp).background(Accent)); Text("  More like this", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(recs) { r ->
                            Column(Modifier.width(140.dp).clickableNoRipple { onOpen(r) }) {
                                AsyncImage(r.poster, r.displayTitle, Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(20.dp)), contentScale = ContentScale.Crop)
                                Text(r.displayTitle, Modifier.padding(top = 6.dp), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("${r.rating}  ${r.year}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }
    }
    if (showInfo) AlertDialog(onDismissRequest = { showInfo = false }, confirmButton = { TextButton({ showInfo = false }) { Text("Close") } },
        title = { Text(t.displayTitle) }, text = { Text(t.overview?.takeIf { it.isNotBlank() } ?: "No description available.") })
}

@Composable
private fun RoundIcon(icon: ImageVector, size: androidx.compose.ui.unit.Dp, bg: Color, onClick: () -> Unit) {
    Box(Modifier.size(size).clip(CircleShape).background(bg).then(if (bg == Color.Transparent) Modifier.border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape) else Modifier)
        .clickableNoRipple(onClick), Alignment.Center) { Icon(icon, null, Modifier.size(size * 0.55f)) }
}

@Composable
private fun Action(icon: ImageVector, label: String, tint: Color = Color.White, onClick: () -> Unit) {
    Column(Modifier.clickableNoRipple(onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(56.dp).glass(CircleShape), Alignment.Center) { Icon(icon, label, tint = tint) }
        Text(label, Modifier.padding(top = 6.dp), fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
    }
}
