package com.starflix.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

val Glass = Color.White.copy(alpha = 0.06f)
val GlassLine = Color.White.copy(alpha = 0.12f)
val Accent get() = Color(Prefs.accent)

fun List<Title>.clean() = filter { !(Prefs.adultFilter && it.adult == true) }

fun fmt(ms: Long): String {
    val s = ms / 1000; val h = s / 3600; val m = (s % 3600) / 60; val c = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, c) else "%02d:%02d".format(m, c)
}

/** Frosted-glass surface: soft white gradient fill + light-catching edge. */
fun Modifier.glass(shape: Shape = RoundedCornerShape(28.dp), tint: Color = Color.White): Modifier = this
    .clip(shape)
    .background(Brush.verticalGradient(listOf(tint.copy(alpha = 0.15f), tint.copy(alpha = 0.04f))))
    .border(BorderStroke(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.38f), Color.White.copy(alpha = 0.05f)))), shape)

/** Dark base with soft colour glows behind the glass, tinted by the accent colour. */
@Composable
fun Modifier.aurora(): Modifier {
    val a = Accent
    return this.background(Color(0xFF070910)).drawBehind {
        val w = size.width; val h = size.height
        fun blob(c: Color, x: Float, y: Float, r: Float) =
            drawCircle(Brush.radialGradient(listOf(c, Color.Transparent), Offset(x, y), r), r, Offset(x, y))
        blob(a.copy(alpha = 0.50f), w * 0.95f, h * 0.06f, w * 1.0f)
        blob(Color(0xFF8B3DFF).copy(alpha = 0.32f), w * 0.0f, h * 0.45f, w * 0.9f)
        blob(Color(0xFF18C0D8).copy(alpha = 0.22f), w * 0.9f, h * 0.88f, w * 0.9f)
    }
}

@Composable
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)

@Composable
fun BottomNav(sel: Int, onSel: (Int) -> Unit, modifier: Modifier) {
    val tabs = listOf(Icons.Filled.Home to "Home", Icons.Filled.Search to "Search", Icons.Filled.VideoLibrary to "Library", Icons.Filled.Settings to "Settings")
    Row(modifier.navigationBarsPadding().padding(bottom = 14.dp)
        .glass(RoundedCornerShape(36.dp), tint = Color(0xFFB8C4FF)).background(Color(0xFF0E1222).copy(alpha = 0.55f)).padding(7.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        tabs.forEachIndexed { i, (icon, label) ->
            val on = sel == i
            Row(Modifier.clip(CircleShape)
                .then(if (on) Modifier.background(Brush.horizontalGradient(listOf(Accent.copy(alpha = 0.55f), Accent.copy(alpha = 0.2f))))
                    .border(1.dp, Accent.copy(alpha = 0.55f), CircleShape) else Modifier)
                .clickableNoRipple { onSel(i) }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, label, tint = if (on) Color.White else Color.White.copy(alpha = 0.65f))
                if (on) { Spacer(Modifier.width(8.dp)); Text(label, color = Color.White, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}
