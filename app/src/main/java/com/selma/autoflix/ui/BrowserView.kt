package com.selma.autoflix.ui

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import com.selma.autoflix.data.repository.SavedSitesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BrowserView(
    context: Context,
    val savedSitesRepository: SavedSitesRepository,
    val scope: CoroutineScope,
    val isCarMode: Boolean = false
) : FrameLayout(context) {

    val webView: WebView = WebView(context)
    private val progressBar: ProgressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal)
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var customView: View? = null

    private val _currentUrl = MutableStateFlow("https://telewebion.com")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    private val _canGoBack = MutableStateFlow(false)
    val canGoBack: StateFlow<Boolean> = _canGoBack.asStateFlow()

    private val _canGoForward = MutableStateFlow(false)
    val canGoForward: StateFlow<Boolean> = _canGoForward.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var hasLoadedInitialUrl = false

    init {
        try {
            val webViewParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            addView(webView, webViewParams)

            val progressParams = LayoutParams(LayoutParams.MATCH_PARENT, 8)
            progressBar.max = 100
            progressBar.visibility = GONE
            addView(progressBar, progressParams)

            setupWebViewSettings()
            setupClients()
        } catch (e: Exception) {
            Log.e("AutoFlixBrowser", "Error initializing BrowserView", e)
        }
    }

    private fun setupWebViewSettings() {
        try {
            val settings = webView.settings
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.userAgentString = settings.userAgentString + " AutoFlix/0.6.0"

            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        } catch (e: Exception) {
            Log.e("AutoFlixBrowser", "Error configuring WebView settings", e)
        }
    }

    private fun setupClients() {
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    return false
                }
                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                _isLoading.value = true
                progressBar.visibility = VISIBLE
                url?.let { _currentUrl.value = it }
                updateNavState()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                _isLoading.value = false
                progressBar.visibility = GONE
                url?.let { _currentUrl.value = it }
                updateNavState()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                if (newProgress >= 100) {
                    progressBar.visibility = GONE
                }
            }

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (customView != null) {
                    callback?.onCustomViewHidden()
                    return
                }
                customView = view
                customViewCallback = callback
                webView.visibility = GONE
                addView(view, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            }

            override fun onHideCustomView() {
                if (customView == null) return
                webView.visibility = VISIBLE
                removeView(customView)
                customView = null
                customViewCallback?.onCustomViewHidden()
                customViewCallback = null
            }
        }
    }

    private fun updateNavState() {
        try {
            _canGoBack.value = webView.canGoBack()
            _canGoForward.value = webView.canGoForward()
        } catch (e: Exception) {
            Log.e("AutoFlixBrowser", "Error updating nav state", e)
        }
    }

    fun loadUrl(url: String) {
        var target = url.trim()
        if (!target.startsWith("http://") && !target.startsWith("https://")) {
            target = "https://$target"
        }
        hasLoadedInitialUrl = true
        _currentUrl.value = target
        webView.loadUrl(target)
    }

    fun ensureLoaded(defaultUrl: String = "https://telewebion.com") {
        if (!hasLoadedInitialUrl) {
            loadUrl(defaultUrl)
        }
    }

    fun handleBack(): Boolean {
        if (customView != null) {
            webView.webChromeClient?.onHideCustomView()
            return true
        }
        if (webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return false
    }

    fun reload() {
        webView.reload()
    }

    fun pause() {
        try {
            webView.onPause()
            webView.pauseTimers()
        } catch (e: Exception) {
            Log.e("AutoFlixBrowser", "Error in pause", e)
        }
    }

    fun resume() {
        try {
            webView.onResume()
            webView.resumeTimers()
        } catch (e: Exception) {
            Log.e("AutoFlixBrowser", "Error in resume", e)
        }
    }

    fun destroy() {
        try {
            customView = null
            customViewCallback = null
            webView.stopLoading()
            webView.destroy()
            removeAllViews()
        } catch (e: Exception) {
            Log.e("AutoFlixBrowser", "Error destroying WebView", e)
        }
    }
}
