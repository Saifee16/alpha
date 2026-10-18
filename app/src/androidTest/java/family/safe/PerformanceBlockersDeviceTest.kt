package family.safe

import android.os.Build
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import family.safe.browser.BrowserSource
import family.safe.browser.createProtectedWebView
import family.safe.browser.fetchRestricted
import family.safe.browser.UrlPolicy
import family.safe.data.FamilySettings
import family.safe.data.SettingsStore
import family.safe.policy.QueryPolicy
import family.safe.search.EducationalResult
import family.safe.search.EducationalSource
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import java.io.ByteArrayInputStream
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import android.webkit.WebResourceResponse
import android.widget.FrameLayout

/** Explicitly opted-in, public-fixture measurements; never run on a family installation. */
class PerformanceBlockersDeviceTest {
    private val ui = createAndroidComposeRule<MainActivity>()
    private val args get() = InstrumentationRegistry.getArguments()
    private val count get() = (args.getString("performanceSamples")?.toInt() ?: 10).coerceIn(3, 20)
    private val reset = object : ExternalResource() {
        override fun before() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            check(args.getString("disposableInstrumentation") == "true")
            check(BuildConfig.BUILD_TYPE == "debug" && context.packageName == "family.safe.debug")
            check(Build.HARDWARE == "ranchu")
            val fixture = FamilySettings(pinHash = RECORD)
            SettingsStore(File(context.filesDir, "settings.json")).save(fixture.copy(
                categories = fixture.categories.map { it.copy(enabled = true) },
                sources = fixture.sources.map { it.copy(enabled = it.id in setOf("vikidia", "wikijunior")) }))
        }
    }
    @get:Rule val rules: RuleChain = RuleChain.outerRule(reset).around(ui)
    @Before fun loaded() { ui.waitUntil(30000) { ui.activity.settings != null } }

    @Test fun prepareFixtureForColdLaunch() {
        ui.runOnIdle { assertEquals(RECORD, ui.activity.settings!!.pinHash) }
    }

    @Test fun productionPinUnlockDistribution() {
        val samples = JSONArray()
        repeat(count) { run ->
            ui.onNodeWithTag("parent-entry").performClick()
            ui.onNodeWithText("Parent PIN").performTextInput("732918")
            ui.onNodeWithText("Unlock").performScrollTo().performClick()
            ui.waitUntil(60000) { ui.activity.parent && !ui.activity.busy }
            samples.put(ui.runOnIdle {
                assertEquals(RECORD, ui.activity.settings!!.pinHash)
                assertEquals(0, ui.activity.settings!!.attempts.failures)
                JSONObject().put("run", run + 1).put("unlockMs", ui.activity.performance.getValue("pin_unlock_ms"))
            })
            report("unlock", JSONObject().put("samples", samples))
            ui.runOnIdle { ui.activity.lock() }
        }
    }

    @Test fun productionInputAndArticleDistributions() {
        val empty = JSONArray()
        repeat(count) { run ->
            ui.runOnIdle { ui.activity.childSearch.cancel(clear = true) }
            empty.put(input("a".repeat(run + 1)).put("run", run + 1))
        }
        val populated = JSONArray()
        repeat(count) { run ->
            seedReadingResult()
            populated.put(input("animals ").put("run", run + 1).put("priorResults", 1))
            ui.runOnIdle { assertTrue(ui.activity.childSearch.results.isEmpty()) }
        }
        report("input", JSONObject().put("emptyStateEdits", empty).put("resultClearingEdits", populated))
        assumeTrue("Requires explicit performanceLive=true", args.getString("performanceLive") == "true")
        val articles = JSONArray()
        repeat(count) { run ->
            seedReadingResult()
            ui.onNodeWithTag("discover-content").performScrollToNode(hasTestTag("read-vikidia-Animal"))
            val started = SystemClock.elapsedRealtime()
            ui.onNodeWithTag("read-vikidia-Animal").performClick()
            var complete = false
            try {
                ui.waitUntil(60000) { ui.runOnIdle { findWebView(ui.activity.findViewById(android.R.id.content))?.let {
                    it.visibility == View.VISIBLE && it.progress == 100 && it.url == ARTICLE &&
                        it.title?.contains("Animal", true) == true && it.contentHeight > 500
                } == true } }
                complete = true
            } finally {
                articles.put(JSONObject().put("run", run + 1).put("firstOpenInTest", run == 0)
                    .put("completed", complete).put("observedMs", SystemClock.elapsedRealtime() - started))
                report("articles", JSONObject().put("samples", articles).put("includesTestSynchronization", true))
            }
            ui.onNodeWithText("Back to FamilySafe").performClick()
            ui.onNodeWithTag("discover-query").assertTextContains("animals")
        }
    }

    @Test fun hardenedReaderPhaseDiagnostics() {
        assumeTrue("Requires explicit performanceLive=true", args.getString("performanceLive") == "true")
        val samples = JSONArray()
        val source = BrowserSource("vikidia", "Vikidia", "en.vikidia.org", setOf("animals"),
            listOf("/wiki/Animal"), enabled = true, externalLinks = false, userGenerated = true)
        assertTrue("Diagnostic source must satisfy the unchanged production policy", UrlPolicy.allows(ARTICLE, source))
        // Existing injected fetch seam isolates renderer/factory cost from the live transport.
        for (live in listOf(false, true)) repeat(count) { run ->
            val phases = ConcurrentHashMap<String, Long>()
            val started = SystemClock.elapsedRealtime()
            fun elapsed() = SystemClock.elapsedRealtime() - started
            lateinit var browser: WebView
            lateinit var panel: FrameLayout
            ui.runOnUiThread {
                panel = FrameLayout(ui.activity)
                phases["constructStartMs"] = elapsed()
                browser = createProtectedWebView(ui.activity, source, {},
                    fetch = { url, source ->
                        phases["interceptStartMs"] = elapsed()
                        val response = if (live) fetchRestricted(url, source) else WebResourceResponse("text/html", "UTF-8",
                            ByteArrayInputStream("<html><head><title>Animal</title></head><body><h1>Animal</h1><p>Public renderer fixture.</p></body></html>".toByteArray()))
                        phases["responseReadyMs"] = elapsed()
                        phases["responseStatus"] = response.statusCode.toLong()
                        response
                    })
                phases["constructEndMs"] = elapsed()
                browser.viewTreeObserver.addOnDrawListener {
                    if (browser.visibility == View.VISIBLE) phases.putIfAbsent("firstVisibleDrawMs", elapsed())
                }
                panel.addView(browser, FrameLayout.LayoutParams(-1, -1))
                ui.activity.addContentView(panel, ViewGroup.LayoutParams(-1, -1))
                phases["loadIssuedMs"] = elapsed()
                browser.loadUrl(ARTICLE)
            }
            var complete = false
            val deadline = SystemClock.elapsedRealtime() + 60000
            while (!complete && SystemClock.elapsedRealtime() < deadline) {
                InstrumentationRegistry.getInstrumentation().runOnMainSync {
                    complete = browser.visibility == View.VISIBLE && browser.progress == 100 && browser.contentHeight > 0 &&
                        browser.url == ARTICLE && browser.title?.contains("Animal", true) == true
                    if (browser.visibility == View.VISIBLE) phases.putIfAbsent("visibleObservedMs", elapsed())
                }
                if (!complete) SystemClock.sleep(25)
            }
            val entry = JSONObject(phases.toMap()).put("run", run + 1).put("live", live)
                .put("completed", complete).put("completeObservedMs", elapsed())
            ui.runOnUiThread {
                entry.put("hardwareAccelerated", browser.isHardwareAccelerated).put("layerType", browser.layerType)
                    .put("width", browser.width).put("height", browser.height).put("contentHeight", browser.contentHeight)
                    .put("titleValid", browser.title?.contains("Animal", true) == true).put("urlMatches", browser.url == ARTICLE)
                    .put("webViewVersion", WebView.getCurrentWebViewPackage()?.versionName)
                browser.stopLoading(); browser.clearHistory(); browser.clearCache(true)
                panel.removeView(browser); browser.destroy()
                (panel.parent as ViewGroup).removeView(panel)
            }
            samples.put(entry)
            report("reader-phases", JSONObject().put("samples", samples))
            assertTrue("Public reader fixture must complete; phase evidence retained", complete)
        }
    }

    private fun input(text: String): JSONObject {
        ui.runOnIdle { ui.activity.performance.remove("input_feedback_ms") }
        ui.onNodeWithTag("discover-query").performTextReplacement(text)
        ui.waitUntil(10000) { ui.runOnIdle { "input_feedback_ms" in ui.activity.performance } }
        return ui.runOnIdle { JSONObject().put("stateMs", ui.activity.performance["input_state_ms"])
            .put("drawMs", ui.activity.performance["input_feedback_ms"]) }
    }
    private fun seedReadingResult() {
        ui.runOnIdle {
            val search = ui.activity.childSearch
            search.cancel(clear = true); search.query = "animals"
            val state = ui.activity.settings!!
            search.decision = QueryPolicy.evaluate("animals", state.categories, state.blocked)
            assertTrue(search.decision!!.allowed)
            search.results = listOf(EducationalResult(EducationalSource.VIKIDIA, "Animal", ARTICLE))
        }
    }
    private fun report(name: String, data: JSONObject) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val label = (args.getString("performanceLabel") ?: "unlabelled").replace(Regex("[^a-zA-Z0-9-]"), "")
        val directory = requireNotNull(context.getExternalFilesDir("evidence")).also { check(it.isDirectory || it.mkdirs()) }
        File(directory, "blockers-$label-$name.json").writeText(data.put("publicFixtureOnly", true).put("clock", "monotonic").toString(2))
    }
    private fun findWebView(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) for (index in 0 until view.childCount) findWebView(view.getChildAt(index))?.let { return it }
        return null
    }
    companion object {
        private const val ARTICLE = "https://en.vikidia.org/wiki/Animal"
        // Independent Python hashlib known-answer record, not a household credential.
        private const val RECORD = "v1:600000:AAECAwQFBgcICQoLDA0ODw==:EvvCvbabu/NneEP7HwNcLFwerIOXZF/GSNbe+m9jQCk="
    }
}
