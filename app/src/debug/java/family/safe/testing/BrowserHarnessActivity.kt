package family.safe.testing

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.webkit.WebView
import family.safe.browser.BrowserSource
import family.safe.browser.createProtectedWebView
import family.safe.browser.fetchRestricted
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections

/** Non-exported debug-only fixture: exercises the real browser without reaching the internet. */
class BrowserHarnessActivity : Activity() {
    lateinit var browser: WebView
    @Volatile var blocked = false
    var attemptedExternalLaunches = 0
    val fetchedUrls: MutableList<String> = Collections.synchronizedList(mutableListOf())
    val source = BrowserSource("fixture", "Local browser fixture", "learning.example", setOf("science"), listOf("/learn"), enabled = true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val href = intent.getStringExtra("href") ?: "https://learning.example/learn/next"
        val target = if (intent.getBooleanExtra("popup", false)) "target='_blank'" else ""
        val html = """<html><head><meta name="viewport" content="width=device-width, initial-scale=1"></head><body style="margin:0"><a $target href="$href" style="position:absolute;left:0;top:0;width:300px;height:120px;background:#eee">Open fixture link</a></body></html>"""
        browser = createProtectedWebView(this, source, { blocked = true }) { url, approved ->
            fetchRestricted(url, approved) {
                fetchedUrls.add(it)
                FixtureConnection(it, body = html.toByteArray())
            }
        }
        setContentView(browser)
        browser.loadUrl("https://learning.example/learn")
    }

    fun tapFixtureLink() {
        val now = android.os.SystemClock.uptimeMillis()
        val point = 50 * resources.displayMetrics.density
        MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, point, point, 0).also { browser.dispatchTouchEvent(it); it.recycle() }
        MotionEvent.obtain(now, now + 80, MotionEvent.ACTION_UP, point, point, 0).also { browser.dispatchTouchEvent(it); it.recycle() }
    }

    override fun startActivity(intent: Intent?, options: Bundle?) { attemptedExternalLaunches++ }
    override fun onDestroy() { browser.stopLoading(); browser.destroy(); super.onDestroy() }
}

class FixtureConnection(
    url: String,
    private val code: Int = 200,
    private val type: String = "text/html; charset=UTF-8",
    private val body: ByteArray = "Fixture page".toByteArray(),
    private val headers: Map<String, String> = emptyMap(),
    private val advertisedLength: Long = body.size.toLong()
) : HttpURLConnection(URL(url)) {
    var disconnected = false
    override fun connect() {}
    override fun disconnect() { disconnected = true }
    override fun usingProxy() = false
    override fun getResponseCode() = code
    override fun getContentType() = type
    override fun getContentLengthLong() = advertisedLength
    override fun getHeaderField(name: String?) = headers[name]
    override fun getInputStream() = ByteArrayInputStream(body)
}
