package family.safe

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.os.ParcelFileDescriptor
import android.view.View
import android.view.WindowManager
import android.widget.VideoView
import android.widget.TextView
import android.widget.Button as NativeButton
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import family.safe.data.SettingsStore
import family.safe.video.nasa.NasaCatalog
import family.safe.video.nasa.NasaClient
import family.safe.video.nasa.NasaResponse
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import java.io.File
import kotlin.math.abs

/** Deletes settings only in an explicitly disposable debug installation. */
class NasaReviewFlowTest {
    private val ui = createAndroidComposeRule<MainActivity>()
    private val freshSettings = object : ExternalResource() {
        override fun before() {
            check(InstrumentationRegistry.getArguments().getString("disposableInstrumentation") == "true") {
                "Requires -e disposableInstrumentation true on a disposable emulator."
            }
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            check(BuildConfig.BUILD_TYPE == "debug" && context.packageName == "family.safe.debug")
            check(Build.HARDWARE in setOf("ranchu", "goldfish"))
            val settings = File(context.filesDir, "settings.json")
            check(!settings.exists() || settings.delete())
        }
    }
    @get:Rule val rules: RuleChain = RuleChain.outerRule(freshSettings).around(ui)
    private val candidate get() = NasaCatalog.videos.first()

    @Before fun createParentPin() {
        loaded()
        ui.onNodeWithText("Parent PIN").performTextInput(PIN)
        ui.onNodeWithText("Confirm PIN").performTextInput(PIN)
        ui.onNodeWithText("Create PIN").performScrollTo().performClick()
        ui.waitUntil(60000) { ui.activity.parent && !ui.activity.busy }
    }

    @Test fun pendingApprovalAndRejectionPersistThroughTheRealParentFlow() {
        videos()
        ui.onNodeWithText("NASA parent discovery").assertExists()
        ui.onNodeWithText("Enable a topic in Topics before searching NASA.").assertExists()
        ui.runOnIdle {
            ui.activity.reviewNasa(candidate.id, approved = true, confirmed = true)
            assertTrue(ui.activity.settings!!.nasaReviews.isEmpty())
        }
        ui.onNodeWithText("Topics").performScrollTo().performClick()
        ui.onNode(isToggleable() and hasAnySibling(hasText("Space"))).performScrollTo().performClick()
        saved()
        ui.onNode(isToggleable() and hasAnySibling(hasText("Science"))).performScrollTo().performClick()
        saved()
        videos()
        ui.onNodeWithText(candidate.title).assertExists()
        ui.onNodeWithTag("nasa-approve-${candidate.id}").assertIsNotEnabled()
        ui.onNodeWithTag("nasa-preview-${candidate.id}").assertIsEnabled()
        lock()
        assertChildCannotSeeCandidate()
        unlock()
        videos()
        ui.onNodeWithTag("nasa-confirm-${candidate.id}").performScrollTo().performClick()
        ui.onNodeWithTag("nasa-approve-${candidate.id}").assertIsNotEnabled()
        recordFixturePreview()
        ui.onNodeWithTag("nasa-approve-${candidate.id}").performScrollTo().performClick()
        saved()
        ui.runOnIdle { assertTrue(ui.activity.settings!!.nasaReviews.single().approved) }
        ui.activityRule.scenario.recreate()
        loaded()
        ui.runOnIdle { assertFalse(ui.activity.parent) }
        watch()
        childVideo().assertIsDisplayed()
        ui.onNodeWithTag("discover-query").performTextReplacement("science")
        ui.onNodeWithTag("discover-search").performClick()
        ui.onNodeWithTag("nasa-play-${candidate.id}").assertDoesNotExist()
        ui.onNodeWithText("Choose an approved Space video below.").assertDoesNotExist()
        ui.onNodeWithTag("discover-query").performTextReplacement("space")
        ui.onNodeWithTag("discover-search").performClick()
        childVideo().assertIsDisplayed()
        assertTrue(SettingsStore(File(ui.activity.filesDir, "settings.json")).load().nasaReviews.single().approved)
        unlock()
        videos()
        ui.onNodeWithTag("nasa-reject-${candidate.id}").performScrollTo().performClick()
        saved()
        ui.activityRule.scenario.recreate()
        loaded()
        assertChildCannotSeeCandidate()
        assertFalse(SettingsStore(File(ui.activity.filesDir, "settings.json")).load().nasaReviews.single().approved)
    }

    @Test fun dynamicCandidatesStayPrivateAndLateSearchCannotCrossParentLock() {
        val id = "JPL-FIXTURE-DISCOVERY"
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val client = NasaClient(enabled=true,assetProbe={true}) { url ->
            if (url.contains("/search?")) {
                entered.countDown()
                check(release.await(10,TimeUnit.SECONDS))
                NasaResponse(200,"""{"collection":{"items":[{"data":[{"center":"JPL","media_type":"video","nasa_id":"$id","title":"Planet lesson","description":"An educational animation","photographer":"NASA/JPL-Caltech"}]}]}}""")
            } else NasaResponse(200,"""{"collection":{"items":[{"href":"https://images-assets.nasa.gov/video/$id/$id~mobile.mp4"}]}}""")
        }
        ui.runOnIdle { ui.activity.update { state -> state.copy(categories=state.categories.map { it.copy(enabled=it.id=="space") }) } }
        saved()
        try {
            ui.runOnIdle { ui.activity.discoverNasa("space","space",client) }
            assertTrue(entered.await(5,TimeUnit.SECONDS))
            ui.runOnIdle { ui.activity.lock() }
        } finally { release.countDown() }
        saved()
        ui.runOnIdle {
            assertFalse(ui.activity.parent)
            assertFalse(ui.activity.settings!!.nasaVideos.any { it.id == id })
            assertTrue(ui.activity.settings!!.nasaReviews.isEmpty())
            ui.activity.discoverNasa("space","space",client)
            assertFalse(ui.activity.busy)
        }
        unlock()
        ui.runOnIdle { ui.activity.discoverNasa("space","space",client) }
        saved()
        assertTrue(SettingsStore(File(ui.activity.filesDir,"settings.json")).load().nasaVideos.any { it.id == id })
        ui.runOnIdle { ui.activity.lock() }
        ui.onNodeWithTag("nasa-play-$id").assertDoesNotExist()
        ui.onNodeWithText("Planet lesson").assertDoesNotExist()
    }

    @Test fun liveDiscoveryPersistsDeduplicatedPendingCandidatesAndReleasesPreview() {
        assumeTrue("Opt in with -e nasaLive true", InstrumentationRegistry.getArguments().getString("nasaLive") == "true")
        check(Build.HARDWARE in setOf("ranchu", "goldfish")) { "Live discovery requires a disposable emulator" }
        ui.onNode(isToggleable() and hasAnySibling(hasText("Space"))).performScrollTo().performClick()
        saved()
        val initialIds = ui.runOnIdle { ui.activity.settings!!.nasaVideos.map { it.id }.toSet() }
        videos()
        ui.onNodeWithTag("nasa-category-space").performScrollTo().performClick()
        ui.onNodeWithTag("nasa-search-query").performScrollTo().performTextReplacement("solar system")
        ui.onNodeWithTag("nasa-discover").performScrollTo().assertIsEnabled().performClick()
        ui.waitUntil(75000) { !ui.activity.busy }
        val discovered = ui.runOnIdle {
            val state = ui.activity.settings!!
            val additions = state.nasaVideos.filter { it.id !in initialIds }
            assertTrue("Live NASA discovery returned no new candidates: ${ui.activity.message}", additions.isNotEmpty())
            assertTrue("Live discovery exceeded five new candidates", additions.size <= 5)
            assertTrue(additions.all { it.category == "space" })
            assertTrue(state.nasaReviews.isEmpty())
            additions
        }
        val firstCollection = ui.runOnIdle { ui.activity.settings!!.nasaVideos }
        ui.onNodeWithTag("nasa-discover").performScrollTo().performClick()
        ui.waitUntil(75000) { !ui.activity.busy }
        ui.runOnIdle {
            assertTrue("Repeated live discovery failed: ${ui.activity.message}", ui.activity.message.contains("new candidates saved"))
            assertEquals(firstCollection, ui.activity.settings!!.nasaVideos)
            assertTrue(ui.activity.settings!!.nasaReviews.isEmpty())
        }
        ui.activityRule.scenario.recreate()
        loaded()
        val persisted = SettingsStore(File(ui.activity.filesDir, "settings.json")).load()
        assertEquals(firstCollection, persisted.nasaVideos)
        assertTrue(persisted.nasaReviews.isEmpty())
        ui.runOnIdle {
            assertFalse(ui.activity.parent)
            discovered.forEach { ui.activity.openNasa(it.id); assertNull(ui.activity.nasaPlayback) }
        }
        discovered.forEach { ui.onNodeWithTag("nasa-play-${it.id}").assertDoesNotExist() }
        unlock()
        val preview = discovered.firstOrNull { it.id == "JPL-20230929-PSYCHEf-0002-Imager" } ?: discovered.first()
        ui.runOnIdle { ui.activity.openNasa(preview.id, preview = true) }
        awaitNative(180000) { it.isPlaying && it.duration > 0 && it.currentPosition > 300 }
        val player = ui.runOnIdle {
            assertTrue(ui.activity.nasaPreviewHashes.containsKey(preview.id))
            assertTrue(ui.activity.settings!!.nasaReviews.isEmpty())
            requireNotNull(nativeVideo())
        }
        ui.onNodeWithText("Close video").performClick()
        ui.runOnIdle { assertNull(ui.activity.nasaPlayback); assertFalse(player.isPlaying) }
        ui.waitUntil(20000) { ui.activity.cacheDir.listFiles()?.none { it.name.startsWith("nasa-player-") } == true }
        ui.runOnIdle { ui.activity.openNasa(preview.id, preview = true) }
        ui.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        ui.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        ui.runOnIdle {
            assertFalse(ui.activity.parent)
            assertNull(ui.activity.nasaPlayback)
            assertTrue(ui.activity.nasaPreviewHashes.isEmpty())
            assertTrue(ui.activity.settings!!.nasaReviews.isEmpty())
            ui.activity.openNasa(preview.id)
            assertNull(ui.activity.nasaPlayback)
        }
        ui.waitUntil(140000) { ui.activity.cacheDir.listFiles()?.none { it.name.startsWith("nasa-player-") } == true }
        discovered.forEach { ui.onNodeWithTag("nasa-play-${it.id}").assertDoesNotExist() }
        assertTrue(SettingsStore(File(ui.activity.filesDir, "settings.json")).load().nasaReviews.isEmpty())
    }

    @Test fun directCallsCannotBypassPinConfirmationOrTopicEligibility() {
        ui.runOnIdle {
            ui.activity.update { state -> state.copy(categories = state.categories.map { if (it.id == "space") it.copy(enabled = true) else it }) }
        }
        saved()
        ui.runOnIdle {
            ui.activity.reviewNasa(candidate.id, approved = true)
            ui.activity.reviewNasa(candidate.id, approved = true, confirmed = true)
            ui.activity.reviewNasa("unlisted-nasa-id", approved = true, confirmed = true)
            assertTrue(ui.activity.settings!!.nasaReviews.isEmpty())
            ui.activity.lock()
            ui.activity.reviewNasa(candidate.id, approved = true, confirmed = true)
            ui.activity.reviewNasa(candidate.id, approved = false)
            ui.activity.openNasa(candidate.id, preview = true)
            ui.activity.openNasa(candidate.id)
            assertNull(ui.activity.nasaPlayback)
            assertTrue(ui.activity.settings!!.nasaReviews.isEmpty())
        }
        unlock()
        recordFixturePreview()
        ui.runOnIdle { ui.activity.reviewNasa(candidate.id, approved = true, confirmed = true) }
        saved()
        ui.runOnIdle {
            ui.activity.update { state -> state.copy(categories = state.categories.map { if (it.id == "space") it.copy(enabled = false) else it }) }
        }
        saved()
        lock()
        ui.runOnIdle {
            ui.activity.openNasa(candidate.id)
            assertNull(ui.activity.nasaPlayback)
        }
        assertChildCannotSeeCandidate()
        unlock()
        ui.runOnIdle {
            ui.activity.update { state -> state.copy(categories = state.categories.filterNot { it.id == "space" }) }
        }
        saved()
        videos()
        ui.onNodeWithText("Restore Space topic (disabled)").performScrollTo().performClick()
        saved()
        ui.runOnIdle {
            assertFalse(ui.activity.settings!!.categories.single { it.id == "space" }.enabled)
            assertTrue(ui.activity.settings!!.nasaReviews.single().approved)
        }
        lock()
        ui.runOnIdle { ui.activity.openNasa(candidate.id); assertNull(ui.activity.nasaPlayback) }
        assertChildCannotSeeCandidate()
    }

    @Test fun previewSelectionCannotSurviveLockRecreationOrBackground() {
        ui.runOnIdle {
            ui.activity.update { state -> state.copy(categories = state.categories.map { if (it.id == "space") it.copy(enabled = true) else it }) }
        }
        saved()
        // Clear before Compose renders, so this deterministic test never starts a download.
        ui.runOnIdle {
            ui.activity.openNasa(candidate.id, preview = true)
            assertEquals(candidate.id, ui.activity.nasaPlayback?.id)
            assertEquals(true, ui.activity.nasaPlayback?.preview)
            ui.activity.recordNasaPreview(candidate.id, FIXTURE_HASH)
            assertEquals(FIXTURE_HASH, ui.activity.nasaPreviewHashes[candidate.id])
            ui.activity.lock()
            assertNull(ui.activity.nasaPlayback)
            assertTrue(ui.activity.nasaPreviewHashes.isEmpty())
            ui.activity.recordNasaPreview(candidate.id, FIXTURE_HASH)
            assertTrue(ui.activity.nasaPreviewHashes.isEmpty())
        }
        ui.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        ui.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        ui.runOnIdle {
            assertFalse(ui.activity.parent)
            assertNull(ui.activity.nasaPlayback)
            ui.activity.openNasa(candidate.id, preview = true)
            assertNull(ui.activity.nasaPlayback)
        }
        ui.activityRule.scenario.recreate()
        loaded()
        ui.runOnIdle { assertFalse(ui.activity.parent); assertNull(ui.activity.nasaPlayback) }
    }

    @Test fun rotationRelocksParentAndKeepsCollectionControlsReachable() {
        val orientations = listOf(
            Triple(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, Configuration.ORIENTATION_PORTRAIT, "portrait"),
            Triple(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, Configuration.ORIENTATION_LANDSCAPE, "landscape")
        )
        val order = if (ui.activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) orientations.reversed() else orientations
        for ((orientation, expected, name) in order) {
            ui.runOnIdle { ui.activity.requestedOrientation = orientation }
            ui.waitUntil(15000) { ui.activity.resources.configuration.orientation == expected && ui.activity.settings != null }
            ui.waitForIdle()
            // An orientation change recreates this activity and never restores parent access.
            ui.runOnIdle { assertFalse(ui.activity.parent) }
            ui.onNodeWithTag("discover-query").assertIsDisplayed()
            captureLayout("nasa-child-$name")
            unlock()
            videos()
            ui.onNodeWithText("Parent review collection (", substring = true).performScrollTo().assertIsDisplayed()
            captureLayout("nasa-parent-$name")
        }
    }

    @Test fun liveMainActivityPreviewAndApprovedChildPlaybackReleaseOnBackground() {
        assumeTrue("Opt in with -e nasaLive true", InstrumentationRegistry.getArguments().getString("nasaLive") == "true")
        ui.runOnIdle {
            ui.activity.update { state -> state.copy(categories = state.categories.map { if (it.id == "space") it.copy(enabled = true) else it }) }
        }
        saved()
        ui.runOnIdle { ui.activity.openNasa(candidate.id, preview = true) }
        awaitNative(180000) { it.isPlaying && it.duration > 0 }
        ui.runOnIdle { assertEquals(true, ui.activity.nasaPlayback?.preview) }
        captureLayout("nasa-parent-preview")
        ui.runOnIdle { ui.activity.closeNasa() }
        NasaCatalog.videos.drop(1).forEachIndexed { index, other ->
            ui.runOnIdle { ui.activity.openNasa(other.id, preview = true) }
            awaitNative(180000) { it.isPlaying && it.duration > 0 }
            ui.runOnIdle { assertTrue(ui.activity.nasaPreviewHashes.containsKey(other.id)) }
            captureLayout("nasa-parent-preview-${index + 2}")
            ui.runOnIdle { ui.activity.closeNasa() }
        }
        videos()
        ui.onNodeWithTag("nasa-confirm-${candidate.id}").performScrollTo().performClick()
        ui.onNodeWithTag("nasa-approve-${candidate.id}").performScrollTo().performClick()
        saved()
        lock()
        watch()
        childVideo().performClick()
        awaitNative(180000) { it.isPlaying && it.duration > 0 }
        var duration = 0
        var pausedAt = 0
        lateinit var player: VideoView
        ui.runOnIdle {
            assertEquals(false, ui.activity.nasaPlayback?.preview)
            player = requireNotNull(nativeVideo())
            duration = player.duration
            player.pause()
            pausedAt = player.currentPosition
        }
        android.os.SystemClock.sleep(700)
        ui.runOnIdle {
            assertFalse(player.isPlaying)
            assertTrue(abs(player.currentPosition - pausedAt) < 300)
            player.seekTo(duration / 2)
        }
        awaitNative(15000) { abs(it.currentPosition - duration / 2) < 4000 }
        ui.runOnIdle { player.start() }
        awaitNative(15000) { it.isPlaying && it.currentPosition > duration / 2 + 300 }
        captureLayout("nasa-approved-child-player")
        ui.runOnIdle { player.seekTo((duration - 600).coerceAtLeast(0)); player.start() }
        awaitNative(15000) { !it.isPlaying && it.currentPosition >= duration - 1500 }
        ui.runOnIdle { player.seekTo(0); player.start() }
        awaitNative(15000) { it.isPlaying }
        ui.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        ui.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        ui.runOnIdle {
            assertFalse(ui.activity.parent)
            assertNull(ui.activity.nasaPlayback)
            assertFalse(player.isPlaying)
        }
        watch()
        childVideo().assertIsDisplayed()
    }

    @Test fun liveOfflinePreviewFailsClosedAndRetriesOnlyTheSameVideo() {
        assumeTrue("Opt in with -e nasaLive true", InstrumentationRegistry.getArguments().getString("nasaLive") == "true")
        assumeTrue("Network mutation requires the disposable emulator", Build.HARDWARE in setOf("ranchu", "goldfish"))
        ui.runOnIdle {
            ui.activity.update { state -> state.copy(categories = state.categories.map { if (it.id == "space") it.copy(enabled = true) else it }) }
        }
        saved()
        val originalMobileData = emulatorShell("settings get global mobile_data").trim()
        check(originalMobileData in setOf("0", "1")) { "Cannot safely restore emulator mobile data state" }
        val wifiStatus = emulatorShell("cmd wifi status").lineSequence().map(String::trim).toList()
        val originalWifi = when {
            "Wifi is enabled" in wifiStatus -> true
            "Wifi is disabled" in wifiStatus -> false
            else -> error("Cannot safely restore emulator Wi-Fi state")
        }
        try {
            setMobileData(false)
            setWifi(false)
            ui.waitUntil(15000) { !hasDefaultNetwork() }
            ui.runOnIdle { ui.activity.openNasa(candidate.id, preview = true) }
            ui.waitUntil(60000) {
                ui.runOnIdle {
                    ui.activity.findViewById<View>(android.R.id.content)
                        .findViewWithTag<TextView>("nasaStatus")?.text?.startsWith("Video unavailable or changed.") == true
                }
            }
            ui.runOnIdle {
                assertFalse(requireNotNull(nativeVideo()).isPlaying)
                assertTrue(ui.activity.nasaPreviewHashes.isEmpty())
                assertEquals(candidate.id, ui.activity.nasaPlayback?.id)
                assertEquals(MainActivity::class.java.name, ui.activity.componentName.className)
                assertEquals(Lifecycle.State.RESUMED, ui.activity.lifecycle.currentState)
            }
            captureLayout("nasa-offline-denied")
            setWifi(true)
            ui.waitUntil(15000) { hasDefaultNetwork() }
            ui.runOnIdle {
                ui.activity.findViewById<View>(android.R.id.content)
                    .findViewWithTag<NativeButton>("nasaRetry").performClick()
            }
            awaitNative(180000) { it.isPlaying && it.duration > 0 }
            ui.runOnIdle {
                assertEquals(candidate.id, ui.activity.nasaPlayback?.id)
                assertTrue(ui.activity.nasaPreviewHashes[candidate.id]?.matches(Regex("[a-f0-9]{64}")) == true)
                assertTrue(ui.activity.settings!!.nasaReviews.isEmpty())
            }
            ui.onNodeWithText("Close video").performClick()
            ui.runOnIdle { assertNull(ui.activity.nasaPlayback) }
            ui.waitUntil(20000) {
                ui.activity.cacheDir.listFiles()?.none { it.name.startsWith("nasa-player-") } == true
            }
        } finally {
            try { setMobileData(originalMobileData == "1") }
            finally { setWifi(originalWifi) }
        }
    }

    private fun setMobileData(enabled: Boolean) {
        emulatorShell(if (enabled) "svc data enable" else "svc data disable")
    }

    private fun setWifi(enabled: Boolean) {
        val command = if (enabled) "svc wifi enable" else "svc wifi disable"
        emulatorShell(command)
    }

    private fun emulatorShell(command: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
    }

    private fun hasDefaultNetwork(): Boolean = emulatorShell("dumpsys connectivity").lineSequence()
        .any { it.trim().matches(Regex("Active default network: [0-9]+")) }

    private fun nativeVideo(): VideoView? =
        ui.activity.findViewById<View>(android.R.id.content).findViewWithTag("nasaVideo")

    private fun awaitNative(timeout: Long, condition: (VideoView) -> Boolean) = ui.waitUntil(timeout) {
        ui.runOnIdle {
            val selection = requireNotNull(ui.activity.nasaPlayback) { "NASA playback selection closed while waiting" }
            val status = ui.activity.findViewById<View>(android.R.id.content)
                .findViewWithTag<TextView>("nasaStatus")?.text?.toString().orEmpty()
            check(!status.startsWith("Video unavailable or changed.") && !status.startsWith("Playback failed.")) {
                "NASA playback failed for ${selection.id}: $status"
            }
            nativeVideo()?.let(condition) == true
        }
    }

    private fun captureLayout(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ui.runOnIdle { ui.activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
        try {
            instrumentation.waitForIdleSync()
            android.os.SystemClock.sleep(250)
            val screenshot = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
            try {
                val directory = requireNotNull(instrumentation.targetContext.getExternalFilesDir("evidence")).apply { mkdirs() }
                File(directory, "$name.png").outputStream().use { check(screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            } finally { screenshot.recycle() }
        } finally { ui.runOnIdle { ui.activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE) } }
    }

    private fun loaded() = ui.waitUntil(15000) { ui.activity.settings != null }
    private fun saved() = ui.waitUntil(15000) { !ui.activity.busy }
    private fun recordFixturePreview() {
        // Fixture for consent/persistence tests; the opt-in live test proves real media preparation.
        ui.runOnIdle {
            ui.activity.openNasa(candidate.id, preview = true)
            ui.activity.recordNasaPreview(candidate.id, FIXTURE_HASH)
            ui.activity.closeNasa()
        }
    }
    private fun videos() {
        ui.onNodeWithText("Parent settings").performScrollTo().assertIsDisplayed()
        ui.onNodeWithText("Videos").performScrollTo().assertIsDisplayed().performClick()
        ui.runOnIdle { assertEquals("Videos", ui.activity.parentTab) }
    }
    private fun lock() = ui.onNodeWithText("Lock and return home").performScrollTo().performClick()
    private fun assertChildCannotSeeCandidate() {
        ui.runOnIdle { assertFalse(family.safe.video.nasa.approvedNasaVideos(ui.activity.settings!!).any { it.id == candidate.id }) }
        ui.onNodeWithTag("nasa-play-${candidate.id}").assertDoesNotExist()
        ui.onNodeWithText(candidate.title).assertDoesNotExist()
    }
    private fun unlock() {
        ui.onNodeWithTag("parent-entry").assertIsDisplayed().performClick()
        ui.onNodeWithText("Parent PIN").performTextInput(PIN)
        ui.onNodeWithText("Unlock").performScrollTo().performClick()
        ui.waitUntil(60000) { ui.activity.parent && !ui.activity.busy }
    }
    private fun watch() = ui.onNodeWithTag("discover-watch").assertIsDisplayed().performClick()
    private fun childVideo(): SemanticsNodeInteraction {
        val tag = "nasa-play-${candidate.id}"
        ui.onNodeWithTag("discover-content").performScrollToNode(hasTestTag(tag))
        return ui.onNodeWithTag(tag)
    }
    companion object {
        private const val PIN = "732918"
        private val FIXTURE_HASH = "a".repeat(64)
    }
}
