package family.safe.data

import family.safe.video.nasa.NasaCatalog
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import org.json.JSONObject

class NasaSettingsTest {
    @Test fun legacySettingsLoadWithoutGrantingVideoApproval() {
        val file=Files.createTempDirectory("nasa-legacy").resolve("settings.json").toFile()
        SettingsStore(file).save(FamilySettings(pinHash="test-hash"))
        val json=JSONObject(file.readText()); json.remove("nasaReviews"); file.writeText(json.toString())
        assertTrue(SettingsStore(file).load().nasaReviews.isEmpty())
    }
    @Test fun decisionsPersistAndMissingPinCannotCarryApprovals() {
        val file=Files.createTempDirectory("nasa-decisions").resolve("settings.json").toFile()
        val state=FamilySettings(pinHash="test-hash",nasaReviews=listOf(NasaReview(NasaCatalog.videos.first().id,true,"a".repeat(64))))
        SettingsStore(file).save(state)
        assertEquals(state,SettingsStore(file).load())
        assertThrows(IllegalArgumentException::class.java) { SettingsStore(file).save(state.copy(pinHash=null)) }
    }
    @Test fun malformedDuplicateOrUnknownDecisionsFailClosed() {
        val file=Files.createTempDirectory("nasa-invalid").resolve("settings.json").toFile()
        val store=SettingsStore(file)
        assertThrows(IllegalArgumentException::class.java) { store.save(FamilySettings(pinHash="hash",nasaReviews=listOf(NasaReview("unknown",true)))) }
        assertThrows(IllegalArgumentException::class.java) { store.save(FamilySettings(pinHash="hash",nasaReviews=listOf(NasaReview(NasaCatalog.videos.first().id,true)))) }
        assertThrows(IllegalArgumentException::class.java) { store.save(FamilySettings(pinHash="hash",nasaReviews=listOf(NasaReview(NasaCatalog.videos.first().id,true,"a".repeat(64)),NasaReview(NasaCatalog.videos.first().id,false)))) }
        store.save(FamilySettings(pinHash="hash"))
        val json=JSONObject(file.readText()).put("nasaReviews", "invalid"); file.writeText(json.toString())
        assertThrows(Exception::class.java) { store.load() }
    }
}
