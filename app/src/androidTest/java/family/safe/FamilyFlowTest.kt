package family.safe

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import family.safe.data.SettingsStore
import family.safe.browser.BrowserSource
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import java.io.File

/** Explicit opt-in: run only on a disposable emulator, never a family's configured tablet. */
class FamilyFlowTest {
    private val ui = createAndroidComposeRule<MainActivity>()
    private val freshSettings = object : ExternalResource() {
        override fun before() {
            check(InstrumentationRegistry.getArguments().getString("disposableInstrumentation") == "true") {
                "Requires -e disposableInstrumentation true on a disposable emulator."
            }
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            check(BuildConfig.DEBUG && context.packageName == "family.safe.debug")
            check(android.os.Build.HARDWARE in setOf("ranchu", "goldfish"))
            val settings = File(context.filesDir, "settings.json")
            check(!settings.exists() || settings.delete())
        }
    }
    @get:Rule val rules: RuleChain = RuleChain.outerRule(freshSettings).around(ui)

    @Before fun createParentPin() {
        loaded()
        ui.onNodeWithText("Parent PIN").performTextInput(PIN)
        ui.onNodeWithText("Confirm PIN").performTextInput(PIN)
        ui.onNodeWithText("Create PIN").performScrollTo().performClick()
        ui.waitUntil(60000) { ui.activity.parent && !ui.activity.busy }
    }

    @Test fun homeCategoryAndSearchRequireApprovedWholePhrases() {
        ui.onAllNodes(isToggleable())[0].performScrollTo().performClick()
        saved()
        ui.onNodeWithText("Lock and return home").performScrollTo()
        captureLayout("parent-top")
        ui.onNodeWithText("Lock and return home").performScrollTo().performClick()
        ui.onNodeWithText("Discover").assertIsDisplayed()
        captureLayout("child-home")
        ui.onNodeWithText("Science").assertDoesNotExist()
        search("science")
        ui.onNodeWithTag("discover-query").assertTextContains("science")
        search("plant cells")
        ui.onNodeWithText(VIDEO_DISABLED).assertExists()
        search("plant cells entertainment")
        ui.onNodeWithText("Ask my parent").assertExists()
        search("tiktok")
        ui.onNodeWithText("Ask my parent").assertDoesNotExist()
        ui.onNodeWithText(VIDEO_DISABLED).assertDoesNotExist()
    }

    @Test fun deniedQueriesHideApprovedWebsiteResults() {
        ui.runOnIdle {
            ui.activity.update { state -> state.copy(
                categories = state.categories.map { it.copy(enabled = it.id == "science") },
                sources = state.sources.map { it.copy(enabled = it.id == "nasa") }
            ) }
        }
        saved()
        ui.onNodeWithText("Lock and return home").performScrollTo().performClick()
        ui.onNodeWithTag("browse-nasa").assertDoesNotExist()
        search("plant cells")
        ui.runOnIdle { assertTrue(ui.activity.childSearch.decision?.allowed == true); assertTrue(ui.activity.childSearch.results.isEmpty()) }
        ui.onNodeWithTag("discover-content").performScrollToNode(hasTestTag("browse-nasa"))
        ui.onNodeWithTag("browse-nasa").assertIsDisplayed().assertIsEnabled()
        listOf("plant cells gaming", "tiktok").forEach { query ->
            search(query)
            if(query == "tiktok") ui.onNodeWithText("Ask my parent").assertDoesNotExist()
            else ui.onNodeWithText("Ask my parent").assertExists()
            ui.onNodeWithText("NASA Space Place").assertDoesNotExist()
            ui.onNodeWithTag("browse-nasa").assertDoesNotExist()
        }
    }

    @Test fun parentSuggestedPhrasesNeedSavingAndPreserveQueryBoundaries() {
        ui.runOnIdle {
            ui.activity.update { state -> state.copy(
                categories = state.categories.map {
                    when (it.id) {
                        "sports" -> it.copy(enabled = true, terms = setOf("football passing drills"))
                        "science" -> it.copy(enabled = true)
                        else -> it
                    }
                },
                sources = state.sources.map { it.copy(enabled = it.id == "nasa") } +
                    BrowserSource("training", "Football lessons", "learn.example.org", setOf("sports"), enabled = true)
            ) }
        }
        saved()
        ui.onNodeWithTag("suggest-phrases-sports").performScrollTo().performClick()
        ui.onNodeWithTag("topic-phrases-sports").assertTextContains("football passing drills for kids", substring = true)
        ui.runOnIdle {
            assertEquals(setOf("football passing drills"), ui.activity.settings!!.categories.single { it.id == "sports" }.terms)
        }
        ui.onNodeWithTag("save-phrases-sports").performScrollTo().performClick()
        saved()
        ui.activityRule.scenario.recreate()
        loaded()
        val stored = SettingsStore(File(ui.activity.filesDir, "settings.json")).load()
        assertTrue("football passing drills for kids" in stored.categories.single { it.id == "sports" }.terms)
        ui.runOnIdle { assertFalse(ui.activity.parent) }
        search("Football passing drills for kids")
        ui.runOnIdle { assertTrue(ui.activity.childSearch.decision?.allowed == true) }
        ui.onNodeWithTag("discover-content").performScrollToNode(hasTestTag("browse-training"))
        ui.onNodeWithTag("browse-training").assertIsDisplayed().assertIsEnabled()
        ui.onNodeWithText("Football lessons").assertExists()
        ui.onNodeWithText("NASA Space Place").assertDoesNotExist()
        ui.onNodeWithTag("browse-nasa").assertDoesNotExist()
        ui.onNodeWithText("Ask my parent").assertDoesNotExist()
        search("football passing drills and gaming")
        ui.onNodeWithText("Ask my parent").assertExists()
        ui.onNodeWithTag("browse-training").assertDoesNotExist()
        ui.onNodeWithText("Football lessons").assertDoesNotExist()
        ui.onNodeWithTag("browse-nasa").assertDoesNotExist()
    }

    @Test fun parentApprovalPersistsAndDuplicateRequestsDoNotAccumulate() {
        ui.onAllNodes(isToggleable())[0].performScrollTo().performClick()
        saved()
        ui.onNodeWithText("Lock and return home").performScrollTo().performClick()
        search("volcano rocks")
        ui.onNodeWithTag("discover-content").performScrollToNode(hasText("Ask my parent"))
        ui.onNodeWithText("Ask my parent").performClick()
        saved()
        ui.onNodeWithText("Request sent").assertIsNotEnabled()
        ui.runOnIdle { ui.activity.requestApproval("volcano rocks"); assertEquals(1, ui.activity.settings!!.requests.size) }
        unlock()
        ui.onNodeWithText("Requests").performScrollTo().performClick()
        ui.onNodeWithText("Approve subject").performScrollTo().performClick()
        saved()
        ui.activityRule.scenario.recreate()
        loaded()
        ui.runOnIdle {
            assertFalse(ui.activity.parent)
            assertEquals("approved subject", ui.activity.settings!!.requests.single().status)
            assertTrue("volcano rocks" in ui.activity.settings!!.categories.first().terms)
        }
        search("volcano rocks")
        ui.onNodeWithText(VIDEO_DISABLED).assertExists()
    }

    @Test fun wrongPinAndThrottleSurviveActivityRecreation() {
        ui.onNodeWithText("Lock and return home").performScrollTo().performClick()
        ui.onNodeWithTag("parent-entry").assertIsDisplayed().performClick()
        enterPin("000000")
        authenticationFinished()
        ui.runOnIdle { assertFalse(ui.activity.parent); assertEquals(1, ui.activity.settings!!.attempts.failures) }
        ui.waitUntil(10000) { ui.activity.settings!!.attempts.canTry(System.currentTimeMillis()) }
        enterPin("111111")
        authenticationFinished()
        val attempts = ui.runOnIdle { ui.activity.settings!!.attempts }
        assertEquals(2, attempts.failures)
        ui.activityRule.scenario.recreate()
        loaded()
        ui.runOnIdle { assertEquals(attempts, ui.activity.settings!!.attempts); assertFalse(ui.activity.parent) }
        assertEquals(attempts, SettingsStore(File(ui.activity.filesDir, "settings.json")).load().attempts)
        ui.onNodeWithTag("parent-entry").assertIsDisplayed().performClick()
        if (!attempts.canTry(System.currentTimeMillis())) {
            enterPin(PIN)
            ui.runOnIdle { assertFalse(ui.activity.parent); assertEquals(attempts, ui.activity.settings!!.attempts) }
        }
        ui.waitUntil(15000) { attempts.canTry(System.currentTimeMillis()) }
        enterPin(PIN)
        ui.waitUntil(60000) { ui.activity.parent && !ui.activity.busy }
        ui.runOnIdle { assertEquals(0, ui.activity.settings!!.attempts.failures) }
    }

    @Test fun backAndBackgroundAlwaysRelockParentAndClearKey() {
        ui.runOnIdle { ui.activity.onBackPressedDispatcher.onBackPressed() }
        ui.onNodeWithTag("discover-query").assertExists()
        unlock()
        ui.runOnIdle { ui.activity.apiKey = "test-memory-only-key" }
        ui.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        ui.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        ui.runOnIdle { assertFalse(ui.activity.parent); assertEquals("", ui.activity.apiKey) }
        ui.onNodeWithTag("parent-entry").assertIsDisplayed().performClick()
        ui.onNodeWithText("Parent PIN").assertExists()
        ui.runOnIdle { ui.activity.onBackPressedDispatcher.onBackPressed() }
        ui.onNodeWithTag("discover-query").assertExists()
    }

    @Test fun settingsRoundTripDoesNotPersistParentSession() {
        ui.onAllNodes(isToggleable())[0].performScrollTo().performClick()
        saved()
        ui.onNodeWithText("Words").performScrollTo().performClick()
        ui.onNodeWithText("New blocked word or phrase").performScrollTo().performTextInput("test blocked phrase")
        ui.onNodeWithText("Block phrase").performScrollTo().performClick()
        saved()
        val expected = ui.runOnIdle { ui.activity.settings!! }
        ui.activityRule.scenario.recreate()
        loaded()
        ui.runOnIdle { assertEquals(expected, ui.activity.settings); assertFalse(ui.activity.parent) }
        assertEquals(expected, SettingsStore(File(ui.activity.filesDir, "settings.json")).load())
        ui.onNodeWithTag("discover-query").assertExists()
    }

    @Test fun authenticationFinishingInBackgroundCannotRestoreParentSession() {
        ui.onNodeWithText("Lock and return home").performScrollTo().performClick()
        // Background immediately after starting authentication, while hashing may still be running.
        ui.runOnIdle { ui.activity.authenticate(PIN, "") }
        ui.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        ui.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        authenticationFinished()
        ui.runOnIdle { assertFalse(ui.activity.parent) }
        ui.onNodeWithTag("discover-query").assertExists()
    }

    private fun loaded() = ui.waitUntil(15000) { ui.activity.settings != null }
    private fun saved() = ui.waitUntil(15000) { !ui.activity.busy }
    private fun authenticationFinished() {
        val started=android.os.SystemClock.elapsedRealtime()
        // Emulated PIN derivation can exceed the separate 15s file-write wait.
        ui.waitUntil(60000) { !ui.activity.busy }
        android.util.Log.i("FamilySafePinTiming","Authentication wait ms=${android.os.SystemClock.elapsedRealtime()-started}")
    }
    private fun captureLayout(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val secureFlag = android.view.WindowManager.LayoutParams.FLAG_SECURE
        // Test-only screenshot override on the explicitly disposable debug emulator.
        ui.runOnIdle { ui.activity.window.clearFlags(secureFlag) }
        try {
            instrumentation.waitForIdleSync()
            android.os.SystemClock.sleep(250)
            val screenshot = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
            try {
                val directory = requireNotNull(instrumentation.targetContext.getExternalFilesDir("evidence")).apply { mkdirs() }
                File(directory, "$name.png").outputStream().use {
                    check(screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it))
                }
            } finally { screenshot.recycle() }
        } finally { ui.runOnIdle { ui.activity.window.addFlags(secureFlag) } }
    }
    private fun search(text: String) {
        ui.onNodeWithTag("discover-query").assertIsDisplayed().performTextReplacement(text)
        ui.onNodeWithTag("discover-search").assertIsDisplayed().performClick()
    }
    private fun unlock() {
        ui.onNodeWithTag("parent-entry").assertIsDisplayed().performClick()
        enterPin(PIN)
        ui.waitUntil(60000) { ui.activity.parent && !ui.activity.busy }
    }
    private fun enterPin(pin: String) {
        ui.onNodeWithText("Parent PIN").performTextReplacement(pin)
        ui.onNodeWithText("Unlock").performScrollTo().performClick()
    }
    companion object {
        private const val PIN = "732918"
        private const val VIDEO_DISABLED = "Nothing to read here yet"
    }
}
