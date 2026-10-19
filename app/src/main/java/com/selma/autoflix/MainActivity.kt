package com.selma.autoflix

import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.selma.autoflix.player.LiveTvPlayerManager
import com.selma.autoflix.ui.BrowserScreen
import com.selma.autoflix.ui.BrowserView
import com.selma.autoflix.ui.FullscreenPlayerView
import com.selma.autoflix.ui.LiveTvScreen
import com.selma.autoflix.ui.SavedSitesScreen
import com.selma.autoflix.ui.components.BrandLogo
import com.selma.autoflix.ui.theme.*
import com.selma.autoflix.ui.viewmodel.MainTab
import com.selma.autoflix.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private lateinit var liveTvPlayerManager: LiveTvPlayerManager
    private lateinit var browserView: BrowserView

    val isInPipMode = mutableStateOf(false)
    val isFullscreen = mutableStateOf(false)

    private val viewModel: MainViewModel by viewModels {
        val app = application as AutoFlixApplication
        MainViewModel.Factory(
            app.liveTvRepository,
            app.savedSitesRepository
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as AutoFlixApplication
        liveTvPlayerManager = LiveTvPlayerManager(this, isCarMode = false)
        browserView = BrowserView(
            context = this,
            savedSitesRepository = app.savedSitesRepository,
            scope = lifecycleScope,
            isCarMode = false
        )

        setContent {
            AutoFlixTheme {
                val inPip by isInPipMode
                val inFullscreen by isFullscreen
                val channels by viewModel.filteredChannels.collectAsState()

                if (inPip) {
                    // Minimalist edge-to-edge video window in PiP mode
                    AndroidView(
                        factory = { ctx ->
                            liveTvPlayerManager.createPlayerView(ctx, useController = false)
                        },
                        update = { pv ->
                            liveTvPlayerManager.attachToView(pv)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (inFullscreen) {
                    // Fullscreen immersive player
                    FullscreenPlayerView(
                        playerManager = liveTvPlayerManager,
                        channels = channels,
                        onExitFullscreen = { setFullscreen(false) },
                        onEnterPip = { enterPipMode() }
                    )
                } else {
                    // Standard Mobile App UI
                    MainAppContainer(
                        viewModel = viewModel,
                        playerManager = liveTvPlayerManager,
                        browserView = browserView,
                        onEnterFullscreen = { setFullscreen(true) },
                        onEnterPip = { enterPipMode() },
                        onExit = { finish() }
                    )
                }
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode.value = isInPictureInPictureMode
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && liveTvPlayerManager.isPlaying.value) {
            enterPipMode()
        }
    }

    fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (e: Exception) {
                Log.e("MainActivity", "Error entering PiP", e)
            }
        }
    }

    fun setFullscreen(enable: Boolean) {
        isFullscreen.value = enable
        val window = window ?: return
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (enable) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onResume() {
        super.onResume()
        if (::browserView.isInitialized) {
            browserView.resume()
        }
    }

    override fun onPause() {
        if (::browserView.isInitialized) {
            browserView.pause()
        }
        super.onPause()
    }

    override fun onDestroy() {
        if (::liveTvPlayerManager.isInitialized) {
            liveTvPlayerManager.release()
        }
        if (::browserView.isInitialized) {
            browserView.destroy()
        }
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(
    viewModel: MainViewModel,
    playerManager: LiveTvPlayerManager,
    browserView: BrowserView,
    onEnterFullscreen: () -> Unit,
    onEnterPip: () -> Unit,
    onExit: () -> Unit
) {
    val currentTab by viewModel.currentTab.collectAsState()

    BackHandler {
        if (currentTab == MainTab.BROWSER) {
            if (browserView.handleBack()) {
                return@BackHandler
            } else {
                viewModel.selectTab(MainTab.LIVE_TV)
                return@BackHandler
            }
        } else if (currentTab == MainTab.SAVED_SITES) {
            viewModel.selectTab(MainTab.LIVE_TV)
            return@BackHandler
        } else {
            onExit()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 4.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Brand Logo + Brand title & Subtitle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            BrandLogo(size = 38.dp)

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        text = "AutoFlix",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = InkDark,
                                        fontSize = 19.sp,
                                        letterSpacing = (-0.4).sp
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(99.dp),
                                        color = Terracotta.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, Terracotta.copy(alpha = 0.25f))
                                    ) {
                                        Text(
                                            text = "LIVE STREAM",
                                            color = Terracotta,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "پخش زنده تلویزیون و ماهواره",
                                    fontSize = 10.sp,
                                    color = SubduedGray,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceWhite
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceWhite,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == MainTab.LIVE_TV,
                    onClick = { viewModel.selectTab(MainTab.LIVE_TV) },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_live_tv_24),
                            contentDescription = "تلویزیون زنده"
                        )
                    },
                    label = { Text("تلویزیون", fontSize = 11.5.sp, fontWeight = if (currentTab == MainTab.LIVE_TV) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Terracotta,
                        selectedTextColor = Terracotta,
                        indicatorColor = ContainerRose,
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )

                NavigationBarItem(
                    selected = currentTab == MainTab.BROWSER,
                    onClick = { viewModel.selectTab(MainTab.BROWSER) },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_website_48),
                            contentDescription = "مرورگر وب"
                        )
                    },
                    label = { Text("مرورگر", fontSize = 11.5.sp, fontWeight = if (currentTab == MainTab.BROWSER) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Terracotta,
                        selectedTextColor = Terracotta,
                        indicatorColor = ContainerRose,
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )

                NavigationBarItem(
                    selected = currentTab == MainTab.SAVED_SITES,
                    onClick = { viewModel.selectTab(MainTab.SAVED_SITES) },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_bookmark_add_24),
                            contentDescription = "سایت‌های ذخیره"
                        )
                    },
                    label = { Text("سایت‌ها", fontSize = 11.5.sp, fontWeight = if (currentTab == MainTab.SAVED_SITES) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Terracotta,
                        selectedTextColor = Terracotta,
                        indicatorColor = ContainerRose,
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentTab) {
                MainTab.LIVE_TV -> {
                    LiveTvScreen(
                        viewModel = viewModel,
                        playerManager = playerManager,
                        onEnterFullscreen = onEnterFullscreen,
                        onEnterPip = onEnterPip
                    )
                }
                MainTab.BROWSER -> {
                    BrowserScreen(
                        browserView = browserView,
                        viewModel = viewModel
                    )
                }
                MainTab.SAVED_SITES -> {
                    SavedSitesScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
