package com.starflix.app

import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
    val exo = remember { ExoPlayer.Builder(ctx).build() }
    val cs = Catalog.series[t.id]
    var season by remember { mutableIntStateOf(cs?.seasons?.firstOrNull()?.season ?: 0) }
    var ep by remember { mutableIntStateOf(cs?.seasons?.firstOrNull()?.episodes?.firstOrNull()?.ep ?: 0) }
    val episode = cs?.seasons?.find { it.season == season }?.episodes?.find { it.ep == ep }
    val url: String? = if (t.kind == "SERIES") episode?.stream else Catalog.movies[t.id]
    fun nextEpisode() {
        val seasons = cs?.seasons.orEmpty()
        val eps = seasons.find { it.season == season }?.episodes.orEmpty()
        val i = eps.indexOfFirst { it.ep == ep }
        if (i in 0 until eps.lastIndex) ep = eps[i + 1].ep
        else seasons.firstOrNull { it.season > season && !it.episodes.isNullOrEmpty() }?.let { season = it.season; ep = it.episodes!!.first().ep }
    }
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
    var videoH by remember { mutableIntStateOf(0) }
    var buffering by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var touch by remember { mutableIntStateOf(0) }

    DisposableEffect(exo) {
        pipEligible = true
        val l = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
            override fun onPlaybackStateChanged(state: Int) { buffering = state == Player.STATE_BUFFERING }
            override fun onTracksChanged(tracks: Tracks) { audio = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO } }
        }
        exo.addListener(l)
        onDispose {
            pipEligible = false; exo.removeListener(l); exo.release()
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    LaunchedEffect(exo) { while (true) { if (!dragging) pos = exo.currentPosition; dur = exo.duration.coerceAtLeast(0L); videoH = exo.videoSize.height; delay(400) } }
    LaunchedEffect(controlsVisible, playing, touch) { if (controlsVisible && playing) { delay(3500); controlsVisible = false } }
    LaunchedEffect(url) {
        if (url != null) { exo.setMediaItem(MediaItem.fromUri(url)); exo.prepare(); exo.playWhenReady = true } else exo.stop()
    }
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
            Box(Modifier.fillMaxSize().clickableNoRipple { controlsVisible = !controlsVisible; touch++ })
            if (url == null) Text("This title isn't in your library yet.\nAdd it to catalog.json to play it.", Modifier.align(Alignment.Center).padding(24.dp),
                color = Color.White.copy(alpha = 0.75f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            AnimatedVisibility(controlsVisible || url == null, Modifier.fillMaxSize(), enter = fadeIn(), exit = fadeOut()) {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(90.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent))))
                    Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(150.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)))))
                    Row(Modifier.align(Alignment.TopStart).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        GlassBtn(Icons.Filled.ArrowBack, 40.dp) { touch++; if (fullscreen) fullscreen = false else onBack() }
                        Text(if (t.kind == "SERIES" && cs != null) "${t.displayTitle} · S$season E$ep" else t.displayTitle, Modifier.padding(start = 12.dp),
                            fontWeight = FontWeight.SemiBold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Row(Modifier.align(Alignment.Center).offset(y = (-8).dp), horizontalArrangement = Arrangement.spacedBy(30.dp), verticalAlignment = Alignment.CenterVertically) {
                        GlassBtn(Icons.Filled.Replay10, 52.dp) { touch++; exo.seekTo((exo.currentPosition - 10_000).coerceAtLeast(0)) }
                        Box(Modifier.size(66.dp).shadow(22.dp, CircleShape, ambientColor = Accent, spotColor = Accent).clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Accent, Accent.copy(alpha = 0.7f)))).border(1.5.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                            .clickableNoRipple { touch++; if (exo.isPlaying) exo.pause() else exo.play() }, Alignment.Center) {
                            if (buffering) CircularProgressIndicator(Modifier.size(28.dp), color = Color.White, strokeWidth = 3.dp)
                            else Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, Modifier.size(36.dp), tint = Color.White)
                        }
                        GlassBtn(Icons.Filled.Forward10, 52.dp) { touch++; exo.seekTo(exo.currentPosition + 10_000) }
                    }
                    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(fmt(pos), fontSize = 12.sp)
                            Slider(if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f,
                                { dragging = true; touch++; pos = (it * dur).toLong() }, Modifier.weight(1f).height(24.dp),
                                onValueChangeFinished = { exo.seekTo(pos); dragging = false },
                                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Accent, inactiveTrackColor = Color.White.copy(alpha = 0.25f)))
                            Text(fmt(dur), fontSize = 12.sp)
                        }
                        Row(Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp)).background(Color.Black.copy(alpha = 0.25f)).padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.SkipNext, "Next", Modifier.clickableNoRipple { touch++; if (t.kind == "SERIES") nextEpisode() else exo.seekTo(0) })
                            Text("CC", Modifier.clip(RoundedCornerShape(6.dp)).background(if (cc) Accent else Color.White).padding(horizontal = 7.dp, vertical = 2.dp)
                                .clickableNoRipple { touch++; cc = !cc }, color = if (cc) Color.White else Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("${speed}x", Modifier.clickableNoRipple {
                                touch++; val l = listOf(0.5f, 1f, 1.25f, 1.5f, 2f); speed = l[(l.indexOf(speed) + 1) % l.size]; exo.setPlaybackSpeed(speed) }, fontWeight = FontWeight.SemiBold)
                            Box {
                                Text(if (quality == 0) (if (videoH > 0) "Auto · ${videoH}P" else "Auto") else "${quality}P",
                                    Modifier.clickableNoRipple { touch++; qMenu = true }, fontWeight = FontWeight.SemiBold)
                                DropdownMenu(qMenu, { qMenu = false }) {
                                    listOf(0 to "Auto", 1080 to "1080P", 720 to "720P", 480 to "480P", 360 to "360P").forEach { (q, l) ->
                                        DropdownMenuItem({ Text(l) }, { quality = q; qMenu = false })
                                    }
                                }
                            }
                            Icon(if (fullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen, "Fullscreen", Modifier.clickableNoRipple { touch++; fullscreen = !fullscreen })
                        }
                    }
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
                    Text("   ${t.year}  •  ${t.kind.lowercase().replaceFirstChar { it.uppercase() }}${if (cs != null) "  •  ${cs.seasons.orEmpty().size} seasons" else ""}", color = Color.White.copy(alpha = 0.75f))
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
                        Text(if (t.kind == "SERIES") "Season %02d · Episode %02d".format(season, ep) else "Full movie", fontWeight = FontWeight.SemiBold)
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
                if (t.kind == "SERIES" && cs != null) {
                    EpisodesCard(cs, season, ep, exo, audio, audioName, { audioName = it }, { season = it.season; ep = it.episodes?.firstOrNull()?.ep ?: 0 }, { ep = it })
                } else Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(26.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Audio", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    AudioPill(exo, audio, audioName) { audioName = it }
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

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.height(48.dp).widthIn(min = 48.dp).clip(RoundedCornerShape(16.dp))
        .background(if (selected) Brush.linearGradient(listOf(Accent, Accent.copy(alpha = 0.7f))) else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.05f))))
        .border(1.dp, if (selected) Color.White.copy(alpha = 0.35f) else GlassLine, RoundedCornerShape(16.dp))
        .clickableNoRipple(onClick).padding(horizontal = 18.dp), Alignment.Center) {
        Text(label, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

@Composable
private fun AudioPill(exo: ExoPlayer, audio: List<Tracks.Group>, name: String, onName: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(Modifier.glass(CircleShape).clickableNoRipple { open = true }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.GraphicEq, null, tint = Accent); Text("  Audio: $name  ", fontWeight = FontWeight.SemiBold); Icon(Icons.Filled.ExpandMore, null)
        }
        DropdownMenu(open, { open = false }) {
            if (audio.size < 2) DropdownMenuItem({ Text("Original") }, { open = false })
            audio.forEachIndexed { i, g ->
                val f = g.getTrackFormat(0); val n = f.label ?: f.language ?: if (i == 0) "Original" else "Track ${i + 1}"
                DropdownMenuItem({ Text(n) }, {
                    exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon().setOverrideForType(TrackSelectionOverride(g.mediaTrackGroup, 0)).build()
                    onName(n); open = false })
            }
        }
    }
}

@Composable
private fun SeasonPill(seasons: List<CSeason>, season: Int, onPick: (CSeason) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(Modifier.glass(CircleShape).clickableNoRipple { open = true }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Layers, null, tint = Accent); Text("  Season %02d  ".format(season), fontWeight = FontWeight.SemiBold); Icon(Icons.Filled.ExpandMore, null)
        }
        DropdownMenu(open, { open = false }) {
            seasons.forEach { sn -> DropdownMenuItem({ Text("Season %02d".format(sn.season)) }, { onPick(sn); open = false }) }
        }
    }
}

@Composable
private fun EpisodesCard(cs: CSeries, season: Int, ep: Int, exo: ExoPlayer, audio: List<Tracks.Group>, audioName: String,
                         onAudioName: (String) -> Unit, onSeason: (CSeason) -> Unit, onEp: (Int) -> Unit) {
    var showAll by remember { mutableStateOf(true) }
    val eps = cs.seasons?.find { it.season == season }?.episodes.orEmpty()
    Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(26.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Episodes", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("${eps.size} episodes", Modifier.glass(CircleShape).padding(horizontal = 12.dp, vertical = 5.dp), fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AudioPill(exo, audio, audioName, onAudioName)
            SeasonPill(cs.seasons.orEmpty(), season) { showAll = true; onSeason(it) }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Chip("All", showAll) { showAll = true }
            eps.forEach { e -> Chip("%02d".format(e.ep), !showAll && e.ep == ep) { showAll = false; onEp(e.ep) } }
        }
        if (showAll) eps.forEach { e ->
            val on = e.ep == ep
            Row(Modifier.fillMaxWidth().glass(RoundedCornerShape(20.dp), if (on) Accent else Color.White).clickable { onEp(e.ep) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(if (on) Accent else Color.White.copy(alpha = 0.1f)), Alignment.Center) { Icon(Icons.Filled.PlayArrow, null, tint = Color.White) }
                Text("E%02d  ${e.title ?: "Episode ${e.ep}"}".format(e.ep), Modifier.padding(start = 12.dp), fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

@Composable
private fun GlassBtn(icon: ImageVector, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    Box(Modifier.size(size).glass(CircleShape).background(Color.Black.copy(alpha = 0.25f)).clickableNoRipple(onClick), Alignment.Center) {
        Icon(icon, null, Modifier.size(size * 0.55f), tint = Color.White)
    }
}
