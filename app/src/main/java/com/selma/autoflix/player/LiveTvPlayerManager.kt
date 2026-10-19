package com.selma.autoflix.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.selma.autoflix.R
import com.selma.autoflix.data.model.LiveTvChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@OptIn(UnstableApi::class)
class LiveTvPlayerManager(
    private val context: Context,
    private val isCarMode: Boolean = false
) {
    private val appContext = context.applicationContext
    private var exoPlayer: ExoPlayer? = null
    private var activePlayerView: PlayerView? = null

    private val _currentChannel = MutableStateFlow<LiveTvChannel?>(null)
    val currentChannel: StateFlow<LiveTvChannel?> = _currentChannel.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _playerError = MutableStateFlow<String?>(null)
    val playerError: StateFlow<String?> = _playerError.asStateFlow()

    private fun getOrCreatePlayer(): ExoPlayer {
        return exoPlayer ?: run {
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    /* minBufferMs = */ 3000,
                    /* maxBufferMs = */ 12000,
                    /* bufferForPlaybackMs = */ 500,
                    /* bufferForPlaybackAfterRebufferMs = */ 1000
                )
                .build()

            val trackSelector = DefaultTrackSelector(appContext).apply {
                if (isCarMode) {
                    parameters = buildUponParameters()
                        .setMaxVideoSize(854, 480)
                        .setMaxVideoBitrate(1_800_000)
                        .build()
                }
            }

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()

            ExoPlayer.Builder(appContext)
                .setLoadControl(loadControl)
                .setTrackSelector(trackSelector)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .build()
                .also { player ->
                    player.addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            _isBuffering.value = (playbackState == Player.STATE_BUFFERING)
                            _isPlaying.value = (playbackState == Player.STATE_READY && player.playWhenReady)
                        }

                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            _isPlaying.value = isPlaying
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            _playerError.value = error.localizedMessage ?: "خطای پخش جریان ویدیو"
                            _isBuffering.value = false
                            _isPlaying.value = false
                        }
                    })
                    exoPlayer = player
                }
        }
    }

    /**
     * Creates a TextureView-backed PlayerView (via R.layout.live_tv_player_view)
     * so video frames render directly inside Android Auto VirtualDisplay projection
     * and Jetpack Compose containers without black-screen SurfaceView overlay issues.
     */
    fun createPlayerView(viewContext: Context, useController: Boolean = false): PlayerView {
        val playerView = LayoutInflater.from(viewContext)
            .inflate(R.layout.live_tv_player_view, null, false) as PlayerView
        playerView.useController = useController
        playerView.keepScreenOn = true
        attachToView(playerView)
        return playerView
    }

    fun attachToView(playerView: PlayerView) {
        val player = getOrCreatePlayer()
        if (activePlayerView != null && activePlayerView !== playerView) {
            PlayerView.switchTargetView(player, activePlayerView, playerView)
        } else {
            playerView.player = player
        }
        playerView.keepScreenOn = true
        activePlayerView = playerView
    }

    fun detachPlayerView(playerView: PlayerView?) {
        if (playerView == null) return
        if (activePlayerView === playerView) {
            playerView.player = null
            activePlayerView = null
        }
        (playerView.parent as? ViewGroup)?.removeView(playerView)
    }

    fun play(channel: LiveTvChannel) {
        _currentChannel.value = channel
        _playerError.value = null
        val player = getOrCreatePlayer()

        var streamUrl = channel.streamUrl
        if (isCarMode && streamUrl.contains(".telewebion.net")) {
            // Downsample telewebion stream in Car Mode so Android Auto projection + decoding run at 60fps with zero lag
            streamUrl = when {
                streamUrl.contains("/ek/faratar/live/2160p/") ->
                    streamUrl.replace("/ek/faratar/live/2160p/", "/ek/faratar/live/720p/")
                streamUrl.contains("/1080p/") ->
                    streamUrl.replace("/1080p/", "/480p/")
                streamUrl.contains("/720p/") ->
                    streamUrl.replace("/720p/", "/480p/")
                else -> streamUrl
            }
        }

        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("AutoFlix")
            .setConnectTimeoutMs(12000)
            .setReadTimeoutMs(12000)
            .setAllowCrossProtocolRedirects(true)

        val mediaItem = MediaItem.fromUri(Uri.parse(streamUrl))
        val mediaSource: MediaSource = if (streamUrl.endsWith(".m3u8", ignoreCase = true) || streamUrl.contains("/live/")) {
            HlsMediaSource.Factory(dataSourceFactory)
                .setAllowChunklessPreparation(true)
                .createMediaSource(mediaItem)
        } else {
            DefaultMediaSourceFactory(dataSourceFactory).createMediaSource(mediaItem)
        }

        player.setMediaSource(mediaSource)
        player.prepare()
        player.playWhenReady = true
    }

    fun sendCurrentChannelToCar() {
        val ch = _currentChannel.value ?: return
        try {
            appContext.getSharedPreferences("car_handoff", Context.MODE_PRIVATE)
                .edit()
                .putString("live_stream_url", ch.streamUrl)
                .putString("live_name", ch.name)
                .putString("live_logo_url", ch.logoUrl)
                .putString("live_group", ch.category)
                .putString("live_quality", ch.quality)
                .putBoolean("live_favorite", ch.isFavorite)
                .apply()

            val intent = Intent("com.selma.autoflix.action.PLAY_LIVE_TV_ON_CAR").apply {
                setPackage(appContext.packageName)
                putExtra("url", ch.streamUrl)
                putExtra("name", ch.name)
                putExtra("category", ch.category)
                putExtra("quality", ch.quality)
            }
            appContext.sendBroadcast(intent)
            pause()
            Toast.makeText(appContext, "پخش گوشی متوقف و شبکه به خودرو فرستاده شد", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(appContext, "ارسال پخش زنده به خودرو انجام نشد", Toast.LENGTH_SHORT).show()
        }
    }

    fun pause() {
        exoPlayer?.playWhenReady = false
    }

    fun resume() {
        exoPlayer?.playWhenReady = true
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.playWhenReady) {
            pause()
        } else {
            resume()
        }
    }

    fun playNext(channels: List<LiveTvChannel>) {
        if (channels.isEmpty()) return
        val current = _currentChannel.value ?: channels.first()
        val currentIndex = channels.indexOfFirst { it.streamUrl == current.streamUrl }
        val nextIndex = if (currentIndex == -1) 0 else (currentIndex + 1) % channels.size
        play(channels[nextIndex])
    }

    fun playPrevious(channels: List<LiveTvChannel>) {
        if (channels.isEmpty()) return
        val current = _currentChannel.value ?: channels.first()
        val currentIndex = channels.indexOfFirst { it.streamUrl == current.streamUrl }
        val prevIndex = if (currentIndex <= 0) channels.size - 1 else currentIndex - 1
        play(channels[prevIndex])
    }

    fun stop() {
        exoPlayer?.stop()
        _currentChannel.value = null
        _isPlaying.value = false
        _isBuffering.value = false
    }

    fun release() {
        activePlayerView?.player = null
        activePlayerView = null
        exoPlayer?.release()
        exoPlayer = null
        _currentChannel.value = null
        _isPlaying.value = false
        _isBuffering.value = false
    }
}
