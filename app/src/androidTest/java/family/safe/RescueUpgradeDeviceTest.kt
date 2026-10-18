package family.safe

import android.os.Build
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import family.safe.browser.BrowserSource
import family.safe.data.ApprovalRequest
import family.safe.data.FamilySettings
import family.safe.data.NasaReview
import family.safe.data.SettingsStore
import family.safe.policy.Category
import family.safe.settings.PinAttempts
import family.safe.settings.PinSecurity
import family.safe.video.nasa.NasaVideo
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/** Run prepare on the legacy APK, then install the new APK with -r and run verify; no Activity. */
class RescueUpgradeDeviceTest {
    @Test fun prepareLegacyFixture() {
        guard()
        val settings = settingsFile()
        SettingsStore(settings).save(expected())
        assertEquals(expected(), SettingsStore(settings).load())
        evidence("rescue-upgrade-prepared.json").writeText(JSONObject()
            .put("fixturePrepared", true).put("settingsSha256", sha256(settings))
            .put("settingsModified", settings.lastModified()).put("pinIterations", 600000).toString(2))
    }

    @Test fun verifyLegacyFixtureWithoutWriting() {
        guard()
        val settings = settingsFile()
        val prepared = JSONObject(evidence("rescue-upgrade-prepared.json").readText())
        assertTrue(prepared.getBoolean("fixturePrepared"))
        val bytes = settings.readBytes()
        val modified = settings.lastModified()
        assertEquals("The replacement install must preserve fixture bytes", prepared.getString("settingsSha256"), sha256(settings))
        assertEquals("The replacement install must preserve the settings timestamp", prepared.getLong("settingsModified"), modified)
        val loaded = SettingsStore(settings).load()
        assertEquals("All legacy settings must survive the upgrade", expected(), loaded)
        val started = SystemClock.elapsedRealtime()
        val verified = PinSecurity.verify("732918", requireNotNull(loaded.pinHash))
        val verificationMillis = SystemClock.elapsedRealtime() - started
        assertTrue("The legacy 600000-iteration public fixture PIN must verify", verified)
        assertArrayEquals("Verification must never save settings", bytes, settings.readBytes())
        assertEquals(modified, settings.lastModified())
        evidence("rescue-upgrade-verified.json").writeText(JSONObject()
            .put("legacySettingsEqual", true).put("bytesPreserved", true).put("timestampPreserved", true)
            .put("pinVerified", verified).put("pinIterations", 600000).put("pinVerificationMs", verificationMillis)
            .put("clock", "monotonic").put("settingsWrittenDuringVerification", false)
            .put("simulatedEngineeringFixture", true).put("humanContentRightsReview", false).toString(2))
    }

    private fun expected() = FamilySettings(
        pinHash = RECORD, attempts = PinAttempts(2, 0),
        categories = listOf(Category("math", "Upgrade fixture maths", setOf("addition", "fractions"), true)),
        blocked = setOf("fixtureupgradeblocked"),
        sources = listOf(BrowserSource("legacy-fixture", "Upgrade fixture reading", "learn.example.org", setOf("math"), listOf("/lessons"), true, false, false)),
        channels = emptyList(), requests = listOf(ApprovalRequest("upgrade-fixture-request", "fixture pending question", "pending")),
        nasaReviews = listOf(NasaReview(ADDITION_ID, true, ADDITION_HASH)),
        nasaVideos = listOf(NasaVideo(ADDITION_ID, "Addition 3", "Practice carrying digits to add multi-digit numbers.",
            "Khan Academy engineering fixture", "Disposable engineering fixture only; no adult content or rights review is claimed.", "math", "archive")),
        archiveCategories = setOf("math")
    )
    private fun guard() {
        assumeTrue("Requires explicit rescueUpgradeFixture=true", InstrumentationRegistry.getArguments().getString("rescueUpgradeFixture") == "true")
        check(InstrumentationRegistry.getArguments().getString("disposableInstrumentation") == "true") { "Requires explicit disposableInstrumentation=true before preparing or reading upgrade fixtures" }
        check(BuildConfig.BUILD_TYPE == "debug" && InstrumentationRegistry.getInstrumentation().targetContext.packageName == "family.safe.debug")
        check(Build.HARDWARE in setOf("ranchu", "goldfish")) { "Requires a dedicated disposable ranchu/goldfish emulator" }
    }
    private fun settingsFile() = File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "settings.json")
    private fun evidence(name: String): File = File(requireNotNull(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir("evidence")).apply { check(isDirectory || mkdirs()) }, name)
    private fun sha256(file: File): String = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
    companion object {
        private const val RECORD = "v1:600000:AAECAwQFBgcICQoLDA0ODw==:EvvCvbabu/NneEP7HwNcLFwerIOXZF/GSNbe+m9jQCk="
        private const val ADDITION_ID = "archive:KA-converted-e_SpXIw_Qts"
        private const val ADDITION_HASH = "a1a9a7538fcdeac9f8cefca54fbd9aea42f4edacde63163ea444c8df127816e7"
    }
}
