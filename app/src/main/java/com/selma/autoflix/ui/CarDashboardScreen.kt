package com.selma.autoflix.ui

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.selma.autoflix.R
import com.selma.autoflix.player.LiveTvPlayerManager
import com.selma.autoflix.ui.components.BrandLogo
import com.selma.autoflix.ui.theme.*
import com.selma.autoflix.ui.viewmodel.MainViewModel

enum class CarSection {
    LIVE_TV,
    BROWSER,
    SAVED_SITES
}

data class CarCategoryItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val color: Color,
    val iconRes: Int
)

val CAR_CATEGORIES = listOf(
    CarCategoryItem("national", "سراسری", "شبکه ۱ تا ۵، ورزش، نسیم", Color(0xFFE04F26), R.drawable.ic_live_tv_24),
    CarCategoryItem("provincial", "استانی", "سهند، ارومیه، فارس، اصفهان", Color(0xFF0F6259), R.drawable.ic_notification_24),
    CarCategoryItem("movies", "فیلم و سریال", "MBC Persia، جم، سینما", Color(0xFFD97706), R.drawable.ic_movie_24),
    CarCategoryItem("music", "موزیک", "رادیو جوان، PMC، آونگ", Color(0xFFBE123C), R.drawable.ic_music_24),
    CarCategoryItem("kids", "کودک", "پویا، جم کیدز، کارتون", Color(0xFF15803D), R.drawable.ic_child_24),
    CarCategoryItem("sports", "ورزشی", "Varzish TV، فوتبال، شبکه ۳", Color(0xFF0369A1), R.drawable.ic_sports_24)
)

@Composable
fun CarDashboardScreen(
    viewModel: MainViewModel,
    playerManager: LiveTvPlayerManager,
    browserView: BrowserView? = null,
    activeSection: CarSection = CarSection.LIVE_TV,
    onSectionChange: (CarSection) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val allChannels by viewModel.channels.collectAsState()
    val savedSites by viewModel.savedSites.collectAsState()
    val currentChannel by playerManager.currentChannel.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val isBuffering by playerManager.isBuffering.collectAsState()
    val playerError by playerManager.playerError.collectAsState()

    var activeCatId by remember { mutableStateOf<String?>(null) }
    var isFullscreen by remember { mutableStateOf(false) }
    var showFullscreenControls by remember { mutableStateOf(true) }

    // Auto-play first channel if none is currently selected once channels load in Live TV mode
    LaunchedEffect(allChannels, activeSection) {
        if (activeSection == CarSection.LIVE_TV && currentChannel == null && allChannels.isNotEmpty()) {
            playerManager.play(allChannels.first())
        }
    }

    val displayedChannels = remember(allChannels, activeCatId) {
        if (activeCatId == null) {
            allChannels
        } else {
            allChannels.filter { MainViewModel.matchesCategory(it, activeCatId!!) }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F17))
    ) {
        if (isFullscreen && activeSection == CarSection.LIVE_TV) {
            // Fullscreen Car Video Monitor (TextureView-backed for Android Auto VirtualDisplay)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { showFullscreenControls = !showFullscreenControls }
            ) {
                AndroidView(
                    factory = { ctx ->
                        playerManager.createPlayerView(ctx, useController = false)
                    },
                    update = { pv ->
                        playerManager.attachToView(pv)
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (isBuffering) {
                    CircularProgressIndicator(
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                if (showFullscreenControls) {
                    // Top bar in Fullscreen
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .background(Color(0xB30B0F17))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            BrandLogo(size = 28.dp)
                            Column {
                                Text(
                                    text = currentChannel?.name ?: "پخش زنده",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = currentChannel?.category ?: "AUTOFLIX CAR OS",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Button(
                            onClick = { isFullscreen = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_fullscreen_exit_24),
                                contentDescription = "خروج از تمام صفحه",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "بازگشت به داشبورد",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Bottom Steering Controls in Fullscreen
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 18.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xCC111726))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(18.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { playerManager.playPrevious(displayedChannels.ifEmpty { allChannels }) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_skip_previous_24),
                                contentDescription = "قبلی",
                                tint = Color(0xFF38BDF8)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("قبلی", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { playerManager.togglePlayPause() },
                            colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = if (isPlaying) R.drawable.ic_pause_24 else R.drawable.ic_play_24),
                                contentDescription = "پخش/توقف",
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPlaying) "توقف" else "پخش",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Button(
                            onClick = { playerManager.playNext(displayedChannels.ifEmpty { allChannels }) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_skip_next_24),
                                contentDescription = "بعدی",
                                tint = Color(0xFF38BDF8)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("بعدی", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {

                // Top Automotive Status & Navigation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF111726))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Brand & HUD Mode
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BrandLogo(size = 34.dp)
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "AUTOFLIX CAR OS",
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    letterSpacing = 0.5.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x3338BDF8))
                                        .border(1.dp, Color(0x6638BDF8), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "ANDROID AUTO",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "سیستم مالتی‌مدیا و تلویزیون زنده خودرو",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Right: Section Tabs (Live TV / Browser / Saved Sites) & Live Status Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CarTopTabButton(
                            title = "تلویزیون زنده",
                            iconRes = R.drawable.ic_live_tv_24,
                            selected = activeSection == CarSection.LIVE_TV,
                            onClick = { onSectionChange(CarSection.LIVE_TV) }
                        )
                        if (browserView != null) {
                            CarTopTabButton(
                                title = "مرورگر وب",
                                iconRes = R.drawable.ic_website_48,
                                selected = activeSection == CarSection.BROWSER,
                                onClick = {
                                    playerManager.pause()
                                    onSectionChange(CarSection.BROWSER)
                                }
                            )
                            CarTopTabButton(
                                title = "سایت‌ها",
                                iconRes = R.drawable.ic_bookmark_add_24,
                                selected = activeSection == CarSection.SAVED_SITES,
                                onClick = {
                                    onSectionChange(CarSection.SAVED_SITES)
                                }
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F172A))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Text(
                                    text = "LIVE • HD",
                                    color = Color(0xFF10B981),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                when (activeSection) {
                    CarSection.BROWSER -> {
                        if (browserView != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(18.dp))
                            ) {
                                AndroidView(
                                    factory = {
                                        (browserView.parent as? ViewGroup)?.removeView(browserView)
                                        browserView.ensureLoaded("https://telewebion.com")
                                        browserView
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    CarSection.SAVED_SITES -> {
                        Card(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF111726)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = "سایت‌های ذخیره‌شده (انتخاب برای باز شدن در مانیتور خودرو)",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(savedSites) { site ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color(0xFF0F172A))
                                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                                                .clickable {
                                                    playerManager.pause()
                                                    browserView?.loadUrl(site.url)
                                                    onSectionChange(CarSection.BROWSER)
                                                }
                                                .padding(12.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF1E293B)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.ic_website_48),
                                                        contentDescription = null,
                                                        tint = Color(0xFF38BDF8),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Column {
                                                    Text(
                                                        text = site.title,
                                                        color = Color.White,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = site.url,
                                                        color = Color(0xFF94A3B8),
                                                        fontSize = 10.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    CarSection.LIVE_TV -> {
                        // Main Split Body for Live TV
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // LEFT PANE: Persistent Stream Monitor & Big Steering Controls (42% width)
                            Card(
                                modifier = Modifier
                                    .weight(0.42f)
                                    .fillMaxHeight(),
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF111726)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Monitor View (TextureView-backed PlayerView for Android Auto projection)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .padding(bottom = 10.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color.Black)
                                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
                                            .clickable {
                                                if (currentChannel != null) {
                                                    isFullscreen = true
                                                }
                                            }
                                    ) {
                                        if (currentChannel != null) {
                                            AndroidView(
                                                factory = { ctx ->
                                                    playerManager.createPlayerView(ctx, useController = false)
                                                },
                                                update = { pv ->
                                                    playerManager.attachToView(pv)
                                                },
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }

                                        if (isBuffering) {
                                            CircularProgressIndicator(
                                                color = Color(0xFF38BDF8),
                                                strokeWidth = 2.5.dp,
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .align(Alignment.Center)
                                            )
                                        }

                                        // Overlay info & badges
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(8.dp),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0x990F172A))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = when {
                                                            playerError != null -> "خطا در پخش"
                                                            isBuffering -> "در حال اتصال..."
                                                            else -> "LIVE"
                                                        },
                                                        color = if (playerError != null) Color(0xFFF43F5E) else Color(0xFF10B981),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                }

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(Color(0x990F172A))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = currentChannel?.quality ?: "HD",
                                                            color = Color(0xFFFDE047),
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }

                                                    IconButton(
                                                        onClick = { isFullscreen = true },
                                                        modifier = Modifier
                                                            .size(26.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0x990F172A))
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(id = R.drawable.ic_fullscreen_24),
                                                            contentDescription = "تمام صفحه",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            if (currentChannel == null) {
                                                Column(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    BrandLogo(size = 40.dp)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "یک شبکه را برای پخش انتخاب کنید",
                                                        color = Color(0xFF94A3B8),
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            } else {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0xB3000000))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = currentChannel!!.name,
                                                            color = Color.White,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = currentChannel!!.category,
                                                            color = Color(0xFF38BDF8),
                                                            fontSize = 10.sp,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Oversized Steering-Wheel Touch Controls
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Previous Channel Button
                                        Button(
                                            onClick = {
                                                playerManager.playPrevious(displayedChannels.ifEmpty { allChannels })
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(54.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_skip_previous_24),
                                                    contentDescription = "قبلی",
                                                    tint = Color(0xFF38BDF8),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text("قبلی", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        // Play / Pause Big Center Button
                                        Button(
                                            onClick = {
                                                if (isPlaying) {
                                                    playerManager.pause()
                                                } else if (currentChannel != null) {
                                                    playerManager.resume()
                                                } else if (displayedChannels.isNotEmpty()) {
                                                    playerManager.play(displayedChannels.first())
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1.3f)
                                                .height(54.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Terracotta)
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    painter = painterResource(id = if (isPlaying) R.drawable.ic_pause_24 else R.drawable.ic_play_24),
                                                    contentDescription = "پخش/توقف",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Text(
                                                    text = if (isPlaying) "توقف" else "پخش",
                                                    fontSize = 11.sp,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }

                                        // Next Channel Button
                                        Button(
                                            onClick = {
                                                playerManager.playNext(displayedChannels.ifEmpty { allChannels })
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(54.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_skip_next_24),
                                                    contentDescription = "بعدی",
                                                    tint = Color(0xFF38BDF8),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text("بعدی", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            // RIGHT PANE: Category Menu or Channel Deck (58% width)
                            Card(
                                modifier = Modifier
                                    .weight(0.58f)
                                    .fillMaxHeight(),
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF111726)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(14.dp)
                                ) {
                                    // Deck Header
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (activeCatId != null) {
                                            Button(
                                                onClick = { activeCatId = null },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowForward,
                                                    contentDescription = "بازگشت",
                                                    tint = Color(0xFF38BDF8),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("همه دسته‌ها", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            Text(
                                                text = "دسته‌بندی‌های شبکه",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }

                                        Text(
                                            text = if (activeCatId != null) "${displayedChannels.size} کانال" else "۶ دسته‌بندی",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Content: Categories Grid (if activeCatId == null) or Channels List
                                    if (activeCatId == null) {
                                        LazyVerticalGrid(
                                            columns = GridCells.Fixed(3),
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            items(CAR_CATEGORIES) { cat ->
                                                val count = allChannels.count { MainViewModel.matchesCategory(it, cat.id) }
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(95.dp)
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .background(
                                                            Brush.linearGradient(
                                                                listOf(cat.color.copy(alpha = 0.25f), Color(0xFF0F172A))
                                                            )
                                                        )
                                                        .border(1.dp, cat.color.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                                        .clickable { activeCatId = cat.id }
                                                        .padding(10.dp)
                                                ) {
                                                    Column(
                                                        modifier = Modifier.fillMaxSize(),
                                                        verticalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                painter = painterResource(id = cat.iconRes),
                                                                contentDescription = cat.title,
                                                                tint = cat.color,
                                                                modifier = Modifier.size(22.dp)
                                                            )
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(6.dp))
                                                                    .background(Color(0x33000000))
                                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                                            ) {
                                                                Text(
                                                                    text = "$count",
                                                                    color = Color.White,
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }

                                                        Column {
                                                            Text(
                                                                text = cat.title,
                                                                color = Color.White,
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.ExtraBold
                                                            )
                                                            Text(
                                                                text = cat.subtitle,
                                                                color = Color(0xFF94A3B8),
                                                                fontSize = 9.sp,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        // Channel Cards Grid for selected category
                                        LazyVerticalGrid(
                                            columns = GridCells.Fixed(2),
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(displayedChannels) { ch ->
                                                val isCurrent = currentChannel?.streamUrl == ch.streamUrl
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(
                                                            if (isCurrent) Color(0xFF1E3A5F) else Color(0xFF0F172A)
                                                        )
                                                        .border(
                                                            1.dp,
                                                            if (isCurrent) Color(0xFF38BDF8) else Color(0xFF1E293B),
                                                            RoundedCornerShape(14.dp)
                                                        )
                                                        .clickable { playerManager.play(ch) }
                                                        .padding(10.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.weight(1f),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(32.dp)
                                                                    .clip(CircleShape)
                                                                    .background(if (isCurrent) Terracotta else Color(0xFF1E293B)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    painter = painterResource(id = if (isCurrent && isPlaying) R.drawable.ic_pause_24 else R.drawable.ic_play_24),
                                                                    contentDescription = null,
                                                                    tint = Color.White,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                            Column {
                                                                Text(
                                                                    text = ch.name,
                                                                    color = Color.White,
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                                Text(
                                                                    text = ch.quality ?: "FHD",
                                                                    color = if (isCurrent) Color(0xFF38BDF8) else Color(0xFF64748B),
                                                                    fontSize = 10.sp
                                                                )
                                                            }
                                                        }

                                                        IconButton(
                                                            onClick = { viewModel.toggleChannelFavorite(ch) },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = if (ch.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                                contentDescription = "علاقه‌مندی",
                                                                tint = if (ch.isFavorite) Color(0xFFF43F5E) else Color(0xFF64748B),
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CarTopTabButton(
    title: String,
    iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Terracotta else Color(0xFF1E293B)
        ),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        modifier = Modifier.height(34.dp)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = title,
            tint = Color.White,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
