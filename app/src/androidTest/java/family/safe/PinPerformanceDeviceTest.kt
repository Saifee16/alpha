package family.safe

import android.os.Build
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import family.safe.settings.PinSecurity
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.crypto.SecretKeyFactory

/** Isolated public fixture benchmark: no Activity, saved settings, or authentication state. */
class PinPerformanceDeviceTest {
    @Test fun measureOneProductionPinVerification() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(InstrumentationRegistry.getArguments().getString("disposableInstrumentation") == "true") {
            "Requires explicit disposableInstrumentation=true"
        }
        check(BuildConfig.BUILD_TYPE == "debug" && context.packageName == "family.safe.debug")
        check(Build.HARDWARE == "ranchu") { "Requires a dedicated disposable ranchu emulator" }

        val provider = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").provider.name
        val started = SystemClock.elapsedRealtime()
        val verified = PinSecurity.verify("732918", RECORD)
        val duration = SystemClock.elapsedRealtime() - started
        val directory = requireNotNull(context.getExternalFilesDir("evidence"))
        check(directory.isDirectory || directory.mkdirs())
        File(directory, "rescue-pin-isolated.json").writeText(JSONObject()
            .put("platformPbkdf2Provider", provider)
            .put("measuredOperation", "PinSecurity.verify")
            .put("iterations", 600000)
            .put("duration_ms", duration)
            .put("verified", verified)
            .toString(2))
        assertTrue("Known public fixture PIN must verify", verified)
        val budget = InstrumentationRegistry.getArguments().getString("pinBudgetMs")?.toLong() ?: Long.MAX_VALUE
        assertTrue("PIN verification ${duration}ms exceeds ${budget}ms fixture budget", duration <= budget)
    }

    companion object {
        // Python hashlib.pbkdf2_hmac SHA-256, 600000 iterations, salt bytes 0..15, 32-byte key.
        private const val RECORD = "v1:600000:AAECAwQFBgcICQoLDA0ODw==:EvvCvbabu/NneEP7HwNcLFwerIOXZF/GSNbe+m9jQCk="
    }
}
