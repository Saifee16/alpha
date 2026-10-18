package family.safe

import android.graphics.Bitmap
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.os.SystemClock
import android.widget.VideoView
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import family.safe.testing.BrowserHarnessActivity
import family.safe.video.archive.ArchiveLesson
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

/** Disposable engineering probe only: no settings mutation, content approval or provider integration. */
class ArchiveMediaProbeTest {
    @Test fun verifiedPublicMp4DecodesAndSupportsNativePlaybackControls() {
        assumeTrue("Requires -e archiveProbe true", InstrumentationRegistry.getArguments().getString("archiveProbe") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        check(BuildConfig.DEBUG && context.packageName == "family.safe.debug")
        check(Build.HARDWARE in setOf("ranchu", "goldfish")) { "Requires a disposable ranchu/goldfish emulator" }
        val videoId = InstrumentationRegistry.getArguments().getString("archiveProbeVideoId") ?: ArchiveLesson.VIDEO_ID
        val snapshot = requireNotNull(ArchiveLesson.find(videoId)) { "Probe requires a known verified Archive video ID" }
        val evidence = requireNotNull(context.getExternalFilesDir("evidence"))
        val source = File(evidence, "archive-probe.mp4")
        assertEquals("Push the exact selected public probe file first", snapshot.bytes, source.length())
        assertEquals(snapshot.sha256, sha256(source))
        val local = File.createTempFile("archive-probe-", ".mp4", context.cacheDir)
        var scenario: ActivityScenario<BrowserHarnessActivity>? = null
        var video: VideoView? = null
        try {
            source.inputStream().use { input -> local.outputStream().use { input.copyTo(it) } }
            assertEquals(snapshot.bytes, local.length())
            assertEquals(snapshot.sha256, sha256(local))
            val extractor = MediaExtractor()
            val codecs = try {
                extractor.setDataSource(local.absolutePath)
                (0 until extractor.trackCount).map { extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME) }
            } finally { extractor.release() }
            assertEquals(setOf("video/avc", "audio/mp4a-latm"), codecs.toSet())
            val failure = AtomicReference<String?>(null)
            val active = ActivityScenario.launch(BrowserHarnessActivity::class.java)
            scenario = active
            active.onActivity { activity ->
                activity.browser.stopLoading()
                video = VideoView(activity).also { native ->
                    native.setOnErrorListener { _, what, extra -> failure.set("Native media error $what/$extra"); true }
                    native.setOnPreparedListener { native.start() }
                    activity.setContentView(native)
                    native.setVideoPath(local.absolutePath)
                }
            }
            val native = requireNotNull(video)
            await(active, native, failure, "playback preparation") { it.isPlaying && it.duration > 0 }
            var duration = 0
            var initial = 0
            active.onActivity { duration = native.duration; initial = native.currentPosition }
            await(active, native, failure, "playback progression") { it.isPlaying && it.currentPosition > initial + 500 }
            var paused = 0
            active.onActivity { native.pause(); paused = native.currentPosition }
            SystemClock.sleep(700)
            active.onActivity { assertFalse(native.isPlaying); assertTrue("Pause did not hold position", abs(native.currentPosition - paused) < 300) }
            val target = duration * 3 / 4
            assertTrue("Probe is too short to distinguish a seek from its current position", abs(target - paused) > 2000)
            active.onActivity { native.seekTo(target) }
            await(active, native, failure, "seek while paused") { !it.isPlaying && abs(it.currentPosition - target) < 2000 }
            var resumed = 0
            active.onActivity { resumed = native.currentPosition; native.start() }
            await(active, native, failure, "resumed progression") { it.isPlaying && it.currentPosition > resumed + 500 }
            val frame = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
            try { File(evidence, "archive-probe.png").outputStream().use { check(frame.compress(Bitmap.CompressFormat.PNG, 100, it)) } }
            finally { frame.recycle() }
            active.onActivity { native.stopPlayback(); assertFalse(native.isPlaying) }
            File(evidence, "archive-probe-result.json").writeText(JSONObject()
                .put("videoId", snapshot.videoId).put("sha256", snapshot.sha256).put("bytes", local.length()).put("codecs", org.json.JSONArray(codecs))
                .put("durationMs", duration).put("initialPositionMs", initial).put("pausedPositionMs", paused)
                .put("seekTargetMs", target).put("resumedPositionMs", resumed)
                .put("progressionPauseSeekResumeStop", "PASS").put("backgroundPolicyTested", false)
                .put("contentApproved", false).toString(2))
        } finally {
            try { instrumentation.runOnMainSync { video?.stopPlayback() } }
            finally { try { scenario?.close() } finally { check(local.delete()) { "Could not delete private probe copy" } } }
        }
    }

    private fun await(scenario: ActivityScenario<BrowserHarnessActivity>, video: VideoView, error: AtomicReference<String?>, phase: String, condition: (VideoView) -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 30000
        var matched = false
        while (SystemClock.uptimeMillis() < deadline) {
            scenario.onActivity { check(error.get() == null) { error.get().orEmpty() }; matched = condition(video) }
            if (matched) return
            SystemClock.sleep(50)
        }
        fail("Public MP4 probe timed out during $phase after 30 seconds")
    }
    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input -> val buffer = ByteArray(8192); while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) } }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
