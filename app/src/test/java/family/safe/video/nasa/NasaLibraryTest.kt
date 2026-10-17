package family.safe.video.nasa

import family.safe.data.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class NasaLibraryTest {
    private val video = NasaVideo("JPL-DYNAMIC-1", "Planet science", "An animation of a planet", "NASA/JPL-Caltech", "Adult content and rights review required", "science")
    private fun state() = FamilySettings(pinHash="test-hash", categories=starterCategories().map { it.copy(enabled=it.id=="science") })
    private fun store() = SettingsStore(Files.createTempDirectory("nasa-library").resolve("settings.json").toFile())

    @Test fun discoveryRequiresParentPinEnabledCategoryAndExactUnblockedPhrase() {
        val state = state()
        assertTrue(canDiscoverNasa(state,true,"science","science"))
        assertFalse(canDiscoverNasa(state,false,"science","science"))
        assertFalse(canDiscoverNasa(state.copy(pinHash=null),true,"science","science"))
        assertFalse(canDiscoverNasa(state,true,"space","science"))
        assertFalse(canDiscoverNasa(state,true,"science","science extra words"))
        assertFalse(canDiscoverNasa(state.copy(blocked=setOf("science")),true,"science","science"))
        assertFalse(canDiscoverNasa(state.copy(categories=starterCategories()),true,"science","science"))
    }

    @Test fun dynamicallyDiscoveredCandidatesPersistButNeverApproveThemselves() {
        val merged = mergeNasaCandidates(state(),"science",listOf(video))
        val store = store()
        store.save(merged)
        val loaded = store.load()
        assertEquals(merged,loaded)
        assertTrue(approvedNasaVideos(loaded).isEmpty())
        assertTrue(loaded.nasaReviews.isEmpty())
        val approved = loaded.copy(nasaReviews=listOf(NasaReview(video.id,true,"a".repeat(64))))
        store.save(approved)
        assertEquals(listOf(video),approvedNasaVideos(store.load()))
    }

    @Test fun childEligibilityDeniesMissingDigestDisabledRemovedBlockedOrRejected() {
        val approved = state().copy(nasaVideos=listOf(video),nasaReviews=listOf(NasaReview(video.id,true,"a".repeat(64))))
        assertEquals(listOf(video),approvedNasaVideos(approved))
        assertTrue(approvedNasaVideos(approved.copy(pinHash=null)).isEmpty())
        assertTrue(approvedNasaVideos(approved.copy(categories=emptyList())).isEmpty())
        assertTrue(approvedNasaVideos(approved.copy(blocked=setOf("planet"))).isEmpty())
        assertTrue(approvedNasaVideos(approved.copy(blocked=setOf("caltech"))).isEmpty())
        assertTrue(approvedNasaVideos(approved.copy(nasaReviews=listOf(NasaReview(video.id,true)))) .isEmpty())
        assertTrue(approvedNasaVideos(approved.copy(nasaReviews=listOf(NasaReview(video.id,false)))) .isEmpty())
        assertTrue(approvedNasaVideos(approved.copy(nasaVideos=emptyList())).isEmpty())
    }

    @Test fun rediscoveryCannotMutateApprovedMetadataCategoryOrDecision() {
        val original = state().copy(nasaVideos=listOf(video),nasaReviews=listOf(NasaReview(video.id,true,"a".repeat(64))))
        val changed = video.copy(title="Different", category="space")
        assertEquals(original,mergeNasaCandidates(original,"space",listOf(changed)))
        assertEquals(original,mergeNasaCandidates(original,"science",listOf(video,video)))
        assertThrows(IllegalArgumentException::class.java) { mergeNasaCandidates(original,"science",listOf(changed)) }
    }

    @Test fun libraryIsBoundedAndMalformedOrOrphanedApprovalsFailClosed() {
        val full = state().copy(nasaVideos=(1..MAX_NASA_VIDEOS).map { video.copy(id="JPL-$it") })
        assertEquals(full,mergeNasaCandidates(full,"science",listOf(video)))
        val store = store()
        for (bad in listOf(
            full.copy(nasaVideos=full.nasaVideos+video),
            state().copy(nasaVideos=listOf(video,video)),
            state().copy(nasaVideos=listOf(video.copy(id="https://evil.example/x"))),
            state().copy(nasaVideos=listOf(video.copy(title="<a>click</a>"))),
            state().copy(nasaVideos=listOf(video.copy(title="safe\u202Eevil"))),
            state().copy(nasaVideos=listOf(video.copy(description="x".repeat(4001)))),
            state().copy(nasaVideos=listOf(video),nasaReviews=listOf(NasaReview("missing",true,"a".repeat(64)))),
            state().copy(nasaVideos=listOf(video),nasaReviews=listOf(NasaReview(video.id,true,"bad"))),
            FamilySettings(nasaVideos=listOf(video))
        )) assertThrows(IllegalArgumentException::class.java) { store.save(bad) }
    }

    @Test fun legacyApprovedCatalogMigratesWithoutAddingApprovalOrChangingIdentity() {
        val file = Files.createTempDirectory("nasa-migration").resolve("settings.json").toFile()
        val store = SettingsStore(file)
        val legacy = FamilySettings(pinHash="test-hash",nasaReviews=listOf(NasaReview(NasaCatalog.videos.first().id,true,"a".repeat(64))))
        store.save(legacy)
        val json = JSONObject(file.readText()); json.remove("nasaVideos"); file.writeText(json.toString())
        assertEquals(legacy,store.load())
        store.save(store.load())
        assertEquals(legacy,store.load())
        assertTrue(approvedNasaVideos(store.load()).isEmpty()) // Disabled Space stays disabled.
    }
}
