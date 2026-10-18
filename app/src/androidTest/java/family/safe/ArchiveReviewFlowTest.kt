package family.safe

import android.os.Build
import android.view.View
import android.widget.TextView
import android.widget.VideoView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import family.safe.data.SettingsStore
import family.safe.video.archive.ArchiveClient
import family.safe.video.archive.ArchiveLesson
import family.safe.video.nasa.NasaResponse
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Disposable engineering fixtures never substitute for a parent's full content/rights review. */
class ArchiveReviewFlowTest {
    private val ui = createAndroidComposeRule<MainActivity>()
    private val freshSettings = object : ExternalResource() {
        override fun before() {
            check(InstrumentationRegistry.getArguments().getString("disposableInstrumentation") == "true") {
                "Requires disposableInstrumentation=true before deleting debug settings"
            }
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            check(BuildConfig.DEBUG && context.packageName == "family.safe.debug")
            check(Build.HARDWARE in setOf("ranchu", "goldfish")) { "Requires a disposable ranchu/goldfish emulator" }
            val settings = File(context.filesDir, "settings.json")
            check(!settings.exists() || settings.delete())
        }
    }
    @get:Rule val rules: RuleChain = RuleChain.outerRule(freshSettings).around(ui)

    @Before fun createParentPinAndMathFixture() {
        ui.waitUntil(15000) { ui.activity.settings != null }
        ui.onNodeWithText("Parent PIN").performTextInput(PIN)
        ui.onNodeWithText("Confirm PIN").performTextInput(PIN)
        ui.onNodeWithText("Create PIN").performScrollTo().performClick()
        ui.waitUntil(60000) { ui.activity.parent && !ui.activity.busy }
        ui.runOnIdle { ui.activity.update { state -> state.copy(categories = state.categories.map {
            if (it.id == "math") it.copy(enabled = true, terms = it.terms + "addition") else it.copy(enabled = false)
        }) } }
        saved()
    }

    @Test fun liveConsentedDiscoveryPreviewFixtureChildPlaybackAndSourceRevocation() {
        assumeTrue("Requires explicit archiveLive=true", InstrumentationRegistry.getArguments().getString("archiveLive") == "true")
        videos()
        ui.onNodeWithTag("archive-topic-math").performScrollTo().performClick()
        ui.onNodeWithTag("archive-enable").performScrollTo().performClick()
        ui.onNodeWithText("Cancel").performClick()
        ui.runOnIdle { assertTrue(ui.activity.settings!!.archiveCategories.isEmpty()) }
        ui.onNodeWithTag("archive-enable").performScrollTo().performClick()
        ui.onNodeWithText("Enable source").performClick()
        saved()
        ui.runOnIdle { assertEquals(setOf("math"), ui.activity.settings!!.archiveCategories); assertTrue(ui.activity.settings!!.nasaReviews.isEmpty()) }
        ui.onNodeWithTag("archive-search-query").performScrollTo().performTextReplacement("addition")
        ui.onNodeWithTag("archive-discover").performScrollTo().assertIsEnabled().performClick()
        ui.waitUntil(75000) { !ui.activity.busy }
        ui.runOnIdle {
            val lesson = ui.activity.settings!!.nasaVideos.singleOrNull { it.id == ArchiveLesson.VIDEO_ID }
            assertNotNull("Production Archive discovery failed: ${ui.activity.message}", lesson)
            assertEquals("archive", lesson!!.provider)
            assertEquals("math", lesson.category)
            assertTrue(ui.activity.settings!!.nasaReviews.isEmpty())
        }
        ui.onNodeWithTag("nasa-approve-${ArchiveLesson.VIDEO_ID}").assertIsNotEnabled()
        lock()
        ui.onNodeWithTag(PLAY).assertDoesNotExist()
        ui.runOnIdle { ui.activity.openNasa(ArchiveLesson.VIDEO_ID); assertNull(ui.activity.nasaPlayback) }
        unlock()
        videos()
        ui.onNodeWithTag("nasa-preview-${ArchiveLesson.VIDEO_ID}").performScrollTo().performClick()
        awaitNativePlayback()
        ui.runOnIdle {
            assertEquals(HASH, ui.activity.nasaPreviewHashes[ArchiveLesson.VIDEO_ID])
            assertTrue(ui.activity.settings!!.nasaReviews.isEmpty())
        }
        ui.onNodeWithText("Close video").performClick()
        awaitNoPlayerFiles()
        ui.runOnIdle { assertTrue(ui.activity.parent); assertNull(ui.activity.nasaPlayback); assertTrue(ui.activity.settings!!.nasaReviews.isEmpty()) }
        // Engineering consent fixture only: this test has not watched/reviewed the full lesson.
        android.util.Log.i("FamilySafeArchiveTest", "Approving only a disposable engineering fixture; no human content/rights approval is established")
        ui.onNodeWithTag("nasa-confirm-${ArchiveLesson.VIDEO_ID}").performScrollTo().performClick()
        ui.onNodeWithTag("nasa-approve-${ArchiveLesson.VIDEO_ID}").performScrollTo().assertIsEnabled().performClick()
        saved()
        assertEquals(HASH, SettingsStore(File(ui.activity.filesDir, "settings.json")).load().nasaReviews.single().sha256)
        lock()
        ui.onNodeWithTag(PLAY).performScrollTo().performClick()
        awaitNativePlayback()
        val playing = ui.runOnIdle { requireNotNull(nativeVideo()) }
        ui.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        ui.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        awaitNoPlayerFiles()
        ui.runOnIdle {
            assertFalse(playing.isPlaying)
            assertFalse(ui.activity.parent)
            assertNull(ui.activity.nasaPlayback)
            assertTrue(ui.activity.nasaPreviewHashes.isEmpty())
        }
        unlock()
        videos()
        ui.onNodeWithTag("archive-topic-math").performScrollTo().performClick()
        ui.onNodeWithTag("archive-enable").performScrollTo().performClick()
        saved()
        ui.runOnIdle {
            assertTrue(ui.activity.settings!!.archiveCategories.isEmpty())
            ui.activity.openNasa(ArchiveLesson.VIDEO_ID, preview = true)
            assertNull(ui.activity.nasaPlayback)
        }
        lock()
        ui.onNodeWithTag(PLAY).assertDoesNotExist()
        ui.runOnIdle { ui.activity.openNasa(ArchiveLesson.VIDEO_ID); assertNull(ui.activity.nasaPlayback) }
    }

    @Test fun lateFixtureDiscoveryCannotCrossParentLockOrApproveItself() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val requests = AtomicInteger()
        val client = ArchiveClient(enabled = true, transport = {
            requests.incrementAndGet()
            entered.countDown()
            check(release.await(10, TimeUnit.SECONDS)) { "Fixture release timed out" }
            NasaResponse(200, """{"response":{"docs":[{"identifier":"${ArchiveLesson.ITEM_ID}","title":"Addition 3","mediatype":"movies","collection":["khanacademy"]}]}}""")
        })
        ui.runOnIdle { ui.activity.update { it.copy(archiveCategories = setOf("math")) } }
        saved()
        try {
            ui.runOnIdle { ui.activity.discoverArchive("math", "addition", client) }
            assertTrue("Discovery fixture was not reached", entered.await(5, TimeUnit.SECONDS))
            ui.runOnIdle { ui.activity.lock() }
        } finally { release.countDown() }
        saved()
        ui.runOnIdle {
            assertFalse(ui.activity.parent)
            assertFalse(ui.activity.settings!!.nasaVideos.any { it.id == ArchiveLesson.VIDEO_ID })
            assertTrue(ui.activity.settings!!.nasaReviews.isEmpty())
            ui.activity.discoverArchive("math", "addition", client)
            assertFalse(ui.activity.busy)
        }
        assertEquals(1, requests.get())
        assertFalse(SettingsStore(File(ui.activity.filesDir, "settings.json")).load().nasaVideos.any { it.id == ArchiveLesson.VIDEO_ID })
        ui.onNodeWithTag(PLAY).assertDoesNotExist()
        unlock()
        ui.runOnIdle { ui.activity.discoverArchive("math", "addition", client) }
        saved()
        val persisted = SettingsStore(File(ui.activity.filesDir, "settings.json")).load()
        assertTrue("The same valid response must become a pending candidate while unlocked", persisted.nasaVideos.any { it.id == ArchiveLesson.VIDEO_ID })
        assertTrue(persisted.nasaReviews.isEmpty())
        assertEquals(2, requests.get())
        ui.runOnIdle { ui.activity.lock() }
        ui.onNodeWithTag(PLAY).assertDoesNotExist()
    }

    private fun videos() {
        ui.onNodeWithText("Parent settings").performScrollTo().assertIsDisplayed()
        ui.onNodeWithText("Videos").performScrollTo().assertIsDisplayed().performClick()
        ui.runOnIdle { assertEquals("Videos", ui.activity.parentTab) }
    }
    private fun lock() = ui.onNodeWithText("Lock and return home").performScrollTo().performClick()
    private fun saved() = ui.waitUntil(15000) { !ui.activity.busy }
    private fun unlock() {
        ui.onNodeWithText("Parent settings").performScrollTo().performClick()
        ui.onNodeWithText("Parent PIN").performTextInput(PIN)
        ui.onNodeWithText("Unlock").performScrollTo().performClick()
        ui.waitUntil(60000) { ui.activity.parent && !ui.activity.busy }
    }
    private fun nativeVideo(): VideoView? = ui.activity.findViewById<View>(android.R.id.content).findViewWithTag("nasaVideo")
    private fun awaitNativePlayback() = ui.waitUntil(180000) {
        ui.runOnIdle {
            check(ui.activity.nasaPlayback?.id == ArchiveLesson.VIDEO_ID) { "Archive selection closed while waiting" }
            val status = ui.activity.findViewById<View>(android.R.id.content).findViewWithTag<TextView>("nasaStatus")?.text?.toString().orEmpty()
            check(!status.startsWith("Video unavailable") && !status.startsWith("Playback failed")) { "Archive playback failed: $status" }
            nativeVideo()?.let { it.isPlaying && it.duration > 0 && it.currentPosition > 500 } == true
        }
    }
    private fun awaitNoPlayerFiles() = ui.waitUntil(20000) { ui.activity.cacheDir.listFiles()?.none { it.name.startsWith("nasa-player-") } == true }
    companion object {
        private const val PIN = "732918"
        private const val HASH = "a1a9a7538fcdeac9f8cefca54fbd9aea42f4edacde63163ea444c8df127816e7"
        private const val PLAY = "nasa-play-archive:KA-converted-e_SpXIw_Qts"
    }
}
