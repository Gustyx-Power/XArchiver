package id.xms.xarchiver.ui.editor

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import id.xms.xarchiver.R
import id.xms.xarchiver.core.ShareUtils
import java.io.File

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HtmlPreviewScreen(
    fileName: String,
    htmlContent: String,
    file: File,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var isDesktopMode by remember { mutableStateOf(false) }
    var pageTitle by remember { mutableStateOf(fileName) }
    var progress by remember { mutableIntStateOf(0) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    // Intercept back button to navigate webview history first, or close preview
    BackHandler {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onClose()
        }
    }

    fun applyViewportMode(wv: WebView, desktop: Boolean) {
        wv.settings.apply {
            if (desktop) {
                userAgentString = DESKTOP_USER_AGENT
                useWideViewPort = true
                loadWithOverviewMode = true
            } else {
                userAgentString = null
                useWideViewPort = true
                loadWithOverviewMode = false
            }
        }
        if (desktop) {
            wv.setInitialScale(0)
        } else {
            wv.setInitialScale(100)
        }

        val preparedHtml = prepareHtmlContent(htmlContent, desktop)
        val baseUrl = file.parentFile?.let { "file://${it.absolutePath}/" }
        wv.loadDataWithBaseURL(baseUrl, preparedHtml, "text/html", "UTF-8", null)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = pageTitle.ifBlank { fileName },
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isDesktopMode) {
                                    stringResource(R.string.preview_desktop_mode)
                                } else {
                                    stringResource(R.string.preview_mobile_mode)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close Preview"
                            )
                        }
                    },
                    actions = {
                        if (canGoBack) {
                            IconButton(onClick = { webViewInstance?.goBack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Go Back"
                                )
                            }
                        }
                        if (canGoForward) {
                            IconButton(onClick = { webViewInstance?.goForward() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Go Forward"
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                webViewInstance?.let { applyViewportMode(it, isDesktopMode) }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.preview_refresh)
                            )
                        }
                        IconButton(
                            onClick = {
                                isDesktopMode = !isDesktopMode
                                webViewInstance?.let { wv ->
                                    applyViewportMode(wv, isDesktopMode)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isDesktopMode) Icons.Default.DesktopWindows else Icons.Default.Smartphone,
                                contentDescription = stringResource(
                                    if (isDesktopMode) R.string.preview_desktop_mode else R.string.preview_mobile_mode
                                ),
                                tint = if (isDesktopMode) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                        IconButton(
                            onClick = {
                                try {
                                    val uri = Uri.fromFile(file)
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "text/html")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Open in browser"))
                                } catch (_: Exception) {
                                    ShareUtils.openFile(context, file.absolutePath)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = stringResource(R.string.preview_open_browser)
                            )
                        }
                    }
                )

                // Loading progress bar
                if (progress in 1..99) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        // Standard web page default background is white
                        setBackgroundColor(android.graphics.Color.WHITE)

                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        @Suppress("DEPRECATION")
                        settings.allowFileAccessFromFileURLs = true
                        @Suppress("DEPRECATION")
                        settings.allowUniversalAccessFromFileURLs = true
                        settings.databaseEnabled = true
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.setSupportZoom(true)

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                progress = newProgress
                            }
                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                if (!title.isNullOrBlank()) {
                                    pageTitle = title
                                }
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true
                                if (view?.title?.isNotBlank() == true) {
                                    pageTitle = view.title ?: fileName
                                }

                                // Enforce viewport override in DOM for desktop mode
                                if (isDesktopMode) {
                                    view?.evaluateJavascript(
                                        """
                                        (function() {
                                            var meta = document.querySelector('meta[name="viewport"]');
                                            if (!meta) {
                                                meta = document.createElement('meta');
                                                meta.name = 'viewport';
                                                document.head.appendChild(meta);
                                            }
                                            meta.setAttribute('content', 'width=1280, initial-scale=0.3, user-scalable=yes');
                                        })();
                                        """.trimIndent(),
                                        null
                                    )
                                }
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val url = request?.url?.toString() ?: return false
                                if (url.startsWith("file://") || url.startsWith("http://") || url.startsWith("https://")) {
                                    return false
                                }
                                return try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                    true
                                } catch (_: Exception) {
                                    false
                                }
                            }
                        }

                        applyViewportMode(this, isDesktopMode)
                        webViewInstance = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            DisposableEffect(Unit) {
                onDispose {
                    webViewInstance?.stopLoading()
                    webViewInstance?.destroy()
                    webViewInstance = null
                }
            }
        }
    }
}

/**
 * Prepares HTML content based on viewport mode.
 * In desktop mode, overrides the viewport meta tag so responsive websites
 * trigger their full desktop layouts (width=1280) instead of mobile layouts.
 */
private fun prepareHtmlContent(rawHtml: String, desktopMode: Boolean): String {
    if (!desktopMode) return rawHtml

    val desktopViewport = """<meta name="viewport" content="width=1280, initial-scale=0.3, user-scalable=yes">"""
    val viewportRegex = Regex("""<meta\s+[^>]*name=["']viewport["'][^>]*>""", RegexOption.IGNORE_CASE)

    return if (viewportRegex.containsMatchIn(rawHtml)) {
        rawHtml.replace(viewportRegex, desktopViewport)
    } else if (rawHtml.contains("<head>", ignoreCase = true)) {
        rawHtml.replaceFirst("(?i)<head>".toRegex(), "<head>\n    $desktopViewport")
    } else {
        "$desktopViewport\n$rawHtml"
    }
}
