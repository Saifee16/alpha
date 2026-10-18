package family.safe

import android.graphics.Bitmap
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import family.safe.browser.BrowserSource
import family.safe.data.ApprovalRequest
import family.safe.data.SettingsStore
import family.safe.policy.Category
import family.safe.settings.PinAttempts
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import java.io.File

/** Resets only an explicitly opted-in debug installation on a disposable emulator. */
class V1ParentDeviceTest {
    private val ui = createAndroidComposeRule<MainActivity>()
    private val freshSettings = object : ExternalResource() {
        override fun before() {
            requireDisposableEmulator()
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val settings = File(context.filesDir, "settings.json")
            check(!settings.exists() || settings.delete()) { "Could not reset disposable settings" }
        }
    }
    @get:Rule val rules: RuleChain = RuleChain.outerRule(freshSettings).around(ui)

    @Before fun createParentPin() {
        ui.waitUntil(15000) { ui.activity.settings != null }
        ui.onNodeWithText("Parent PIN").performTextInput(PIN)
        ui.onNodeWithText("Confirm PIN").performTextInput(PIN)
        ui.onNodeWithText("Create PIN").performScrollTo().performClick()
        ui.waitUntil(60000) { ui.activity.parent && !ui.activity.busy }
    }

    @Test fun renamedCustomTopicRulePreviewAndConfirmedResetPreserveAuthentication() {
        ui.runOnIdle {
            ui.activity.update { state -> state.copy(
                categories = state.categories + Category("custom-v1", "Practice", setOf("football passing drills"), true),
                sources = listOf(BrowserSource("fixture", "Practice library", "practice.example.org", setOf("custom-v1"), enabled = true)),
                blocked = state.blocked + "gaming", attempts = PinAttempts(3, 123456),
                requests = listOf(ApprovalRequest("retained-request", "piano lessons"),
                    ApprovalRequest("completed-request", "old request", "rejected"))
            ) }
        }
        saved()
        ui.onNodeWithTag("topic-name-custom-v1").performScrollTo().performTextReplacement("Ball skills")
        ui.onNodeWithTag("save-topic-name-custom-v1").performScrollTo().performClick()
        saved()
        ui.runOnIdle { assertEquals("Ball skills", ui.activity.settings!!.categories.single { it.id == "custom-v1" }.name) }
        ui.onNodeWithTag("topic-name-custom-v1").assertIsNotFocused()
        tab("Test rules")
        testRule("football passing drills for kids")
        ui.onNodeWithText("Outcome: Allowed by saved topic rules").assertExists()
        ui.onNodeWithText("Approved topic phrase: football passing drills").assertExists()
        ui.onNodeWithText("Categories: Ball skills").assertExists()
        ui.onNodeWithText("Approved sources: Practice library").assertExists()
        testRule("football passing drills gaming")
        ui.onNodeWithText("Outcome: Blocked content").assertExists()
        ui.onNodeWithText("Approved sources: None").assertExists()
        tab("Requests")
        ui.onNodeWithText("Clear completed requests").performScrollTo().performClick()
        ui.onNodeWithText("Cancel").performClick()
        ui.runOnIdle { assertEquals(2, ui.activity.settings!!.requests.size) }
        ui.onNodeWithText("Clear completed requests").performScrollTo().performClick()
        ui.onNodeWithText("Confirm").performClick()
        saved()
        ui.runOnIdle { assertEquals(listOf("retained-request"), ui.activity.settings!!.requests.map { it.id }) }
        val before = ui.runOnIdle { ui.activity.settings!! }
        tab("PIN")
        ui.onNodeWithText("Restore recommended safe defaults").performScrollTo().performClick()
        ui.onNodeWithText("Cancel").performClick()
        ui.runOnIdle { assertEquals(before, ui.activity.settings) }
        ui.onNodeWithText("Restore recommended safe defaults").performScrollTo().performClick()
        ui.onNodeWithText("Confirm").performClick()
        saved()
        val after = SettingsStore(File(ui.activity.filesDir, "settings.json")).load()
        assertEquals(before.pinHash, after.pinHash)
        assertEquals(before.attempts, after.attempts)
        assertEquals(before.requests, after.requests)
        assertEquals(before.nasaVideos, after.nasaVideos)
        assertFalse(after.categories.any { it.enabled })
        assertFalse(after.sources.any { it.enabled })
        assertTrue(after.channels.isEmpty())
        assertTrue(after.nasaReviews.isEmpty())
        assertFalse(after.categories.any { it.id == "custom-v1" })
    }

    @Test fun consentedLiveAnimalSearchArticleReturnAndBackgroundStayControlled() {
        assumeTrue("Requires explicit educationalLive=true; otherwise no live search is performed",
            InstrumentationRegistry.getArguments().getString("educationalLive") == "true")
        ui.runOnIdle {
            ui.activity.update { state -> state.copy(categories = state.categories.map { it.copy(enabled = it.id == "animals") }) }
        }
        saved()
        tab("Websites")
        ui.onNodeWithTag("source-enable-vikidia").performScrollTo().performClick()
        ui.onNodeWithText("Cancel").performClick()
        ui.runOnIdle { assertFalse(ui.activity.settings!!.sources.single { it.id == "vikidia" }.enabled) }
        ui.onNodeWithTag("source-enable-vikidia").performScrollTo().performClick()
        ui.onNodeWithText("Approve and enable").performClick()
        saved()
        ui.runOnIdle { assertTrue(ui.activity.settings!!.sources.single { it.id == "vikidia" }.enabled) }
        ui.onNodeWithText("Lock and return home").performScrollTo().performClick()
        search("animals")
        try {
            ui.waitUntil(60000) { ui.onAllNodesWithTag(ANIMAL_RESULT).fetchSemanticsNodes().isNotEmpty() }
        } catch (failure: AssertionError) {
            throw AssertionError("Live Vikidia search did not return the expected Animal article within 60 seconds. Provider/network changes must be investigated.", failure)
        }
        ui.onNodeWithTag(ANIMAL_RESULT).performScrollTo().performClick()
        ui.waitUntil(60000) {
            ui.runOnIdle {
                findWebView(ui.activity.findViewById(android.R.id.content))?.let {
                    it.visibility == View.VISIBLE && it.progress == 100 &&
                        it.url == "https://en.vikidia.org/wiki/Animal" && it.title?.contains("Animal", ignoreCase = true) == true
                } == true
            }
        }
        capturePublicArticle()
        ui.onNodeWithText("Back to FamilySafe").performClick()
        ui.onNodeWithText(QUERY).assertTextContains("animals")
        ui.onNodeWithText("Reading results").assertExists()
        ui.onNodeWithTag(ANIMAL_RESULT).assertExists()
        search("animals")
        ui.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        ui.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        ui.onNodeWithText("Reading results").assertDoesNotExist()
        ui.onNodeWithTag(ANIMAL_RESULT).assertDoesNotExist()
        ui.onNodeWithText("Searching your approved sources…").assertDoesNotExist()
        ui.runOnIdle { assertFalse(ui.activity.parent); assertNull(ui.activity.nasaPlayback); assertTrue(ui.activity.settings!!.nasaReviews.isEmpty()) }
    }

    private fun testRule(query: String) {
        ui.onNodeWithTag("rule-test-query").performScrollTo().performTextReplacement(query)
        ui.onNodeWithTag("rule-test-run").performScrollTo().performClick()
        ui.onNodeWithTag("rule-test-query").assertIsNotFocused()
    }
    private fun tab(name: String) {
        ui.onNodeWithText("Parent settings").performScrollTo().assertIsDisplayed()
        ui.onNodeWithText(name).performScrollTo().assertIsDisplayed().performClick()
        ui.runOnIdle { assertEquals("Parent tab click did not select its destination", name, ui.activity.parentTab) }
    }
    private fun saved() = ui.waitUntil(15000) { !ui.activity.busy }
    private fun search(query: String) {
        ui.onNodeWithText(QUERY).performScrollTo().performTextReplacement(query)
        ui.onNodeWithText("Explore").performScrollTo().performClick()
    }
    private fun findWebView(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) for (index in 0 until view.childCount) {
            findWebView(view.getChildAt(index))?.let { return it }
        }
        return null
    }
    private fun capturePublicArticle() {
        requireDisposableEmulator()
        ui.runOnIdle {
            check(!ui.activity.parent && !ui.activity.gate && ui.activity.nasaPlayback == null)
            check(findWebView(ui.activity.findViewById(android.R.id.content))?.url == "https://en.vikidia.org/wiki/Animal")
        }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ui.runOnIdle { ui.activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
        try {
            instrumentation.waitForIdleSync()
            android.os.SystemClock.sleep(250)
            val screenshot = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
            try {
                val directory = requireNotNull(instrumentation.targetContext.getExternalFilesDir("evidence")).apply { mkdirs() }
                File(directory, "v1-animal.png").outputStream().use {
                    check(screenshot.compress(Bitmap.CompressFormat.PNG, 100, it))
                }
            } finally { screenshot.recycle() }
        } finally { ui.runOnIdle { ui.activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE) } }
    }
    private fun requireDisposableEmulator() {
        check(InstrumentationRegistry.getArguments().getString("disposableInstrumentation") == "true") {
            "Requires explicit disposableInstrumentation=true before resetting any settings"
        }
        check(BuildConfig.DEBUG && InstrumentationRegistry.getInstrumentation().targetContext.packageName == "family.safe.debug")
        check(Build.HARDWARE in setOf("ranchu", "goldfish")) { "Requires a dedicated ranchu/goldfish disposable emulator" }
    }
    companion object {
        private const val PIN = "732918"
        private const val QUERY = "What would you like to learn?"
        private const val ANIMAL_RESULT = "read-vikidia-Animal"
    }
}
