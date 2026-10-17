package family.safe.video.nasa

import family.safe.data.FamilySettings
import family.safe.data.NasaReview
import family.safe.data.starterCategories
import org.junit.Assert.*
import org.junit.Test

class NasaAccessTest {
    private fun enabled() = FamilySettings(pinHash="test-hash", categories=starterCategories().map { it.copy(enabled=it.id=="space") })
    @Test fun pendingRejectedDisabledAndUnknownNeverReachChildren() {
        val id=NasaCatalog.videos.first().id
        val state=enabled()
        assertTrue(approvedNasaVideos(state).isEmpty())
        assertTrue(approvedNasaVideos(state.copy(nasaReviews=listOf(NasaReview(id,false)))).isEmpty())
        assertTrue(approvedNasaVideos(FamilySettings(pinHash="test-hash",nasaReviews=listOf(NasaReview(id,true,"a".repeat(64))))).isEmpty())
        assertTrue(approvedNasaVideos(state.copy(nasaReviews=listOf(NasaReview("https://youtube.com/shorts/escape",true,"a".repeat(64))))).isEmpty())
        assertTrue(approvedNasaVideos(state.copy(nasaReviews=listOf(NasaReview(id,true)))).isEmpty())
    }
    @Test fun onlyApprovedKnownAssetsInEnabledSpaceCategoryAreVisible() {
        val id=NasaCatalog.videos.first().id
        val state=enabled().copy(nasaReviews=listOf(NasaReview(id,true,"a".repeat(64))))
        assertEquals(listOf(id),approvedNasaVideos(state).map { it.id })
        assertTrue(approvedNasaVideos(state.copy(blocked=state.blocked+NasaCatalog.videos.first().title)).isEmpty())
        assertTrue(approvedNasaVideos(state.copy(pinHash=null)).isEmpty())
    }
}
