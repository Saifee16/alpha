package family.safe

import android.content.Intent
import android.net.Uri
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebChromeClient
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import family.safe.browser.BrowserSource
import family.safe.browser.fetchRestricted
import family.safe.testing.BrowserHarnessActivity
import family.safe.testing.FixtureConnection
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class BrowserDeviceTest {
    @Test fun approvedRenderedLinkStaysInsideRealWebView() {
        launch().use { scenario ->
            ready(scenario)
            scenario.onActivity { it.tapFixtureLink() }
            await(scenario) { it.browser.url == "https://learning.example/learn/next" && it.browser.visibility == View.VISIBLE }
            scenario.onActivity { assertFalse(it.blocked); assertEquals(0, it.attemptedExternalLaunches) }
        }
    }

    @Test fun renderedExternalAndSocialLinksAreBlockedBeforeFetch() {
        listOf("https://outside.example/learn", "https://instagram.com/", "https://learning.example.evil.test/learn", "intent://escape/#Intent;scheme=https;end").forEach { destination ->
            launch(destination).use { scenario ->
                ready(scenario)
                scenario.onActivity { it.tapFixtureLink() }
                await(scenario) { it.blocked }
                scenario.onActivity {
                    assertEquals(0, it.attemptedExternalLaunches)
                    assertFalse(it.fetchedUrls.contains(destination))
                    assertEquals(View.INVISIBLE, it.browser.visibility)
                }
            }
        }
    }

    @Test fun popupAndPrivilegedWebCapabilitiesStayDisabled() {
        launch("https://outside.example/learn", popup = true).use { scenario ->
            ready(scenario)
            scenario.onActivity {
                val web = it.browser
                assertFalse(web.settings.javaScriptEnabled)
                assertFalse(web.settings.domStorageEnabled)
                assertFalse(web.settings.allowFileAccess)
                assertFalse(web.settings.allowContentAccess)
                assertFalse(CookieManager.getInstance().acceptCookie())
                assertEquals(WebSettings.MIXED_CONTENT_NEVER_ALLOW, web.settings.mixedContentMode)
                assertFalse(web.webChromeClient!!.onCreateWindow(web, false, true, android.os.Message.obtain()))
                var chosen: Array<Uri>? = arrayOf(Uri.parse("file:///should-not-survive"))
                val params = object : WebChromeClient.FileChooserParams() {
                    override fun getMode() = MODE_OPEN
                    override fun getAcceptTypes() = arrayOf("*/*")
                    override fun isCaptureEnabled() = false
                    override fun getTitle(): CharSequence? = null
                    override fun getFilenameHint(): String? = null
                    override fun createIntent() = Intent(Intent.ACTION_GET_CONTENT)
                }
                assertTrue(web.webChromeClient!!.onShowFileChooser(web, { value -> chosen = value }, params))
                assertNull(chosen)
                it.tapFixtureLink()
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { assertEquals(0, it.attemptedExternalLaunches); assertFalse(it.fetchedUrls.contains("https://outside.example/learn")) }
        }
    }

    @Test fun realClientRejectsMethodsSchemesAndSubresourcesBeforeTransport() {
        launch().use { scenario ->
            ready(scenario)
            scenario.onActivity {
                val web = it.browser
                listOf("javascript:alert(1)", "file:///etc/passwd", "content://contacts/people", "mailto:a@example.com", "https://learning.example@evil.example/learn", "https://learning.example/learn/%2e%2e/private").forEach { url ->
                    assertTrue(url, web.webViewClient.shouldOverrideUrlLoading(web, Request(url)))
                    assertEquals(403, web.webViewClient.shouldInterceptRequest(web, Request(url))!!.statusCode)
                }
                assertEquals(403, web.webViewClient.shouldInterceptRequest(web, Request("https://learning.example/learn", "POST"))!!.statusCode)
                assertEquals(403, web.webViewClient.shouldInterceptRequest(web, Request("https://outside.example/image.png", main = false))!!.statusCode)
                assertEquals(0, it.attemptedExternalLaunches)
            }
        }
    }

    @Test fun restrictedTransportFollowsOnlyApprovedRedirectsAndSetsCsp() {
        val calls = mutableListOf<String>()
        val response = fetchRestricted(START, SOURCE) { url ->
            calls += url
            if (calls.size == 1) FixtureConnection(url, code = 302, headers = mapOf("Location" to "/learn/next"))
            else FixtureConnection(url)
        }
        assertEquals(listOf(START, "$START/next"), calls)
        assertEquals(200, response.statusCode)
        assertTrue(response.responseHeaders["Content-Security-Policy"]!!.contains("script-src 'none'"))
        assertTrue(response.responseHeaders["Content-Security-Policy"]!!.contains("form-action 'none'"))
        assertEquals("Fixture page", response.data.bufferedReader().readText())
        listOf("https://instagram.com/", "https://learning.example.evil.test/learn", "intent://escape", "/private", "/learn/%2e%2e/private").forEach { target ->
            var count = 0
            val denied = fetchRestricted(START, SOURCE) { url -> count++; FixtureConnection(url, code = 302, headers = mapOf("Location" to target)) }
            assertEquals(target, 403, denied.statusCode)
            assertEquals(target, 1, count)
        }
    }

    @Test fun restrictedTransportFailsClosedForErrorsDownloadsAndOversizedBodies() {
        val fixtures = listOf(
            FixtureConnection(START, code = 500),
            FixtureConnection(START, type = "video/mp4"),
            FixtureConnection(START, headers = mapOf("Content-Disposition" to "attachment; filename=lesson.html")),
            FixtureConnection(START, advertisedLength = 2_097_153),
            FixtureConnection(START, body = ByteArray(2_097_153), advertisedLength = -1),
            FixtureConnection(START, code = 302, headers = mapOf("Location" to START))
        )
        fixtures.forEach { fixture ->
            assertEquals(403, fetchRestricted(START, SOURCE) { fixture }.statusCode)
            assertTrue(fixture.disconnected)
        }
        assertEquals(403, fetchRestricted(START, SOURCE) { throw javax.net.ssl.SSLHandshakeException("untrusted certificate") }.statusCode)
        assertEquals(403, fetchRestricted(START, SOURCE) { throw java.net.SocketTimeoutException("timeout") }.statusCode)
    }

    private fun launch(href: String = "$START/next", popup: Boolean = false): ActivityScenario<BrowserHarnessActivity> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return ActivityScenario.launch(Intent(context, BrowserHarnessActivity::class.java).putExtra("href", href).putExtra("popup", popup))
    }
    private fun ready(scenario: ActivityScenario<BrowserHarnessActivity>) = await(scenario) { it.browser.visibility == View.VISIBLE && it.browser.progress == 100 }
    private fun await(scenario: ActivityScenario<BrowserHarnessActivity>, predicate: (BrowserHarnessActivity) -> Boolean) {
        val deadline = android.os.SystemClock.uptimeMillis() + 15000
        val result = AtomicReference(false)
        while (android.os.SystemClock.uptimeMillis() < deadline) {
            scenario.onActivity { result.set(predicate(it)) }
            if (result.get()) return
            android.os.SystemClock.sleep(50)
        }
        fail("WebView did not reach the expected state")
    }
    private class Request(private val address: String, private val verb: String = "GET", private val main: Boolean = true) : WebResourceRequest {
        override fun getUrl() = Uri.parse(address)
        override fun isForMainFrame() = main
        override fun isRedirect() = false
        override fun hasGesture() = true
        override fun getMethod() = verb
        override fun getRequestHeaders() = mutableMapOf<String, String>()
    }
    companion object {
        private const val START = "https://learning.example/learn"
        private val SOURCE = BrowserSource("test", "Fixture", "learning.example", setOf("science"), listOf("/learn"), enabled = true)
    }
}
