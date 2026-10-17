package family.safe

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.VideoView
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import family.safe.video.nasa.NasaPlaybackSpikeActivity
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

/** Explicit live adult-only source experiment; normal instrumentation runs skip it. */
class NasaPlaybackDeviceTest {
    @Test fun liveReferenceDownloadsPlaysSeeksCompletesAndReleasesOnBackground() {
        assumeTrue("Opt in with -e nasaLive true", InstrumentationRegistry.getArguments().getString("nasaLive") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val videoRef = AtomicReference<VideoView>()
        ActivityScenario.launch(NasaPlaybackSpikeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val root = activity.findViewById<View>(android.R.id.content)
                videoRef.set(root.findViewWithTag("nasaVideo"))
                root.findViewWithTag<Button>("nasaLoad").performClick()
            }
            await(scenario, 120000) { activity, video ->
                val status = activity.findViewById<View>(android.R.id.content).findViewWithTag<TextView>("nasaStatus").text.toString()
                check(!status.startsWith("NASA download denied") && !status.startsWith("Native playback failed")) { status }
                video.isPlaying && video.duration > 0
            }
            var duration = 0
            var initialPosition = 0
            scenario.onActivity { activity ->
                assertEquals(NasaPlaybackSpikeActivity::class.java.name, activity.componentName.className)
                duration = videoRef.get().duration
                initialPosition = videoRef.get().currentPosition
            }
            await(scenario, 10000) { _, video -> video.currentPosition > initialPosition + 500 }
            val evidenceDirectory = requireNotNull(instrumentation.targetContext.getExternalFilesDir("evidence")).apply { mkdirs() }
            val frame = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
            try {
                java.io.File(evidenceDirectory, "nasa-player.png").outputStream().use {
                    check(frame.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it))
                }
            } finally { frame.recycle() }
            var pausedPosition = 0
            scenario.onActivity { videoRef.get().pause(); pausedPosition = videoRef.get().currentPosition }
            SystemClock.sleep(700)
            scenario.onActivity {
                assertFalse(videoRef.get().isPlaying)
                assertTrue("Paused native position changed", abs(videoRef.get().currentPosition - pausedPosition) < 300)
                videoRef.get().seekTo(duration / 2)
            }
            await(scenario, 10000) { _, video -> abs(video.currentPosition - duration / 2) < 4000 }
            var resumePosition = 0
            scenario.onActivity { resumePosition = videoRef.get().currentPosition; videoRef.get().start() }
            await(scenario, 10000) { _, video -> video.isPlaying && video.currentPosition > resumePosition + 500 }
            scenario.onActivity { videoRef.get().seekTo((duration - 600).coerceAtLeast(0)); videoRef.get().start() }
            await(scenario, 15000) { activity, video ->
                activity.findViewById<View>(android.R.id.content).findViewWithTag<TextView>("nasaStatus").text.startsWith("Completed.") && !video.isPlaying
            }
            var cacheDirectory: java.io.File? = null
            scenario.onActivity { activity ->
                assertEquals(NasaPlaybackSpikeActivity::class.java.name, activity.componentName.className)
                cacheDirectory = activity.cacheDir
                activity.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
            }
            val stopDeadline = SystemClock.uptimeMillis() + 10000
            while (scenario.state != Lifecycle.State.DESTROYED && SystemClock.uptimeMillis() < stopDeadline) SystemClock.sleep(50)
            assertEquals("Background must finish the adult playback harness", Lifecycle.State.DESTROYED, scenario.state)
            instrumentation.runOnMainSync { assertFalse("Native player must stop on background", videoRef.get().isPlaying) }
            assertTrue("Temporary downloaded video must be removed", cacheDirectory!!.listFiles()!!.none { it.name.startsWith("nasa-spike-") && it.extension == "mp4" })
            val evidence = "NASA native playback PASS: durationMs=$duration; initialPositionMs=$initialPosition; pausedPositionMs=$pausedPosition; resumedFromMs=$resumePosition; seek/completion/background-release verified."
            android.util.Log.i("FamilySafeNasaTest", evidence)
            instrumentation.sendStatus(0, Bundle().apply { putString("stream", "$evidence\n") })
        }
    }

    private fun await(scenario: ActivityScenario<NasaPlaybackSpikeActivity>, timeout: Long, condition: (NasaPlaybackSpikeActivity, VideoView) -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + timeout
        val matched = AtomicReference(false)
        while (SystemClock.uptimeMillis() < deadline) {
            scenario.onActivity { activity ->
                val video = activity.findViewById<View>(android.R.id.content).findViewWithTag<VideoView>("nasaVideo")
                matched.set(condition(activity, video))
            }
            if (matched.get()) return
            SystemClock.sleep(100)
        }
        fail("NASA native playback condition timed out after ${timeout}ms")
    }
}
