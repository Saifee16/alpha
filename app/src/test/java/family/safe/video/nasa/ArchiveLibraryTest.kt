package family.safe.video.nasa

import family.safe.data.*
import family.safe.settings.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class ArchiveLibraryTest {
    private val digest = "a1a9a7538fcdeac9f8cefca54fbd9aea42f4edacde63163ea444c8df127816e7"
    private fun state() = FamilySettings(pinHash="test-hash", categories=starterCategories().map {
        if(it.id=="math") it.copy(enabled=true,terms=it.terms+"addition") else it
    }, archiveCategories=setOf("math"))

    @Test fun sourceOptInAndWholeQueryAreRequiredBeforeDiscovery() {
        val state=state()
        assertTrue(canDiscoverArchive(state,true,"math","addition for kids"))
        assertFalse(canDiscoverArchive(state,false,"math","addition"))
        assertFalse(canDiscoverArchive(state.copy(pinHash=null),true,"math","addition"))
        assertFalse(canDiscoverArchive(state.copy(archiveCategories=emptySet()),true,"math","addition"))
        assertFalse(canDiscoverArchive(state,true,"science","addition"))
        assertFalse(canDiscoverArchive(state,true,"math","addition gaming"))
        assertFalse(canDiscoverArchive(state.copy(blocked=setOf("addition")),true,"math","addition"))
    }

    @Test fun discoveryStaysPendingAndSourceDisableOrWrongDigestHidesReviewedLesson() {
        val video=archiveVideo("math")
        val pending=mergeNasaCandidates(state(),"math",listOf(video))
        assertTrue(approvedNasaVideos(pending).isEmpty())
        assertTrue(pending.nasaReviews.isEmpty())
        val approved=pending.copy(nasaReviews=listOf(NasaReview(video.id,true,digest)))
        assertEquals(listOf(video),approvedNasaVideos(approved))
        assertTrue(approvedNasaVideos(approved.copy(archiveCategories=emptySet())).isEmpty())
        assertTrue(approvedNasaVideos(approved.copy(blocked=setOf("addition"))).isEmpty())
        assertTrue(approvedNasaVideos(approved.copy(nasaReviews=listOf(NasaReview(video.id,true,"a".repeat(64))))).isEmpty())
        assertTrue(approvedNasaVideos(approved.copy(categories=starterCategories())).isEmpty())
    }

    @Test fun providerIdentityCannotBorrowAnotherProvidersIdOrApproval() {
        val archive=archiveVideo("math")
        assertTrue(validNasaVideo(archive))
        assertFalse(validNasaVideo(archive.copy(provider="nasa")))
        assertFalse(validNasaVideo(archive.copy(provider="unknown")))
        assertFalse(validNasaVideo(archive.copy(id="JPL-EXISTING")))
        assertFalse(validNasaVideo(archive.copy(title="Other lesson")))
        assertFalse(validNasaVideo(NasaCatalog.videos.first().copy(provider="archive")))
    }

    @Test fun additiveSourceSettingsPreserveLegacyReviewsAndUnknownProviderLocksLoad() {
        val file=Files.createTempDirectory("archive-library").resolve("settings.json").toFile()
        val store=SettingsStore(file)
        val legacy=FamilySettings(pinHash="test-hash",nasaReviews=listOf(NasaReview(NasaCatalog.videos.first().id,true,"a".repeat(64))))
        store.save(legacy)
        val old=JSONObject(file.readText()).apply { remove("archiveCategories") }
        val videos=old.getJSONArray("nasaVideos")
        for(index in 0 until videos.length()) videos.getJSONObject(index).remove("provider")
        file.writeText(old.toString())
        assertEquals(legacy,store.load())
        val current=state().copy(nasaVideos=listOf(archiveVideo("math")))
        store.save(current)
        assertEquals(current,store.load())
        val bad=JSONObject(file.readText())
        bad.getJSONArray("nasaVideos").getJSONObject(0).put("provider","unknown")
        file.writeText(bad.toString())
        assertThrows(IllegalArgumentException::class.java) { store.load() }
    }

    @Test fun topicRemovalAndBroadResetRevokeSourceAccessWithoutErasingAuthentication() {
        val original=state().copy(nasaVideos=listOf(archiveVideo("math")),nasaReviews=listOf(NasaReview(archiveVideo("math").id,true,digest)))
        val removed=removeTopicRules(original,"math")
        assertTrue(removed.archiveCategories.isEmpty())
        assertTrue(removed.nasaReviews.isEmpty())
        assertEquals(original.pinHash,removed.pinHash)
        assertEquals(original.nasaVideos,removed.nasaVideos)
        assertTrue(resetRules(original,RuleGroup.ALL).archiveCategories.isEmpty())
        assertTrue(resetRules(original,RuleGroup.TOPICS).archiveCategories.isEmpty())
        assertEquals(setOf("math"),resetRules(original,RuleGroup.WEBSITES).archiveCategories)
    }
}
