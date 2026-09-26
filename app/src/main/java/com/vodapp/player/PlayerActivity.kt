package com.vodapp.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView

@UnstableApi
class PlayerActivity : ComponentActivity() {

    private var player: ExoPlayer? = null

    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_TITLE = "title"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.getStringExtra(EXTRA_URL) ?: ""
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "播放"

        val playerView = PlayerView(this)
        setContentView(playerView)

        val dataSourceFactory = DataSourceFactory.build(this)

        val p = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
        player = p
        playerView.player = p
        playerView.keepScreenOn = true

        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setMediaId(title)
            .build()

        // 播放错误时自动重试，避免黑屏卡死
        p.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                // 错误后延迟一点点重新 prepare 续播
                playerView.postDelayed({
                    p.prepare()
                    p.playWhenReady = true
                }, 1000)
            }
        })

        p.setMediaItem(mediaItem)
        p.prepare()
        p.playWhenReady = true
    }

    override fun onResume() {
        super.onResume()
        player?.playWhenReady = true
    }

    override fun onStop() {
        super.onStop()
        player?.playWhenReady = false
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}
