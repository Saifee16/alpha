package family.safe.video.nasa

import family.safe.data.*
import family.safe.video.archive.ArchiveLesson
import org.junit.Assert.*
import org.junit.Test

class ExpandedArchiveGateTest {
    @Test fun everyKnownSnapshotRequiresItsOwnDigestAndCurrentSourceConsent() {
        val state=FamilySettings(pinHash="fixture",categories=starterCategories().map { it.copy(enabled=it.id=="math") },archiveCategories=setOf("math"))
        for(snapshot in ArchiveLesson.snapshots) {
            val video=NasaVideo(snapshot.videoId,snapshot.title,snapshot.description,snapshot.credit,snapshot.rightsNote,"math","archive")
            assertTrue(snapshot.title,validNasaVideo(video))
            val reviewed=state.copy(nasaVideos=listOf(video),nasaReviews=listOf(NasaReview(video.id,true,snapshot.sha256)))
            assertEquals(listOf(video),approvedNasaVideos(reviewed))
            assertTrue(approvedNasaVideos(reviewed.copy(archiveCategories=emptySet())).isEmpty())
            assertTrue(approvedNasaVideos(reviewed.copy(nasaReviews=listOf(NasaReview(video.id,true,"0".repeat(64))))).isEmpty())
            assertTrue(approvedNasaVideos(reviewed.copy(nasaReviews=emptyList())).isEmpty())
        }
    }
}
