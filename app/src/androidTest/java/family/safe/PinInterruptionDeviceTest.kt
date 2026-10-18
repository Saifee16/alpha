package family.safe

import android.os.Build
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import family.safe.data.FamilySettings
import family.safe.data.SettingsStore
import family.safe.settings.PinAttempts
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Two-stage public fixture. The external driver kills only the guarded emulator app between stages. */
class PinInterruptionDeviceTest {
    @Test fun beginAuthenticationForExternalInterruption() {
        val context = disposableContext()
        val directory = requireNotNull(context.getExternalFilesDir("evidence")).also { check(it.isDirectory || it.mkdirs()) }
        val marker = File(directory, "pin-reservation-ready.json")
        check(!marker.exists() || marker.delete())
        SettingsStore(File(context.filesDir, "settings.json")).save(FamilySettings(
            pinHash = RECORD, attempts = PinAttempts(failures = 9)))
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            await(scenario) { it.settings != null }
            // A late kill must fail stage two: completed success would clear the reservation.
            scenario.onActivity { it.authenticate("732918", "") }
            await(scenario) { it.busy && it.settings?.attempts?.failures == 10 }
            val saved = SettingsStore(File(context.filesDir, "settings.json")).load()
            assertEquals(10, saved.attempts.failures)
            assertTrue(saved.attempts.nextAllowedAt > System.currentTimeMillis())
            scenario.onActivity {
                assertTrue("Authentication must still be pending at the interruption marker", it.busy)
                marker.writeText(JSONObject().put("reservedWhileAuthenticationPending", true).put("publicFixtureOnly", true).toString())
            }
            // The driver terminates pending authentication after the durable reservation.
            CountDownLatch(1).await(30, TimeUnit.SECONDS)
            fail("The driver must force-stop this guarded fixture process after the marker")
        }
    }

    @Test fun persistedReservationStillBlocksCorrectPinAfterProcessRestart() {
        val context = disposableContext()
        val file = File(context.filesDir, "settings.json")
        val before = file.readBytes()
        val saved = SettingsStore(file).load()
        assertEquals(RECORD, saved.pinHash)
        assertEquals(10, saved.attempts.failures)
        assertTrue(saved.attempts.nextAllowedAt > System.currentTimeMillis())
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            await(scenario) { it.settings != null }
            scenario.onActivity {
                it.authenticate("732918", "")
                assertFalse(it.parent)
                assertFalse(it.busy)
                assertTrue(it.message.contains("wait", true))
                assertEquals(saved.attempts, it.settings!!.attempts)
            }
        }
        assertArrayEquals(before, file.readBytes())
    }

    private fun disposableContext(): android.content.Context {
        assumeTrue("Requires the two-stage external driver", InstrumentationRegistry.getArguments()
            .getString("pinInterruptionFixture") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(InstrumentationRegistry.getArguments().getString("disposableInstrumentation") == "true")
        check(BuildConfig.BUILD_TYPE == "debug" && context.packageName == "family.safe.debug")
        check(Build.HARDWARE == "ranchu")
        return context
    }
    private fun await(scenario: ActivityScenario<MainActivity>, condition: (MainActivity) -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 30000
        while (SystemClock.elapsedRealtime() < deadline) {
            var ready = false
            scenario.onActivity { ready = condition(it) }
            if (ready) return
            SystemClock.sleep(25)
        }
        fail("Guarded authentication fixture did not reach the required phase")
    }
    companion object {
        private const val RECORD = "v1:600000:AAECAwQFBgcICQoLDA0ODw==:EvvCvbabu/NneEP7HwNcLFwerIOXZF/GSNbe+m9jQCk="
    }
}
