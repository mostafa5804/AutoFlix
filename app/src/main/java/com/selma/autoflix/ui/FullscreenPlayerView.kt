package com.selma.autoflix.ui

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.selma.autoflix.R
import com.selma.autoflix.data.model.LiveTvChannel
import com.selma.autoflix.player.LiveTvPlayerManager
import com.selma.autoflix.ui.theme.AccentCoral
import com.selma.autoflix.ui.theme.LiveGreen
import kotlinx.coroutines.delay

@Composable
fun FullscreenPlayerView(
    playerManager: LiveTvPlayerManager,
    channels: List<LiveTvChannel>,
    onExitFullscreen: () -> Unit,
    onEnterPip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentChannel by playerManager.currentChannel.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val isBuffering by playerManager.isBuffering.collectAsState()
    val playerError by playerManager.playerError.collectAsState()

    var showControls by remember { mutableStateOf(true) }

    // Auto-hide controls after 4 seconds of inactivity
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4000)
            showControls = false
        }
    }

    BackHandler {
        onExitFullscreen()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showControls = !showControls
            }
    ) {
        // Video Surface
        AndroidView(
            factory = { ctx ->
                playerManager.createPlayerView(ctx, useController = false)
            },
            update = { pv ->
                playerManager.attachToView(pv)
            },
            modifier = Modifier.fillMaxSize()
        )

        // Buffering Indicator
        if (isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(56.dp)
                    .align(Alignment.Center),
                color = AccentCoral,
                strokeWidth = 4.dp
            )
        }

        // Error message if playback failed
        if (playerError != null) {
            Surface(
                color = Color.Black.copy(alpha = 0.85f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = playerError ?: "خطا در پخش زنده",
                        color = Color(0xFFFF5252),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { currentChannel?.let { playerManager.play(it) } },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCoral),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("تلاش مجدد", color = Color.White)
                    }
                }
            }
        }

        // Interactive Overlay Controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.75f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.75f)
                            )
                        )
                    )
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Actions: PiP and Exit Fullscreen
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = onExitFullscreen,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_fullscreen_exit_24),
                                contentDescription = "خروج از تمام‌صفحه",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = onEnterPip,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_pip_24),
                                contentDescription = "صفحه روی صفحه (تصویر در تصویر)",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = {
                                playerManager.sendCurrentChannelToCar()
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_send_to_car_24),
                                contentDescription = "ارسال به خودرو",
                                tint = Color.White
                            )
                        }
                    }

                    // Right Info: Channel Name, Quality, Live Dot
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        currentChannel?.quality?.let { q ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = q,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(99.dp),
                            color = Color.Black.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(LiveGreen)
                                )
                                Text(
                                    text = "زنده",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = currentChannel?.name ?: "",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Center Navigation & Play/Pause Controls
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(36.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Channel
                    IconButton(
                        onClick = { playerManager.playPrevious(channels) },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.22f))
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_skip_previous_24),
                            contentDescription = "شبکه قبلی",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Main Play / Pause Button
                    IconButton(
                        onClick = { playerManager.togglePlayPause() },
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(AccentCoral)
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (isPlaying) R.drawable.ic_pause_24 else R.drawable.ic_play_24
                            ),
                            contentDescription = if (isPlaying) "توقف" else "پخش",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Next Channel
                    IconButton(
                        onClick = { playerManager.playNext(channels) },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.22f))
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_skip_next_24),
                            contentDescription = "شبکه بعدی",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Bottom Bar Hint
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "برای نمایش یا پنهان‌سازی کنترل‌ها، روی صفحه ضربه بزنید",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
