package com.selma.autoflix.shared

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.google.android.apps.auto.sdk.CarActivity
import com.selma.autoflix.AutoFlixApplication
import com.selma.autoflix.data.model.LiveTvChannel
import com.selma.autoflix.player.LiveTvPlayerManager
import com.selma.autoflix.ui.BrowserView
import com.selma.autoflix.ui.CarDashboardScreen
import com.selma.autoflix.ui.CarSection
import com.selma.autoflix.ui.theme.AutoFlixTheme
import com.selma.autoflix.ui.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Custom LifecycleOwner + ViewModelStoreOwner + SavedStateRegistryOwner
 * so Jetpack Compose and AndroidView (TextureView PlayerView & WebView) can run
 * inside Android Auto's CarActivity projection window.
 */
private class CarComposeStateOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    fun onCreate(savedState: Bundle?) {
        savedStateController.performAttach()
        savedStateController.performRestore(savedState)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun handleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }

    fun onSaveInstanceState(outBundle: Bundle) {
        savedStateController.performSave(outBundle)
    }

    fun clear() {
        store.clear()
    }
}

class AutoFlixCarActivity : CarActivity() {

    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val composeStateOwner = CarComposeStateOwner()

    private lateinit var contentContainer: FrameLayout
    private lateinit var liveTvPlayerManager: LiveTvPlayerManager
    private lateinit var viewModel: MainViewModel
    private var browserView: BrowserView? = null

    private var activeSection by mutableStateOf(CarSection.LIVE_TV)
    private var handoffRegistered = false

    private val handoffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            when (action) {
                "com.selma.autoflix.action.OPEN_URL_ON_CAR" -> {
                    val url = intent.getStringExtra("url")
                    if (!url.isNullOrBlank()) {
                        liveTvPlayerManager.pause()
                        activeSection = CarSection.BROWSER
                        browserView?.loadUrl(url)
                    }
                }
                "com.selma.autoflix.action.PLAY_LIVE_TV_ON_CAR" -> {
                    val url = intent.getStringExtra("url")
                    if (!url.isNullOrBlank()) {
                        val name = intent.getStringExtra("name") ?: "پخش زنده"
                        val category = intent.getStringExtra("category") ?: "شبکه‌های زنده"
                        val quality = intent.getStringExtra("quality") ?: "HD"
                        activeSection = CarSection.LIVE_TV
                        liveTvPlayerManager.play(
                            LiveTvChannel(
                                id = url,
                                streamUrl = url,
                                name = name,
                                logoUrl = "",
                                category = category,
                                quality = quality
                            )
                        )
                    } else {
                        checkPendingHandoff()
                    }
                }
            }
        }
    }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)

        // Hide default Android Auto system header/bars so our full Car OS HUD fills the screen
        try {
            carUiController?.statusBarController?.hideAppHeader()
            carUiController?.statusBarController?.hideTitle()
            carUiController?.menuController?.hideMenuButton()
        } catch (e: Throwable) {
            Log.w("AutoFlixCarActivity", "Could not hide CarUiController bars", e)
        }

        composeStateOwner.onCreate(bundle)

        val app = AutoFlixApplication.instance
        liveTvPlayerManager = LiveTvPlayerManager(this, isCarMode = true)
        viewModel = MainViewModel(
            liveTvRepository = app.liveTvRepository,
            savedSitesRepository = app.savedSitesRepository
        )
        browserView = BrowserView(
            context = this,
            savedSitesRepository = app.savedSitesRepository,
            scope = activityScope,
            isCarMode = true
        )

        contentContainer = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(11, 15, 23))
        }
        setContentView(contentContainer)

        // Attach ViewTree owners to contentContainer and rootView before adding ComposeView
        attachViewTreeOwners()

        val composeView = ComposeView(this).apply {
            attachViewTreeOwnersTo(this)
            setContent {
                AutoFlixTheme {
                    CarDashboardScreen(
                        viewModel = viewModel,
                        playerManager = liveTvPlayerManager,
                        browserView = browserView,
                        activeSection = activeSection,
                        onSectionChange = { newSection ->
                            activeSection = newSection
                        }
                    )
                }
            }
        }

        contentContainer.addView(
            composeView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        checkPendingHandoff()
    }

    private fun attachViewTreeOwners() {
        attachViewTreeOwnersTo(contentContainer)
        contentContainer.rootView?.let { attachViewTreeOwnersTo(it) }
    }

    private fun attachViewTreeOwnersTo(view: android.view.View) {
        view.setViewTreeLifecycleOwner(composeStateOwner)
        view.setViewTreeViewModelStoreOwner(composeStateOwner)
        view.setViewTreeSavedStateRegistryOwner(composeStateOwner)
    }

    private fun checkPendingHandoff() {
        try {
            val prefs = applicationContext.getSharedPreferences("car_handoff", Context.MODE_PRIVATE)
            val streamUrl = prefs.getString("live_stream_url", null)
            if (!streamUrl.isNullOrBlank()) {
                val name = prefs.getString("live_name", "پخش زنده") ?: "پخش زنده"
                val logoUrl = prefs.getString("live_logo_url", "") ?: ""
                val group = prefs.getString("live_group", "شبکه‌های زنده") ?: "شبکه‌های زنده"
                val quality = prefs.getString("live_quality", "HD") ?: "HD"
                val fav = prefs.getBoolean("live_favorite", false)

                prefs.edit().remove("live_stream_url").apply()

                activeSection = CarSection.LIVE_TV
                liveTvPlayerManager.play(
                    LiveTvChannel(
                        id = streamUrl,
                        streamUrl = streamUrl,
                        name = name,
                        logoUrl = logoUrl,
                        category = group,
                        quality = quality,
                        isFavorite = fav
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("AutoFlixCarActivity", "Error checking car handoff", e)
        }
    }

    override fun onStart() {
        super.onStart()
        composeStateOwner.handleEvent(Lifecycle.Event.ON_START)
        if (!handoffRegistered) {
            val filter = IntentFilter().apply {
                addAction("com.selma.autoflix.action.OPEN_URL_ON_CAR")
                addAction("com.selma.autoflix.action.PLAY_LIVE_TV_ON_CAR")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(handoffReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(handoffReceiver, filter)
            }
            handoffRegistered = true
        }
    }

    override fun onResume() {
        super.onResume()
        composeStateOwner.handleEvent(Lifecycle.Event.ON_RESUME)
        browserView?.resume()
        if (activeSection == CarSection.LIVE_TV && liveTvPlayerManager.currentChannel.value != null) {
            liveTvPlayerManager.resume()
        }
    }

    override fun onPause() {
        composeStateOwner.handleEvent(Lifecycle.Event.ON_PAUSE)
        browserView?.pause()
        liveTvPlayerManager.pause()
        super.onPause()
    }

    override fun onStop() {
        composeStateOwner.handleEvent(Lifecycle.Event.ON_STOP)
        if (handoffRegistered) {
            try {
                unregisterReceiver(handoffReceiver)
            } catch (_: Exception) {
            }
            handoffRegistered = false
        }
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        composeStateOwner.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        composeStateOwner.handleEvent(Lifecycle.Event.ON_DESTROY)
        composeStateOwner.clear()
        activityScope.cancel()
        liveTvPlayerManager.release()
        browserView?.destroy()
        browserView = null
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (activeSection == CarSection.BROWSER && browserView?.handleBack() == true) {
            return
        }
        if (activeSection != CarSection.LIVE_TV) {
            activeSection = CarSection.LIVE_TV
            return
        }
        super.onBackPressed()
    }
}
