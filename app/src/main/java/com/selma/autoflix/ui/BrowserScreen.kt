package com.selma.autoflix.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.selma.autoflix.R
import com.selma.autoflix.ui.theme.*
import com.selma.autoflix.ui.viewmodel.MainViewModel

@Composable
fun BrowserScreen(
    browserView: BrowserView,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val currentUrl by browserView.currentUrl.collectAsState()
    val canGoBack by browserView.canGoBack.collectAsState()
    val canGoForward by browserView.canGoForward.collectAsState()
    val isLoading by browserView.isLoading.collectAsState()

    val targetUrlFromVm by viewModel.browserUrl.collectAsState()

    var urlInput by remember { mutableStateOf(currentUrl) }

    LaunchedEffect(currentUrl) {
        urlInput = currentUrl
    }

    LaunchedEffect(targetUrlFromVm) {
        if (targetUrlFromVm.isNotBlank()) {
            browserView.loadUrl(targetUrlFromVm)
        } else {
            browserView.ensureLoaded()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Navigation Toolbar
        Surface(
            color = SurfaceLight,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // History back/forward buttons
                    IconButton(
                        onClick = { browserView.handleBack() },
                        enabled = canGoBack
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "عقب",
                            tint = if (canGoBack) TealPrimary else Color(0xFFCBD5E1)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (browserView.webView.canGoForward()) {
                                browserView.webView.goForward()
                            }
                        },
                        enabled = canGoForward
                    ) {
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = "جلو",
                            tint = if (canGoForward) TealPrimary else Color(0xFFCBD5E1)
                        )
                    }

                    // Address input
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                focusManager.clearFocus()
                                browserView.loadUrl(urlInput)
                            }
                        ),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = OutlineLight,
                            focusedContainerColor = SurfaceVariantLight,
                            unfocusedContainerColor = SurfaceVariantLight
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
                    )

                    // Reload
                    IconButton(onClick = { browserView.reload() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "تازه‌سازی",
                            tint = TealPrimary
                        )
                    }

                    // Save / Bookmark current page
                    IconButton(
                        onClick = {
                            val title = browserView.webView.title ?: urlInput
                            viewModel.addSite(title, urlInput)
                            Toast.makeText(context, "سایت در علاقه‌مندی‌ها ذخیره شد", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_bookmark_add_24),
                            contentDescription = "نشان‌گذاری",
                            tint = CyanSecondary
                        )
                    }

                    // Send to car head unit
                    IconButton(
                        onClick = {
                            val sendIntent = Intent("com.selma.autoflix.action.OPEN_URL_ON_CAR").apply {
                                putExtra("url", currentUrl)
                            }
                            context.sendBroadcast(sendIntent)
                            Toast.makeText(context, "در حال ارسال به مانیتور خودرو...", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_send_to_car_24),
                            contentDescription = "ارسال به خودرو",
                            tint = TealPrimary
                        )
                    }
                }

                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = TealPrimary,
                        trackColor = TealContainer
                    )
                }
            }
        }

        // WebView Container
        AndroidView(
            factory = {
                (browserView.parent as? android.view.ViewGroup)?.removeView(browserView)
                browserView
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}
