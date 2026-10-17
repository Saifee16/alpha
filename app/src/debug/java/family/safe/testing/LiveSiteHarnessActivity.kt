package family.safe.testing

import android.app.Activity
import android.os.Bundle
import android.webkit.WebView
import family.safe.browser.UrlPolicy
import family.safe.browser.createProtectedWebView
import family.safe.browser.fetchRestricted
import family.safe.data.starterSources
import java.io.ByteArrayInputStream

/** Unexported live rendering probe; source choices are fixed and family settings are untouched. */
class LiveSiteHarnessActivity : Activity() {
    lateinit var browser: WebView
    @Volatile var blocked = false
    @Volatile var mainStatus = 0
    @Volatile var mainMime = ""
    @Volatile var mainBytes = 0
    @Volatile var requestCount = 0
    @Volatile var deniedCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val source = starterSources().single { it.id == intent.getStringExtra("sourceId") }.copy(enabled = true)
        val initial = requireNotNull(UrlPolicy.startUrl(source))
        browser = createProtectedWebView(this, source, { blocked = true }) { url, approved ->
            val response = fetchRestricted(url, approved)
            synchronized(this@LiveSiteHarnessActivity) {
                requestCount++
                if (response.statusCode !in 200..299) deniedCount++
            }
            if (url == initial) {
                val bytes = response.data.use { it.readBytes() }
                response.data = ByteArrayInputStream(bytes)
                mainMime = response.mimeType.orEmpty()
                mainBytes = bytes.size
                mainStatus = response.statusCode
            }
            response
        }
        setContentView(browser)
        browser.loadUrl(initial)
    }

    override fun onDestroy() { browser.stopLoading(); browser.destroy(); super.onDestroy() }
}
