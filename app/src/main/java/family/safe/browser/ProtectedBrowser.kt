package family.safe.browser

import android.content.Context
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Message
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/** Read-only browsing: no scripts, forms, accounts, third-party resources, or media. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ProtectedBrowser(source: BrowserSource, onHome: () -> Unit) {
    key(source) {
        var browser by remember { mutableStateOf<WebView?>(null) }
        var blocked by remember { mutableStateOf(false) }
        val initial = remember(source) { UrlPolicy.startUrl(source) }
        BackHandler { onHome() }
        DisposableEffect(Unit) {
            onDispose {
                browser?.apply { stopLoading(); clearHistory(); clearCache(true); destroy() }
                browser = null
            }
        }
        Column(Modifier.fillMaxSize()) {
            Button(onClick = onHome, modifier = Modifier.padding(8.dp)) { Text("Back to FamilySafe") }
            Text(source.name, Modifier.padding(horizontal = 16.dp))
            Text("Read-only view. Some interactive sites will not work.", Modifier.padding(16.dp))
            if (blocked || initial == null) {
                Text("This page is not available in FamilySafe. Return home to choose an approved source.", Modifier.padding(16.dp))
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    factory = { context ->
                        createProtectedWebView(context, source, { blocked = true }).also {
                            browser = it
                            it.loadUrl(initial)
                        }
                    },
                )
            }
        }
    }
}

/** Shared production WebView; injected fetch replaces only network IO for device tests. */
@SuppressLint("SetJavaScriptEnabled")
internal fun createProtectedWebView(
    context: Context,
    source: BrowserSource,
    onBlocked: () -> Unit,
    fetch: (String, BrowserSource) -> WebResourceResponse = { url, approved -> fetchRestricted(url, approved) }
): WebView = WebView(context).apply {
    visibility = android.view.View.INVISIBLE
    settings.apply {
        javaScriptEnabled = false
        domStorageEnabled = false
        databaseEnabled = false
        allowFileAccess = false
        allowContentAccess = false
        @Suppress("DEPRECATION")
        allowFileAccessFromFileURLs = false
        @Suppress("DEPRECATION")
        allowUniversalAccessFromFileURLs = false
        javaScriptCanOpenWindowsAutomatically = false
        setSupportMultipleWindows(true)
        setGeolocationEnabled(false)
        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        safeBrowsingEnabled = true
        mediaPlaybackRequiresUserGesture = true
        cacheMode = WebSettings.LOAD_NO_CACHE
        builtInZoomControls = true
        displayZoomControls = false
    }
    CookieManager.getInstance().setAcceptCookie(false)
    CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
    isLongClickable = false
    setOnLongClickListener { true }
    setDownloadListener { _, _, _, _, _ -> stopLoading(); onBlocked() }
    webChromeClient = object : WebChromeClient() {
        override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean = false
        override fun onPermissionRequest(request: PermissionRequest) { request.deny() }
        override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback) { callback.invoke(origin, false, false) }
        override fun onShowFileChooser(webView: WebView?, callback: ValueCallback<Array<android.net.Uri>>?, params: FileChooserParams?): Boolean { callback?.onReceiveValue(null); return true }
    }
    webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val denied = request.method != "GET" || !UrlPolicy.allows(request.url.toString(), source)
            if (denied && request.isForMainFrame) { view.stopLoading(); view.visibility = android.view.View.INVISIBLE; onBlocked() }
            return denied
        }

        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse =
            if (request.method == "GET" && UrlPolicy.allows(request.url.toString(), source)) fetch(request.url.toString(), source) else deniedResponse()

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            view.visibility = android.view.View.INVISIBLE
            if (url == null || !UrlPolicy.allows(url, source)) { view.stopLoading(); onBlocked() }
        }

        override fun onPageCommitVisible(view: WebView, url: String?) {
            if (url != null && UrlPolicy.allows(url, source)) view.visibility = android.view.View.VISIBLE
            else { view.stopLoading(); onBlocked() }
        }

        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) { handler.cancel(); view.stopLoading(); onBlocked() }
    }
}

private fun deniedResponse() = WebResourceResponse("text/plain", "UTF-8", 403, "Blocked", mapOf("Cache-Control" to "no-store"), ByteArrayInputStream("Page unavailable in FamilySafe.".toByteArray()))

// WebView omits redirect callbacks for subresources; fetch every hop ourselves instead.
internal fun fetchRestricted(
    initial: String,
    source: BrowserSource,
    openConnection: (String) -> HttpURLConnection = { URL(it).openConnection() as HttpURLConnection }
): WebResourceResponse {
    var target = initial
    val deadline = System.nanoTime() + 20_000_000_000L
    repeat(5) {
        if (!UrlPolicy.allows(target, source) || System.nanoTime() >= deadline) return deniedResponse()
        try {
            val connection = openConnection(target)
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 8_000
                connection.readTimeout = 8_000
                connection.useCaches = false
                connection.setRequestProperty("User-Agent", "FamilySafe/1.0 (read-only Android viewer)")
                connection.setRequestProperty("Accept-Encoding", "identity")
                val code = connection.responseCode
                if (code in 300..399) {
                    target = UrlPolicy.redirectUrl(target, connection.getHeaderField("Location") ?: return deniedResponse(), source) ?: return deniedResponse()
                } else {
                    if (code !in 200..299 || connection.getHeaderField("Content-Disposition")?.contains("attachment", ignoreCase = true) == true) return deniedResponse()
                    val type = connection.contentType.orEmpty().substringBefore(';').trim().lowercase(java.util.Locale.ROOT)
                    if (type !in setOf("text/html", "text/plain", "text/css", "image/png", "image/jpeg", "image/gif", "image/webp", "image/svg+xml", "image/x-icon", "font/woff", "font/woff2")) return deniedResponse()
                    if (connection.contentLengthLong > 2_097_152L) return deniedResponse()
                    val bytes = ByteArrayOutputStream()
                    connection.inputStream.use { input ->
                        val buffer = ByteArray(8192)
                        var count: Int
                        while (input.read(buffer).also { count = it } != -1) {
                            if (bytes.size() + count > 2_097_152 || System.nanoTime() >= deadline) return deniedResponse()
                            bytes.write(buffer, 0, count)
                        }
                    }
                    val charset = Regex("charset=([^;\\s]+)", RegexOption.IGNORE_CASE).find(connection.contentType.orEmpty())?.groupValues?.get(1)?.trim('"', '\'') ?: "UTF-8"
                    return WebResourceResponse(type, charset, code, "OK", mapOf(
                        "Cache-Control" to "no-store",
                        "X-Content-Type-Options" to "nosniff",
                        "Content-Security-Policy" to "default-src 'self'; script-src 'none'; frame-src 'none'; child-src 'none'; object-src 'none'; connect-src 'none'; form-action 'none'; base-uri 'none'; media-src 'none'; style-src 'self' 'unsafe-inline'",
                    ), ByteArrayInputStream(bytes.toByteArray()))
                }
            } finally { connection.disconnect() }
        } catch (_: Exception) { return deniedResponse() }
    }
    return deniedResponse()
}
