package family.safe

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import family.safe.testing.LiveSiteHarnessActivity
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/** Live transport/render smoke only; screenshots require human usability/content review. */
class LiveSitesDeviceTest {
    @Test fun approvedSitesCommitReadOnlyPagesAndProduceReviewScreenshots() {
        assumeTrue("Opt in with -e liveSites true", InstrumentationRegistry.getArguments().getString("liveSites") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val failures = mutableListOf<String>()
        val evidenceDirectory = requireNotNull(context.getExternalFilesDir("evidence")).apply { mkdirs() }
        listOf("nasa", "natgeo", "bbc").forEach { id ->
            try {
                ActivityScenario.launch<LiveSiteHarnessActivity>(Intent(context, LiveSiteHarnessActivity::class.java).putExtra("sourceId", id)).use { scenario ->
                    val finished = AtomicReference(false)
                    val deadline = SystemClock.uptimeMillis() + 60000
                    while (!finished.get() && SystemClock.uptimeMillis() < deadline) {
                        scenario.onActivity { finished.set(it.blocked || (it.browser.progress == 100 && it.mainStatus != 0 && it.browser.visibility == View.VISIBLE)) }
                        if (!finished.get()) SystemClock.sleep(100)
                    }
                    var rendered = false
                    var observation = ""
                    scenario.onActivity {
                        rendered = !it.blocked && it.mainStatus in 200..299 && it.mainMime == "text/html" && it.mainBytes > 0 && it.browser.progress == 100 && it.browser.visibility == View.VISIBLE
                        observation = "$id: HTTP=${it.mainStatus}, MIME=${it.mainMime}, bytes=${it.mainBytes}, requests=${it.requestCount}, denied=${it.deniedCount}, progress=${it.browser.progress}, visible=${it.browser.visibility == View.VISIBLE}, blocked=${it.blocked}; transport/render smoke only; usability/content NOT verified."
                    }
                    val screenshot = requireNotNull(instrumentation.uiAutomation.takeScreenshot()) { "Screenshot unavailable for $id" }
                    val screenshotFile = File(evidenceDirectory, "live-$id.png")
                    try { screenshotFile.outputStream().use { check(screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)) } }
                    finally { screenshot.recycle() }
                    observation += " Screenshot=${screenshotFile.absolutePath}"
                    android.util.Log.i("FamilySafeLiveSites", observation)
                    instrumentation.sendStatus(0, Bundle().apply { putString("stream", "$observation\n") })
                    if (!rendered) failures += observation
                }
            } catch (failure: Exception) {
                val detail = "$id live smoke failed: ${failure.javaClass.simpleName}: ${failure.message}"
                failures += detail
                instrumentation.sendStatus(0, Bundle().apply { putString("stream", "$detail\n") })
            }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }
}
