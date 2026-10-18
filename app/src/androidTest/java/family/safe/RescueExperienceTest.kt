package family.safe

import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import family.safe.data.NasaReview
import family.safe.data.FamilySettings
import family.safe.data.ApprovalRequest
import family.safe.data.SettingsStore
import family.safe.browser.BrowserSource
import family.safe.policy.Category
import family.safe.policy.QueryPolicy
import family.safe.search.EducationalSearchClient
import family.safe.search.EducationalResponse
import family.safe.search.EducationalResult
import family.safe.search.EducationalSource
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import family.safe.video.nasa.NasaVideo
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestName
import java.io.File

/** Fixture-only acceptance checks. Never reset or capture a physical/family installation. */
class RescueExperienceTest {
    private val ui = createAndroidComposeRule<MainActivity>()
    private val currentTest = TestName()
    private val reset = object : ExternalResource() {
        override fun before() {
            requireDisposableEmulator()
            val file = File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "settings.json")
            check(!file.exists() || file.delete()) { "Could not reset disposable fixture settings" }
            if(currentTest.methodName in setOf("optedInTenLiveQueriesProduceReadingAndArticleEvidence", "editingQueryWhileLazyResultsRenderClearsStaleActions")) {
                val fixture=FamilySettings(pinHash="v1:600000:AAECAwQFBgcICQoLDA0ODw==:EvvCvbabu/NneEP7HwNcLFwerIOXZF/GSNbe+m9jQCk=")
                SettingsStore(file).save(fixture.copy(categories=fixture.categories.map { it.copy(enabled=true) },
                    sources=fixture.sources.map { it.copy(enabled=it.id in setOf("vikidia","wikijunior")) }))
            }
        }
    }
    @get:Rule val rules: RuleChain = RuleChain.outerRule(currentTest).around(reset).around(ui)

    @Before fun createFixturePin() {
        ui.waitUntil(15000) { ui.activity.settings != null }
        if(currentTest.methodName in setOf("optedInTenLiveQueriesProduceReadingAndArticleEvidence", "editingQueryWhileLazyResultsRenderClearsStaleActions")) return
        ui.onNodeWithText("Parent PIN").performTextInput(PIN)
        ui.onNodeWithText("Confirm PIN").performTextInput(PIN)
        ui.onNodeWithText("Create PIN").performScrollTo().performClick()
        ui.waitUntil(120000) { ui.activity.parent && !ui.activity.busy }
        evidenceFile("rescue-startup-timings.json").writeText(
            JSONObject(ui.runOnIdle { ui.activity.performance.toMap() }).put("clock", "monotonic").toString(2))
    }

    @Test fun discoverKeepsParentConfigurationBehindThePin() {
        ui.runOnIdle { ui.activity.update { state -> state.copy(
            categories = listOf(Category("fixture", PRIVATE_TOPIC, setOf(PRIVATE_PHRASE), true)),
            sources = state.sources.map { it.copy(enabled = true, categories = setOf("fixture")) },
            blocked = setOf(PRIVATE_BLOCKED)
        ) } }
        saved()
        capture("rescue-parent-fixture")
        lock()
        ui.onNodeWithText("Discover").assertIsDisplayed()
        ui.onNodeWithTag("discover-query").assertExists()
        ui.onNodeWithTag("discover-search").assertIsNotEnabled()
        assertNoParentConfiguration()
        capture("rescue-discover")
        ui.onNodeWithTag("parent-entry").assertIsDisplayed().performClick()
        ui.onNodeWithText("Parent access").assertIsDisplayed()
        ui.runOnIdle { assertFalse(ui.activity.parent); assertTrue(ui.activity.gate) }
        ui.onNodeWithTag("topic-name-fixture").assertDoesNotExist()
        ui.onNodeWithText("Back home").performScrollTo().performClick()
        ui.onNodeWithText("Discover").assertIsDisplayed()
        unlock()
        ui.onNodeWithText("Parent settings").assertIsDisplayed()
        ui.onNodeWithTag("topic-name-fixture").assertExists()
    }

    @Test fun editingQueryWhileLazyResultsRenderClearsStaleActions() {
        val source = EducationalSource.VIKIDIA
        val results = (1..20).map { index ->
            val title = "Animal fixture $index"
            EducationalResult(source, title, requireNotNull(source.articleUrl(title)))
        }
        repeat(4) { iteration ->
            ui.runOnIdle {
                val search = ui.activity.childSearch
                search.query = "animals"
                search.decision = QueryPolicy.evaluate("animals", ui.activity.settings!!.categories, ui.activity.settings!!.blocked)
                search.results = results
            }
            val tag = "read-vikidia-${results.last().title}"
            ui.onNodeWithTag("discover-content").performScrollToNode(hasTestTag(tag))
            val staleClick = requireNotNull(ui.onNodeWithTag(tag).assertIsDisplayed().fetchSemanticsNode()
                .config[androidx.compose.ui.semantics.SemanticsActions.OnClick].action)
            ui.onNodeWithTag("discover-query").performTextClearance()
            ui.onNodeWithTag("discover-query").performTextReplacement("unapproved fixture $iteration")
            // Invoke the formerly rendered action after editing; it must recheck the current query.
            ui.runOnIdle { staleClick() }
            ui.onNodeWithText("Discover").assertIsDisplayed()
            ui.onNodeWithTag(tag).assertDoesNotExist()
            ui.onAllNodes(hasTestTagPrefix("result-")).assertCountEquals(0)
            ui.runOnIdle {
                assertNull(ui.activity.childSearch.decision)
                assertTrue(ui.activity.childSearch.results.isEmpty())
                assertFalse(ui.activity.childSearch.searching)
                assertNull(findWebView(ui.activity.findViewById(android.R.id.content)))
                assertNull(ui.activity.nasaPlayback)
            }
        }
    }

    @Test fun blockedUnknownMixedAndDisabledSourcesNeverProduceResults() {
        ui.runOnIdle { ui.activity.update { state -> state.copy(
            categories = listOf(Category("animals", PRIVATE_TOPIC, setOf("animals"), true)),
            sources = state.sources.map { it.copy(enabled = false) },
            blocked = setOf("fixtureblocked")
        ) } }
        saved(); lock()
        listOf("fixtureblocked", "unapprovedfixture", "animals fixtureblocked", "animals unapprovedfixture", "animals").forEach { query ->
            search(query)
            ui.runOnIdle {
                assertFalse(ui.activity.childSearch.searching)
                assertTrue(ui.activity.childSearch.results.isEmpty())
                assertTrue(ui.activity.childSearch.providerMillis.isEmpty())
            }
            ui.onAllNodes(hasTestTagPrefix("result-")).assertCountEquals(0)
            ui.onNodeWithText(PRIVATE_TOPIC).assertDoesNotExist()
            ui.onNodeWithText("Learning categories").assertDoesNotExist()
            ui.onNodeWithText("Approved websites").assertDoesNotExist()
        }
        capture("rescue-empty")
    }

    @Test fun startupDoesNotRewriteSettings() {
        requireDisposableEmulator()
        saved()
        val file = File(ui.activity.filesDir, "settings.json")
        val settings = SettingsStore(file).load()
        val bytes = file.readBytes()
        check(file.setLastModified(System.currentTimeMillis() - 10000))
        val modified = file.lastModified()
        ui.activityRule.scenario.recreate()
        ui.waitUntil(15000) { ui.activity.settings != null }
        assertArrayEquals("Startup must preserve existing settings bytes", bytes, file.readBytes())
        assertEquals("Startup must not rewrite existing settings", modified, file.lastModified())
        ui.runOnIdle {
            assertEquals(settings, ui.activity.settings)
            assertFalse(ui.activity.parent)
            assertFalse(ui.activity.gate)
        }
    }

    @Test fun readingSourceRequiresExplicitParentConsentAndPersists() {
        ui.runOnIdle { ui.activity.update { state -> state.copy(
            categories = state.categories.map { it.copy(enabled = it.id == "animals") }
        ) } }
        saved()
        ui.onNodeWithText("Websites").performScrollTo().performClick()
        ui.onNodeWithTag("source-enable-vikidia").performScrollTo().performClick()
        ui.onNodeWithText("Cancel").performClick()
        ui.runOnIdle { assertFalse(ui.activity.settings!!.sources.single { it.id == "vikidia" }.enabled) }
        ui.onNodeWithTag("source-enable-vikidia").performScrollTo().performClick()
        ui.onNodeWithText("Approve and enable").performClick()
        saved()
        val file = File(ui.activity.filesDir, "settings.json")
        assertTrue(SettingsStore(file).load().sources.single { it.id == "vikidia" }.enabled)
        ui.activityRule.scenario.recreate()
        ui.waitUntil(15000) { ui.activity.settings != null }
        ui.runOnIdle {
            assertFalse(ui.activity.parent)
            assertTrue(ui.activity.settings!!.sources.single { it.id == "vikidia" }.enabled)
        }
        ui.onNodeWithTag("discover-query").assertIsDisplayed()
        ui.onNodeWithText("Vikidia").assertDoesNotExist()
        ui.onNodeWithText("en.vikidia.org", substring = true).assertDoesNotExist()
    }

    @Test fun readingSetupConnectsOnlyExplicitSubjects() {
        ui.runOnIdle { ui.activity.update { state -> state.copy(
            categories = state.categories.map { it.copy(enabled = it.id == "math") },
            sources = state.sources.map { it.copy(enabled = false) },
            blocked = state.blocked + "fixturesetupblocked",
            requests = listOf(ApprovalRequest("fixture-reading-setup", "fixture pending question"))
        ) } }
        saved()
        val before = ui.runOnIdle { requireNotNull(ui.activity.settings) }
        ui.onNodeWithTag("set-up-reading").performScrollTo().performClick()
        ui.onNodeWithTag("reading-setup-confirm").assertIsNotEnabled()
        ui.onNodeWithTag("reading-setup-subject-animals").performScrollTo().assertIsOff().performClick()
        ui.onNodeWithTag("reading-setup-confirm").assertIsNotEnabled()
        ui.onNodeWithTag("reading-setup-consent").performScrollTo().assertIsOff().performClick()
        ui.onNodeWithTag("reading-setup-confirm").assertIsEnabled().performClick()
        saved()
        val after = ui.runOnIdle { requireNotNull(ui.activity.settings) }
        listOf("vikidia", "wikijunior").forEach { id ->
            val source = after.sources.single { it.id == id }
            assertTrue(source.enabled)
            assertEquals(setOf("animals"), source.categories)
        }
        assertTrue(after.categories.single { it.id == "animals" }.enabled)
        assertTrue(after.categories.single { it.id == "math" }.enabled)
        assertEquals(before.sources.filter { it.id !in setOf("vikidia", "wikijunior") },
            after.sources.filter { it.id !in setOf("vikidia", "wikijunior") })
        assertEquals(before.pinHash, after.pinHash)
        assertEquals(before.blocked, after.blocked)
        assertEquals(before.requests, after.requests)
        assertEquals(after, SettingsStore(File(ui.activity.filesDir, "settings.json")).load())
        ui.activityRule.scenario.recreate()
        ui.waitUntil(15000) { ui.activity.settings != null }
        ui.runOnIdle { assertEquals(after, ui.activity.settings); assertFalse(ui.activity.parent); assertFalse(ui.activity.gate) }
        ui.onNodeWithText("Discover").assertIsDisplayed()
    }

    @Test fun customWebsiteStaysPrivateUntilAnAuthorizedQueryAndDisappearsForDenials() {
        val source = BrowserSource("custom-fixture", "Fixture custom reading", "learn.example.org",
            setOf("animals"), listOf("/lessons"), enabled = true)
        ui.runOnIdle { ui.activity.update { state -> state.copy(
            categories = listOf(Category("animals", PRIVATE_TOPIC, setOf("animals"), true)),
            sources = listOf(source), blocked = setOf("fixtureblocked")
        ) } }
        saved(); lock()
        ui.onNodeWithText(source.name).assertDoesNotExist()
        ui.onNodeWithText(source.domain, substring = true).assertDoesNotExist()
        ui.onNodeWithTag("browse-${source.id}").assertDoesNotExist()
        search("animals")
        ui.onNodeWithTag("discover-content").performScrollToNode(hasTestTag("browse-${source.id}"))
        ui.onNodeWithTag("browse-${source.id}").assertIsDisplayed().assertIsEnabled()
        ui.onNodeWithText(source.name).assertExists()
        ui.runOnIdle {
            assertTrue(ui.activity.childSearch.decision?.allowed == true)
            assertFalse(ui.activity.childSearch.searching)
            assertTrue(ui.activity.childSearch.providerMillis.isEmpty())
        }
        listOf("animals unapprovedfixture", "animals fixtureblocked").forEach { query ->
            search(query)
            ui.onNodeWithTag("browse-${source.id}").assertDoesNotExist()
            ui.onNodeWithText(source.name).assertDoesNotExist()
            ui.onNodeWithText(source.domain, substring = true).assertDoesNotExist()
            ui.runOnIdle {
                assertFalse(ui.activity.childSearch.decision?.allowed == true)
                assertTrue(ui.activity.childSearch.providerMillis.isEmpty())
            }
        }
        assertEquals(listOf(source), SettingsStore(File(ui.activity.filesDir, "settings.json")).load().sources)
    }

    @Test fun watchShowsOnlyEligibleApprovedFixturesAndRecreationPreservesSettingsBytes() {
        val approved = video("fixture-approved", "Fixture approved lesson")
        val pending = video("fixture-pending", "Fixture pending lesson")
        val rejected = video("fixture-rejected", "Fixture rejected lesson")
        val disabled = video("fixture-disabled", "Fixture disabled lesson", "disabled")
        val blocked = video("fixture-blocked", "fixtureblocked lesson")
        ui.runOnIdle { ui.activity.update { state -> state.copy(
            categories = listOf(Category("space", PRIVATE_TOPIC, setOf("planets"), true), Category("disabled", "Fixture disabled category", setOf("disabled"))),
            blocked = setOf("fixtureblocked"),
            nasaVideos = listOf(approved, pending, rejected, disabled, blocked),
            nasaReviews = listOf(NasaReview(approved.id, true, HASH), NasaReview(rejected.id, false), NasaReview(disabled.id, true, HASH), NasaReview(blocked.id, true, HASH))
        ) } }
        saved(); lock()
        ui.onNodeWithTag("discover-watch").assertIsDisplayed().performClick()
        ui.onNodeWithTag("nasa-play-${approved.id}").assertExists()
        listOf(pending, rejected, disabled, blocked).forEach {
            ui.onNodeWithTag("nasa-play-${it.id}").assertDoesNotExist()
            ui.onNodeWithText(it.title).assertDoesNotExist()
        }
        capture("rescue-watch")
        val file = File(ui.activity.filesDir, "settings.json")
        val bytes = file.readBytes()
        check(file.setLastModified(System.currentTimeMillis() - 10000))
        val modified = file.lastModified()
        ui.activityRule.scenario.recreate()
        ui.waitUntil(15000) { ui.activity.settings != null }
        assertArrayEquals("Recreation must not rewrite existing settings", bytes, file.readBytes())
        assertEquals("Startup must preserve the settings file timestamp", modified, file.lastModified())
        ui.runOnIdle { assertFalse(ui.activity.parent); assertFalse(ui.activity.gate) }
        assertEquals(listOf(approved, pending, rejected, disabled, blocked), SettingsStore(file).load().nasaVideos)
        ui.onNodeWithTag("discover-watch").assertIsDisplayed().performClick()
        ui.onNodeWithTag("nasa-play-${approved.id}").assertExists()
        unlock()
        ui.runOnIdle { ui.activity.update { it.copy(nasaReviews = emptyList()) } }
        saved(); lock()
        ui.onNodeWithTag("discover-watch").assertDoesNotExist()
    }

    @Test fun retryRechecksDisabledSourcesWithoutSendingAnotherProviderRequest() {
        ui.runOnIdle { ui.activity.update { state -> state.copy(
            categories = listOf(Category("animals", PRIVATE_TOPIC, setOf("animals"), true)),
            sources = state.sources.map { it.copy(enabled = false) }
        ) } }
        saved(); lock()
        val scope = MainScope()
        try {
            ui.runOnIdle {
                val state = ui.activity.childSearch
                state.query = "animals"
                state.decision = QueryPolicy.evaluate(state.query, ui.activity.settings!!.categories, ui.activity.settings!!.blocked)
                state.start(scope, listOf(EducationalSource.VIKIDIA), "animals", emptySet(),
                    eligible = { !ui.activity.parent && !ui.activity.gate },
                    clientFactory = { EducationalSearchClient { EducationalResponse(503, "") } })
            }
            ui.waitUntil(15000) { ui.runOnIdle { !ui.activity.childSearch.searching } }
            ui.onNodeWithTag("discover-content").performScrollToNode(hasTestTag("discover-retry"))
            ui.onNodeWithTag("discover-retry").assertIsDisplayed().assertIsEnabled()
            val generation = ui.runOnIdle { ui.activity.childSearch.generation }
            ui.onNodeWithTag("discover-retry").performClick()
            ui.runOnIdle {
                assertTrue(ui.activity.childSearch.generation > generation)
                assertFalse(ui.activity.childSearch.searching)
                assertTrue(ui.activity.childSearch.results.isEmpty())
            }
            ui.onNodeWithText(PRIVATE_TOPIC).assertDoesNotExist()
            capture("rescue-retry-empty")
        } finally { scope.cancel() }
    }
    @Test fun optedInTenLiveQueriesProduceReadingAndArticleEvidence() {
        assumeTrue("Requires explicit rescueLive=true", InstrumentationRegistry.getArguments().getString("rescueLive") == "true")
        val samples = JSONArray()
        var totalResults = 0
        SAMPLE_QUERIES.forEach { query ->
            val started = SystemClock.elapsedRealtime()
            search(query)
            val interactionMillis = SystemClock.elapsedRealtime() - started
            ui.waitUntil(60000) { ui.runOnIdle { ui.activity.childSearch.results.isNotEmpty() || !ui.activity.childSearch.searching } }
            val first = ui.runOnIdle { ui.activity.childSearch.results.firstOrNull() }
            val firstDisplayedMillis = first?.let {
                ui.onNodeWithTag("result-${it.source.id}-${it.title}").assertIsDisplayed()
                SystemClock.elapsedRealtime() - started
            }
            ui.waitUntil(60000) { ui.runOnIdle { !ui.activity.childSearch.searching } }
            val entry = ui.runOnIdle {
                val state = ui.activity.childSearch
                assertTrue("Fixture sample must be allowed: $query", state.decision?.allowed == true)
                totalResults += state.results.size
                JSONObject().put("sample", query).put("wallMs", SystemClock.elapsedRealtime() - started)
                    .put("interactionMs", interactionMillis).put("inputStateMs", ui.activity.performance["input_state_ms"] ?: JSONObject.NULL).put("inputFeedbackMs", ui.activity.performance["input_feedback_ms"] ?: JSONObject.NULL)
                    .put("titles", JSONArray(state.results.map { it.title }))
                    .put("resultCount", state.results.size).put("providerMs", JSONObject(state.providerMillis.mapKeys { it.key.id }))
                    .put("firstResultMs", state.firstResultMillis ?: JSONObject.NULL).put("firstDisplayedObservedMs", firstDisplayedMillis ?: JSONObject.NULL).put("elapsedMs", state.elapsedMillis ?: JSONObject.NULL)
            }
            samples.put(entry)
            evidenceFile("rescue-live-timings.json").writeText(JSONObject().put("clock", "monotonic").put("fixtureQueriesOnly", true).put("startupMs", JSONObject(ui.runOnIdle { ui.activity.performance.toMap() })).put("samples", samples).toString(2))
        }
        assertTrue("Ten real provider samples returned no reading results", totalResults > 0)
        search("animals")
        ui.waitUntil(60000) { ui.runOnIdle { !ui.activity.childSearch.searching } }
        val result = ui.runOnIdle { ui.activity.childSearch.results.firstOrNull { it.source.id == "vikidia" && it.title == "Animal" } }
        assertNotNull("Real animals search must return the expected readable Animal result", result)
        capture("rescue-results")
        ui.onNodeWithTag("discover-content").performScrollToNode(hasTestTag("read-vikidia-Animal"))
        val articleStarted = SystemClock.elapsedRealtime()
        ui.onNodeWithTag("read-vikidia-Animal").assertIsDisplayed().performClick()
        ui.waitUntil(60000) { ui.runOnIdle { findWebView(ui.activity.findViewById(android.R.id.content))?.let {
            it.visibility == View.VISIBLE && it.progress == 100 && it.url == "https://en.vikidia.org/wiki/Animal" &&
                it.title?.contains("Animal", true) == true && it.contentHeight > 500
        } == true } }
        val reportFile = evidenceFile("rescue-live-timings.json")
        reportFile.writeText(JSONObject(reportFile.readText()).put("articleElapsedMs", SystemClock.elapsedRealtime() - articleStarted).toString(2))
        capture("rescue-article")
        ui.onNodeWithText("Back to FamilySafe").performClick()
        ui.onNodeWithTag("discover-query").assertTextContains("animals")
        ui.runOnIdle {
            assertTrue("Returning from the article must retain the exact Animal result",
                ui.activity.childSearch.results.any { it.source.id == "vikidia" && it.title == "Animal" && it.url == "https://en.vikidia.org/wiki/Animal" })
        }
        ui.onNodeWithTag("discover-content").performScrollToNode(hasTestTag("read-vikidia-Animal"))
        ui.onNodeWithTag("read-vikidia-Animal").assertIsDisplayed()
    }

    private fun assertNoParentConfiguration() {
        listOf("Learning categories", "Approved websites", PRIVATE_TOPIC, PRIVATE_PHRASE, PRIVATE_BLOCKED).forEach { ui.onNodeWithText(it, substring = true).assertDoesNotExist() }
        ui.runOnIdle { ui.activity.settings!!.sources.flatMap { listOf(it.name, it.domain) } }.forEach { ui.onNodeWithText(it, substring = true).assertDoesNotExist() }
        ui.onNodeWithTag("discover-watch").assertDoesNotExist()
    }
    private fun search(query: String) {
        ui.onNodeWithTag("discover-query").assertIsDisplayed().performTextReplacement(query)
        ui.onNodeWithTag("discover-search").assertIsDisplayed().performClick()
    }
    private fun saved() = ui.waitUntil(15000) { !ui.activity.busy }
    private fun lock() = ui.onNodeWithText("Lock and return home").performScrollTo().performClick()
    private fun unlock() {
        ui.onNodeWithTag("parent-entry").assertIsDisplayed().performClick()
        ui.onNodeWithText("Parent PIN").performTextInput(PIN)
        ui.onNodeWithText("Unlock").performScrollTo().performClick()
        ui.waitUntil(120000) { ui.activity.parent && !ui.activity.busy }
    }
    private fun video(id: String, title: String, category: String = "space") = NasaVideo(id, title, "Disposable engineering fixture", "NASA fixture", "Fixture only; no content approval established", category)
    private fun hasTestTagPrefix(prefix: String) = SemanticsMatcher("tag starts with $prefix") { androidx.compose.ui.semantics.SemanticsProperties.TestTag in it.config && it.config[androidx.compose.ui.semantics.SemanticsProperties.TestTag].startsWith(prefix) }
    private fun findWebView(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) for (index in 0 until view.childCount) findWebView(view.getChildAt(index))?.let { return it }
        return null
    }
    private fun evidenceFile(name: String): File = File(requireNotNull(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir("evidence")).apply { mkdirs() }, name)
    private fun capture(name: String) {
        requireDisposableEmulator()
        ui.runOnIdle { check(!ui.activity.gate && ui.activity.settings?.pinHash != null); ui.activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        try {
            instrumentation.waitForIdleSync()
            SystemClock.sleep(250)
            val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
            try { evidenceFile("$name.png").outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) } }
            finally { bitmap.recycle() }
        } finally { ui.runOnIdle { ui.activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE) } }
    }
    private fun requireDisposableEmulator() {
        check(InstrumentationRegistry.getArguments().getString("disposableInstrumentation") == "true") { "Requires explicit disposableInstrumentation=true before fixture reset/capture" }
        check(BuildConfig.DEBUG && InstrumentationRegistry.getInstrumentation().targetContext.packageName == "family.safe.debug")
        check(Build.HARDWARE in setOf("ranchu", "goldfish")) { "Requires a dedicated disposable ranchu/goldfish emulator" }
    }
    companion object {
        private const val PIN = "732918"
        private val HASH = "a".repeat(64)
        private const val PRIVATE_TOPIC = "Fixture private topic name"
        private const val PRIVATE_PHRASE = "fixture private approved phrase"
        private const val PRIVATE_BLOCKED = "fixtureprivateblocked"
        private val SAMPLE_QUERIES = listOf("animals", "dinosaurs", "planets", "solar system", "mathematics", "fractions", "geography", "continents", "football", "football rules")
    }
}
