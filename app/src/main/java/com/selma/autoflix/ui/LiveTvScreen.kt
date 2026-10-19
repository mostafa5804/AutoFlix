package com.selma.autoflix.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.selma.autoflix.R
import com.selma.autoflix.data.model.LiveTvChannel
import com.selma.autoflix.player.LiveTvPlayerManager
import com.selma.autoflix.ui.theme.*
import com.selma.autoflix.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

// 6 Graphic Category definitions matching index.html
data class GraphicCategory(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconRes: Int,
    val gradientColors: List<Color>,
    val accentColor: Color
)

val MAIN_CATEGORIES = listOf(
    GraphicCategory(
        id = "national",
        title = "سراسری",
        subtitle = "شبکه‌های ۱ تا ۵، پویا، نسیم",
        iconRes = R.drawable.ic_live_tv_24,
        gradientColors = listOf(Color(0xFFE04F26), Color(0xFFC23E18)),
        accentColor = Color(0xFFFFCCBC)
    ),
    GraphicCategory(
        id = "provincial",
        title = "استانی",
        subtitle = "مراکز ۳۱ استان کشور",
        iconRes = R.drawable.ic_notification_24,
        gradientColors = listOf(Color(0xFF0F6259), Color(0xFF0A433D)),
        accentColor = Color(0xFFB2DFDB)
    ),
    GraphicCategory(
        id = "movies",
        title = "فیلم و سریال",
        subtitle = "آی‌فیلم، نمایش، تماشا",
        iconRes = R.drawable.ic_movie_24,
        gradientColors = listOf(Color(0xFF4F46E5), Color(0xFF3730A3)),
        accentColor = Color(0xFFC7D2FE)
    ),
    GraphicCategory(
        id = "music",
        title = "موزیک",
        subtitle = "رادیو آوا، موسیقی و ترانه",
        iconRes = R.drawable.ic_music_24,
        gradientColors = listOf(Color(0xFFEC4899), Color(0xFFBE185D)),
        accentColor = Color(0xFFFBCFE8)
    ),
    GraphicCategory(
        id = "kids",
        title = "کودک",
        subtitle = "پویا، نهال، هدهد، امید",
        iconRes = R.drawable.ic_child_24,
        gradientColors = listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
        accentColor = Color(0xFFFEF3C7)
    ),
    GraphicCategory(
        id = "sports",
        title = "ورزشی",
        subtitle = "شبکه ۳، ورزش، فوتبال",
        iconRes = R.drawable.ic_sports_24,
        gradientColors = listOf(Color(0xFF10B981), Color(0xFF047857)),
        accentColor = Color(0xFFA7F3D0)
    )
)

data class QuickChipItem(
    val id: String,
    val label: String
)

val QUICK_CHIPS = listOf(
    QuickChipItem("all", "همه شبکه‌ها"),
    QuickChipItem("national", "سراسری"),
    QuickChipItem("provincial", "استانی"),
    QuickChipItem("movies", "فیلم و سریال"),
    QuickChipItem("music", "موزیک"),
    QuickChipItem("kids", "کودک"),
    QuickChipItem("sports", "ورزشی"),
    QuickChipItem("favorites", "علاقه‌مندی‌ها")
)

@Composable
fun LiveTvScreen(
    viewModel: MainViewModel,
    playerManager: LiveTvPlayerManager,
    onEnterFullscreen: () -> Unit,
    onEnterPip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allChannels by viewModel.channels.collectAsState()
    val channels by viewModel.filteredChannels.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortAlphabetical by viewModel.sortAlphabetical.collectAsState()

    val currentChannel by playerManager.currentChannel.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val isBuffering by playerManager.isBuffering.collectAsState()
    val playerError by playerManager.playerError.collectAsState()

    var activeCategory by remember { mutableStateOf<String?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Determine whether we are in View 1 (Categories Grid) or View 2 (Channels List)
    val isInChannelView = activeCategory != null || searchQuery.isNotEmpty()

    // Hardware back button navigation: return to View 1 if in View 2
    BackHandler(enabled = isInChannelView) {
        if (searchQuery.isNotEmpty()) {
            viewModel.setSearchQuery("")
        } else {
            activeCategory = null
            viewModel.setCategory("all")
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgCream)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. COMPACT SEARCH BAR AT THE VERY TOP (44dp height, standard, clean clear button)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .height(44.dp),
                shape = RoundedCornerShape(14.dp),
                color = SurfaceWhite,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (searchQuery.isNotEmpty()) Terracotta else BorderLine
                ),
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = if (searchQuery.isNotEmpty()) Terracotta else SubduedGray,
                        modifier = Modifier.size(19.dp)
                    )

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { query ->
                            viewModel.setSearchQuery(query)
                            if (query.isNotEmpty() && activeCategory == null) {
                                activeCategory = "all"
                            }
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = InkDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "جستجوی نام شبکه (مثلاً: ورزش، سه، جم، پویا...)",
                                    fontSize = 12.sp,
                                    color = SubduedGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                viewModel.setSearchQuery("")
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "پاک‌کردن",
                                tint = SubduedGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 2. MAIN CONTENT: VIEW 1 (Categories) or VIEW 2 (Channels)
            if (!isInChannelView) {
                // ==========================================
                // VIEW 1: 6 COMPACT GRAPHIC CATEGORY CARDS
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp)
                ) {
                    // Header row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Terracotta.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "۶ بخش",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Terracotta,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "دسته‌بندی شبکه‌ها (انتخاب سریع)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = InkDark
                            )
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Terracotta)
                            )
                        }
                    }

                    // 2x3 Grid of Category Cards (Height ~88dp each, fitting single screen)
                    val chunked = MAIN_CATEGORIES.chunked(2)
                    chunked.forEach { pair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            pair.forEach { cat ->
                                val channelCount = allChannels.count { ch ->
                                    MainViewModel.matchesCategory(ch, cat.id)
                                }

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(86.dp)
                                        .clickable {
                                            activeCategory = cat.id
                                            viewModel.setCategory(cat.id)
                                        },
                                    shape = RoundedCornerShape(18.dp),
                                    color = Color.Transparent,
                                    shadowElevation = 3.dp
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    colors = cat.gradientColors
                                                )
                                            )
                                            .padding(10.dp)
                                    ) {
                                        // Header inside card: Count Badge & Icon
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Channel count pill
                                            Surface(
                                                shape = RoundedCornerShape(99.dp),
                                                color = Color.Black.copy(alpha = 0.25f)
                                            ) {
                                                Text(
                                                    text = "$channelCount کانال",
                                                    color = Color.White.copy(alpha = 0.95f),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            // Category Icon box
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color.White.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = cat.iconRes),
                                                    contentDescription = cat.title,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        // Bottom inside card: Title & Subtitle (RTL)
                                        Column(
                                            modifier = Modifier.align(Alignment.BottomEnd),
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            Text(
                                                text = cat.title,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = cat.subtitle,
                                                color = cat.accentColor,
                                                fontSize = 9.5.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Secondary Quick Action Bars (All Channels, Favorites, Add M3U)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // All Channels Button
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clickable {
                                    activeCategory = "all"
                                    viewModel.setCategory("all")
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceWhite,
                            border = BorderStroke(1.dp, BorderLine)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "همه شبکه‌ها (${allChannels.size})",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = InkDark
                                )
                            }
                        }

                        // Favorites Button
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clickable {
                                    activeCategory = "favorites"
                                    viewModel.setCategory("favorites")
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceWhite,
                            border = BorderStroke(1.dp, BorderLine)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = Color(0xFFF64668),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "علاقه‌مندی‌ها",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = InkDark
                                )
                            }
                        }

                        // Add M3U Button
                        Surface(
                            modifier = Modifier
                                .size(42.dp)
                                .clickable { showImportDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            color = ContainerRose,
                            border = BorderStroke(1.dp, Terracotta.copy(alpha = 0.2f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_add_24),
                                    contentDescription = "افزودن M3U",
                                    tint = Terracotta,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // ==========================================
                // VIEW 2: CHANNEL LIST FOR SELECTED CATEGORY
                // ==========================================
                val currentCategoryObj = MAIN_CATEGORIES.find { it.id == activeCategory }
                val bannerTitle = when {
                    searchQuery.isNotEmpty() -> "نتایج جستجو"
                    activeCategory == "favorites" -> "علاقه‌مندی‌های من"
                    activeCategory == "all" -> "همه شبکه‌ها"
                    currentCategoryObj != null -> "شبکه‌های ${currentCategoryObj.title}"
                    else -> "شبکه‌های انتخابی"
                }

                val bannerSubtitle = when {
                    searchQuery.isNotEmpty() -> "نمایش کانال‌های منطبق با «$searchQuery»"
                    activeCategory == "favorites" -> "شبکه‌های ذخیره شده و محبوب شما"
                    activeCategory == "all" -> "آرشیو کامل شبکه‌های تلویزیونی و رادیویی"
                    currentCategoryObj != null -> currentCategoryObj.subtitle
                    else -> "پخش زنده استریم"
                }

                val bannerGradients = currentCategoryObj?.gradientColors ?: listOf(
                    Color(0xFF141413),
                    Color(0xFF2C2C2B)
                )

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = if (currentChannel != null) 96.dp else 24.dp)
                ) {
                    // 1. Active Category Banner Header (with Back button to return to View 1)
                    item(key = "active_category_banner") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(20.dp),
                            shadowElevation = 4.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Brush.linearGradient(bannerGradients))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Right: Back button + Title & Subtitle (RTL)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Back button to return to 6-category view
                                        Surface(
                                            onClick = {
                                                activeCategory = null
                                                viewModel.setCategory("all")
                                                viewModel.setSearchQuery("")
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color.White.copy(alpha = 0.22f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_arrow_back_24),
                                                    contentDescription = "بازگشت به دسته‌ها",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "دسته‌ها",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = bannerTitle,
                                                color = Color.White,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Text(
                                                text = bannerSubtitle,
                                                color = Color.White.copy(alpha = 0.85f),
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // Left: Count Pill
                                    Surface(
                                        shape = RoundedCornerShape(99.dp),
                                        color = Color.Black.copy(alpha = 0.3f)
                                    ) {
                                        Text(
                                            text = "${channels.size} شبکه",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Quick Horizontal Category Chips (Quick switcher)
                    item(key = "quick_chips_row") {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(QUICK_CHIPS, key = { it.id }) { chip ->
                                val isSelected = (activeCategory == chip.id) || (activeCategory == null && chip.id == "all")
                                Surface(
                                    modifier = Modifier
                                        .height(32.dp)
                                        .clickable {
                                            activeCategory = chip.id
                                            viewModel.setCategory(chip.id)
                                        },
                                    shape = RoundedCornerShape(99.dp),
                                    color = if (isSelected) Terracotta else SurfaceWhite,
                                    border = BorderStroke(1.dp, if (isSelected) Terracotta else BorderLine)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = chip.label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else InkDark
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Inline Active Live Stream Player Box (shown when playing)
                    if (currentChannel != null) {
                        item(key = "inline_player_box") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(20.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF141413))
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(16f / 9f)
                                            .background(Color.Black)
                                    ) {
                                        AndroidView(
                                            factory = { ctx ->
                                                playerManager.createPlayerView(ctx, useController = true)
                                            },
                                            update = { pv ->
                                                playerManager.attachToView(pv)
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        if (isBuffering) {
                                            CircularProgressIndicator(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .align(Alignment.Center),
                                                color = Terracotta,
                                                strokeWidth = 3.dp
                                            )
                                        }

                                        if (playerError != null) {
                                            Surface(
                                                color = Color.Black.copy(alpha = 0.85f),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.align(Alignment.Center)
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.padding(14.dp)
                                                ) {
                                                    Text(
                                                        text = playerError ?: "خطا در پخش",
                                                        color = Color(0xFFFF5252),
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Button(
                                                        onClick = { currentChannel?.let { playerManager.play(it) } },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text("تلاش مجدد", color = Color.White, fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }

                                        // Quick Overlay Controls: Fullscreen, PiP, Close
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = onEnterFullscreen,
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.Black.copy(alpha = 0.6f))
                                                ) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.ic_fullscreen_24),
                                                        contentDescription = "تمام‌صفحه",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                IconButton(
                                                    onClick = onEnterPip,
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.Black.copy(alpha = 0.6f))
                                                ) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.ic_pip_24),
                                                        contentDescription = "تصویر در تصویر",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(17.dp)
                                                    )
                                                }

                                                IconButton(
                                                    onClick = {
                                                        playerManager.sendCurrentChannelToCar()
                                                    },
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.Black.copy(alpha = 0.6f))
                                                ) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.ic_send_to_car_24),
                                                        contentDescription = "ارسال به خودرو",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }

                                            IconButton(
                                                onClick = { playerManager.stop() },
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black.copy(alpha = 0.6f))
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Clear,
                                                    contentDescription = "بستن",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Subtitle Bar under player
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF1E1E1D))
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(99.dp),
                                                color = Color(0x3310B981)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF10B981))
                                                    )
                                                    Text(
                                                        text = "پخش زنده",
                                                        color = Color(0xFF10B981),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            currentChannel?.quality?.let { q ->
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color.White.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = q,
                                                        color = Color(0xFFFDE68A),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = currentChannel?.name ?: "",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. Channel Toolbar (Sort button + M3U button)
                    item(key = "channels_toolbar") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    onClick = { viewModel.toggleSortOrder() },
                                    shape = RoundedCornerShape(10.dp),
                                    color = SurfaceWhite,
                                    border = BorderStroke(1.dp, BorderLine)
                                ) {
                                    Text(
                                        text = if (sortAlphabetical) "مرتب: الفبا" else "مرتب: پیش‌فرض",
                                        fontSize = 10.5.sp,
                                        color = MutedSlate,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                IconButton(
                                    onClick = { showImportDialog = true },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ContainerRose)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_add_24),
                                        contentDescription = "افزودن M3U",
                                        tint = Terracotta,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Text(
                                text = "${channels.size} شبکه آماده پخش",
                                fontSize = 11.sp,
                                color = MutedSlate,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // 5. Channel Cards List
                    if (channels.isEmpty()) {
                        item(key = "empty_channel_state") {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                shape = RoundedCornerShape(18.dp),
                                color = SurfaceWhite,
                                border = BorderStroke(1.dp, BorderLine)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(28.dp)
                                ) {
                                    Text(
                                        text = "شبکه‌ای با این مشخصات پیدا نشد.",
                                        color = InkDark,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "دسته‌بندی را تغییر داده یا عبارت دیگری جستجو کنید.",
                                        color = MutedSlate,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            activeCategory = null
                                            viewModel.setCategory("all")
                                            viewModel.setSearchQuery("")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("بازگشت به دسته‌ها", color = Color.White, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        items(channels, key = { it.streamUrl }) { ch ->
                            val isChannelPlaying = currentChannel?.streamUrl == ch.streamUrl
                            ChannelItemCard(
                                channel = ch,
                                isPlaying = isChannelPlaying,
                                onPlay = { playerManager.play(ch) },
                                onToggleFavorite = { viewModel.toggleChannelFavorite(ch) },
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. FLOATING BOTTOM PLAYER CONTROLLER (when music / live stream playing)
        if (currentChannel != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .shadow(12.dp, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                color = SurfaceWhite,
                border = BorderStroke(1.dp, BorderLine)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Right: Channel Logo & Info (RTL)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ContainerRose),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentChannel?.name?.take(2) ?: "TV",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Terracotta
                            )
                        }

                        Column {
                            Text(
                                text = currentChannel?.name ?: "",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = InkDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentChannel?.category ?: "پخش زنده",
                                fontSize = 9.5.sp,
                                color = MutedSlate,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Center: Controls (Prev, Play/Pause, Next)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { playerManager.playPrevious(channels) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_skip_previous_24),
                                contentDescription = "شبکه قبلی",
                                tint = Color(0xFF758196),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Main Play / Pause Button (terracotta circular button)
                        IconButton(
                            onClick = { playerManager.togglePlayPause() },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Terracotta)
                                .shadow(4.dp, CircleShape)
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (isPlaying) R.drawable.ic_pause_24 else R.drawable.ic_play_24
                                ),
                                contentDescription = if (isPlaying) "توقف" else "پخش",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { playerManager.playNext(channels) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_skip_next_24),
                                contentDescription = "شبکه بعدی",
                                tint = Color(0xFF758196),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Left Actions: PiP (صفحه رو صفحه) & Fullscreen (تمام صفحه)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = onEnterPip,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_pip_24),
                                contentDescription = "تصویر در تصویر",
                                tint = Terracotta,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        IconButton(
                            onClick = onEnterFullscreen,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_fullscreen_24),
                                contentDescription = "تمام‌صفحه",
                                tint = Terracotta,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Import M3U Dialog
    if (showImportDialog) {
        ImportM3uDialog(
            onDismiss = { showImportDialog = false },
            onImport = { url ->
                viewModel.importM3u(url) { _, _ ->
                    showImportDialog = false
                }
            }
        )
    }
}

@Composable
fun ChannelItemCard(
    channel: LiveTvChannel,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPlay() },
        shape = RoundedCornerShape(16.dp),
        color = if (isPlaying) Color(0xFFFFF7F4) else SurfaceWhite,
        border = BorderStroke(
            width = if (isPlaying) 1.5.dp else 1.dp,
            color = if (isPlaying) Terracotta.copy(alpha = 0.5f) else BorderLine
        ),
        shadowElevation = if (isPlaying) 2.dp else 0.5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Actions: Favorite & Play Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Favorite Heart
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "علاقه‌مندی",
                        tint = if (channel.isFavorite) Color(0xFFD65372) else Color(0xFF9AA5B5),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Play Button
                IconButton(
                    onClick = onPlay,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (isPlaying) Terracotta else ContainerRose)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "پخش",
                        tint = if (isPlaying) Color.White else Terracotta,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Right: Channel Info & Station Logo (RTL)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = channel.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = InkDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        channel.quality?.let { q ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFF0F5FA)
                            ) {
                                Text(
                                    text = q,
                                    fontSize = 9.sp,
                                    color = Color(0xFF738096),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }

                        Text(
                            text = channel.category,
                            fontSize = 10.sp,
                            color = MutedSlate,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Station Logo Box
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFFFEDE6), Color(0xFFFFDED4))
                            )
                        )
                        .border(BorderStroke(1.dp, Color.White), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val label = when {
                        channel.name.contains("یک") || channel.name.contains("1") -> "شبکه ۱"
                        channel.name.contains("دو") || channel.name.contains("2") -> "شبکه ۲"
                        channel.name.contains("سه") || channel.name.contains("3") -> "شبکه ۳"
                        channel.name.contains("چهار") || channel.name.contains("4") -> "شبکه ۴"
                        channel.name.contains("پنج") || channel.name.contains("5") -> "شبکه ۵"
                        channel.name.contains("خبر") -> "خبر"
                        channel.name.contains("آی‌فیلم") || channel.name.contains("ifilm") -> "آی‌فیلم"
                        channel.name.contains("ورزش") -> "ورزش"
                        channel.name.contains("مستند") -> "مستند"
                        channel.name.contains("پویا") -> "پویا"
                        channel.name.contains("امید") -> "امید"
                        channel.name.contains("نسیم") -> "نسیم"
                        channel.name.contains("نمایش") -> "نمایش"
                        channel.name.contains("تماشا") -> "تماشا"
                        else -> channel.name.take(4)
                    }

                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Terracotta,
                        textAlign = TextAlign.Center,
                        lineHeight = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ImportM3uDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var urlText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("افزودن فهرست شبکه‌ها (M3U)", fontWeight = FontWeight.Bold, color = InkDark) },
        text = {
            Column {
                Text(
                    text = "آدرس URL استریم یا فایل M3U آنلاین را جهت اضافه شدن به لیست شبکه‌ها وارد کنید:",
                    fontSize = 12.5.sp,
                    color = MutedSlate
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    placeholder = { Text("https://example.com/playlist.m3u") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (urlText.isNotBlank()) {
                        onImport(urlText.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("افزودن", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف", color = MutedSlate)
            }
        }
    )
}
