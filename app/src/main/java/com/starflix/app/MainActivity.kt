package com.starflix.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// Replace with your own licensed stream / your backend's URL for a title.
const val DEMO_STREAM = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF5B7CFF), background = Color(0xFF0B0D14))) {
                HomeScreen { t ->
                    startActivity(Intent(this, PlayerActivity::class.java)
                        .putExtra("url", DEMO_STREAM).putExtra("title", t.displayTitle))
                }
            }
        }
    }
}
