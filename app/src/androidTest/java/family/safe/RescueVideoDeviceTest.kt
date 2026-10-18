package family.safe

import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.VideoView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import family.safe.data.SettingsStore
import family.safe.video.archive.ArchiveLesson
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import java.io.File
import java.security.MessageDigest
import kotlin.math.abs

/** Disposable engineering consent only; never represents an adult content/credits/rights review. */
class RescueVideoDeviceTest {
    private val ui = createAndroidComposeRule<MainActivity>()
    private val snapshot = requireNotNull(ArchiveLesson.find(FRACTIONS_ID))
    private val fresh = object : ExternalResource() {
        override fun before() {
            assumeTrue("Requires explicit rescueVideoLive=true", InstrumentationRegistry.getArguments().getString("rescueVideoLive") == "true")
            requireDisposableEmulator()
            val file = File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "settings.json")
            check(!file.exists() || file.delete()) { "Could not reset disposable video fixture settings" }
        }
    }
    @get:Rule val rules: RuleChain = RuleChain.outerRule(fresh).around(ui)

    @Before fun createDisposableMathFixture() {
        ui.waitUntil(15000) { ui.activity.settings != null }
        ui.onNodeWithText("Parent PIN").performTextInput(PIN)
        ui.onNodeWithText("Confirm PIN").performTextInput(PIN)
        ui.onNodeWithText("Create PIN").performScrollTo().performClick()
        ui.waitUntil(60000) { ui.activity.parent && !ui.activity.busy }
        ui.runOnIdle { ui.activity.update { state -> state.copy(
            categories = state.categories.map { it.copy(enabled = it.id == "math") },
            archiveCategories = setOf("math"), nasaVideos = emptyList(), nasaReviews = emptyList()
        ) } }
        saved()
    }

    @Test fun fractionsDiscoveryActualPreviewSimulatedConsentChildControlsAndRevocation() {
        ui.runOnIdle { ui.activity.discoverArchive("math", "fractions") }
        ui.waitUntil(75000) { !ui.activity.busy }
        ui.runOnIdle {
            assertTrue("Production discovery did not persist the known fractions candidate: ${ui.activity.message}",
                ui.activity.settings!!.nasaVideos.any { it.id == FRACTIONS_ID && it.title == snapshot.title })
            assertTrue(ui.activity.settings!!.nasaReviews.isEmpty())
        }
        lock()
        ui.onNodeWithTag("discover-watch").assertDoesNotExist()
        ui.onNodeWithTag(PLAY).assertDoesNotExist()
        ui.runOnIdle { ui.activity.openNasa(FRACTIONS_ID); assertNull(ui.activity.nasaPlayback) }
        unlock()
        videos()
        ui.onNodeWithTag("nasa-preview-$FRACTIONS_ID").performScrollTo().performClick()
        awaitNative()
        ui.runOnIdle {
            assertEquals(true, ui.activity.nasaPlayback?.preview)
            assertEquals(snapshot.sha256, ui.activity.nasaPreviewHashes[FRACTIONS_ID])
            assertTrue(ui.activity.settings!!.nasaReviews.isEmpty())
        }
        assertPrivateVerifiedSnapshot()
        val previewTiming = timing()
        capturePlayer("rescue-fractions-parent-preview")
        ui.onNodeWithText("Close video").performClick()
        awaitCleanup()
        // Deliberately simulated consent after actual native preview; no full human review is claimed.
        android.util.Log.i("FamilySafeRescueVideoTest", "SIMULATED ENGINEERING CONSENT ONLY: no adult content, credits or rights review is established")
        ui.runOnIdle { ui.activity.reviewNasa(FRACTIONS_ID, approved = true, confirmed = true) }
        saved()
        assertEquals(snapshot.sha256, SettingsStore(File(ui.activity.filesDir, "settings.json")).load().nasaReviews.single().sha256)
        lock()
        ui.onNodeWithTag("discover-watch").assertIsDisplayed().performClick()
        ui.onNodeWithTag("discover-content").performScrollToNode(hasTestTag(PLAY))
        ui.onNodeWithTag(PLAY).assertIsDisplayed().performClick()
        awaitNative()
        assertPrivateVerifiedSnapshot()
        val childTiming = timing()
        val player = ui.runOnIdle { requireNotNull(nativeVideo()) }
        var pausedAt = 0
        var duration = 0
        ui.runOnIdle {
            assertEquals(false, ui.activity.nasaPlayback?.preview)
            duration = player.duration
            player.pause()
            pausedAt = player.currentPosition
        }
        SystemClock.sleep(700)
        ui.runOnIdle { assertFalse(player.isPlaying); assertTrue(abs(player.currentPosition - pausedAt) < 300) }
        val seekTarget = duration / 2
        ui.runOnIdle { player.seekTo(seekTarget) }
        ui.waitUntil(15000) { ui.runOnIdle { !player.isPlaying && abs(player.currentPosition - seekTarget) < 4000 } }
        ui.runOnIdle { player.start() }
        ui.waitUntil(15000) { ui.runOnIdle { player.isPlaying && player.currentPosition > seekTarget + 300 } }
        capturePlayer("rescue-fractions-child-player")
        ui.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        ui.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        awaitCleanup()
        ui.runOnIdle {
            assertFalse(player.isPlaying)
            assertNull(ui.activity.nasaPlayback)
            assertFalse(ui.activity.parent)
            assertTrue(ui.activity.nasaPreviewHashes.isEmpty())
        }
        unlock()
        ui.runOnIdle { ui.activity.reviewNasa(FRACTIONS_ID, approved = false) }
        saved(); lock()
        ui.onNodeWithTag("discover-watch").assertDoesNotExist()
        ui.runOnIdle { ui.activity.openNasa(FRACTIONS_ID); assertNull(ui.activity.nasaPlayback) }
        unlock()
        ui.runOnIdle { ui.activity.update { it.copy(archiveCategories = emptySet()) } }
        saved()
        ui.runOnIdle {
            ui.activity.openNasa(FRACTIONS_ID, preview = true)
            assertNull(ui.activity.nasaPlayback)
        }
        lock()
        ui.onNodeWithTag("discover-watch").assertDoesNotExist()
        evidence("rescue-fractions-video-result.json").writeText(JSONObject()
            .put("videoId", snapshot.videoId).put("bytes", snapshot.bytes).put("sha256", snapshot.sha256)
            .put("clock", "monotonic").put("previewMs", previewTiming).put("childMs", childTiming)
            .put("durationMs", duration).put("pausedPositionMs", pausedAt).put("seekTargetMs", seekTarget)
            .put("nativePreviewAndChildControls", "PASS").put("backgroundCleanupAndRevocation", "PASS")
            .put("simulatedEngineeringConsent", true).put("humanContentRightsReview", false).toString(2))
    }

    private fun timing(): JSONObject {
        val values = ui.runOnIdle { ui.activity.performance.toMap() }
        listOf("video_transfer_ms", "video_prepare_ms", "video_total_ms").forEach { key ->
            assertTrue("Missing monotonic playback timing: $key", values[key]?.let { it >= 0 } == true)
        }
        return JSONObject(values)
    }
    private fun assertPrivateVerifiedSnapshot() {
        val file = ui.activity.cacheDir.listFiles().orEmpty().single { it.name.startsWith("nasa-player-") }
        assertEquals(snapshot.bytes, file.length())
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
        }
        assertEquals(snapshot.sha256, digest.digest().joinToString("") { "%02x".format(it) })
    }
    private fun nativeVideo(): VideoView? = ui.activity.findViewById<View>(android.R.id.content).findViewWithTag("nasaVideo")
    private fun awaitNative() = ui.waitUntil(180000) {
        ui.runOnIdle {
            check(ui.activity.nasaPlayback?.id == FRACTIONS_ID) { "Selected fractions player closed while waiting" }
            val status = ui.activity.findViewById<View>(android.R.id.content).findViewWithTag<TextView>("nasaStatus")?.text?.toString().orEmpty()
            check(!status.startsWith("Video unavailable") && !status.startsWith("Playback failed")) { "Real fractions playback failed: $status" }
            nativeVideo()?.let { it.isPlaying && it.duration > 0 && it.currentPosition > 500 } == true
        }
    }
    private fun awaitCleanup() = ui.waitUntil(20000) { ui.activity.cacheDir.listFiles().orEmpty().none { it.name.startsWith("nasa-player-") } }
    private fun saved() = ui.waitUntil(15000) { !ui.activity.busy }
    private fun lock() = ui.onNodeWithText("Lock and return home").performScrollTo().performClick()
    private fun unlock() {
        ui.onNodeWithTag("parent-entry").assertIsDisplayed().performClick()
        ui.onNodeWithText("Parent PIN").performTextInput(PIN)
        ui.onNodeWithText("Unlock").performScrollTo().performClick()
        ui.waitUntil(60000) { ui.activity.parent && !ui.activity.busy }
    }
    private fun videos() = ui.onNodeWithText("Videos").performScrollTo().performClick()
    private fun evidence(name: String): File = File(requireNotNull(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir("evidence")).apply { mkdirs() }, name)
    private fun capturePlayer(name: String) {
        requireDisposableEmulator()
        ui.runOnIdle { check(!ui.activity.gate && ui.activity.nasaPlayback?.id == FRACTIONS_ID); ui.activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        try {
            instrumentation.waitForIdleSync(); SystemClock.sleep(250)
            val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
            try { evidence("$name.png").outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) } }
            finally { bitmap.recycle() }
        } finally { ui.runOnIdle { ui.activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE) } }
    }
    private fun requireDisposableEmulator() {
        check(InstrumentationRegistry.getArguments().getString("disposableInstrumentation") == "true") { "Requires explicit disposableInstrumentation=true before reset/capture" }
        check(BuildConfig.BUILD_TYPE == "debug" && InstrumentationRegistry.getInstrumentation().targetContext.packageName == "family.safe.debug")
        check(Build.HARDWARE in setOf("ranchu", "goldfish")) { "Requires a dedicated disposable ranchu/goldfish emulator" }
    }
    companion object {
        private const val PIN = "732918"
        private const val FRACTIONS_ID = "archive:KA-converted-WPimvspI0_c"
        private const val PLAY = "nasa-play-$FRACTIONS_ID"
    }
}
