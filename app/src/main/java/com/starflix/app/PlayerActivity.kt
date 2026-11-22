package com.starflix.app

import android.app.PictureInPictureParams
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class PlayerActivity : ComponentActivity() {
    private lateinit var player: ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = ExoPlayer.Builder(this).build().apply {
            setMediaItem(MediaItem.fromUri(intent.getStringExtra("url") ?: DEMO_STREAM))
            prepare(); playWhenReady = true
        }
        setContent { AndroidView(factory = { PlayerView(it).apply { this.player = this@PlayerActivity.player } }) }
    }

    override fun onUserLeaveHint() {
        enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build())
    }

    override fun onDestroy() { player.release(); super.onDestroy() }
}
