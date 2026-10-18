package family.safe.video.nasa

import family.safe.data.*
import family.safe.video.archive.ArchiveLesson
import org.junit.Assert.*
import org.junit.Test

class ArchiveExpansionAccessTest {
    private fun state(): FamilySettings {
        val videos = ArchiveLesson.snapshots.map { archiveVideo("math", it) }
        return FamilySettings(pinHash="fixture", archiveCategories=setOf("math"),
            categories=starterCategories().map { if(it.id=="math") it.copy(enabled=true,terms=setOf("math","maths","fractions","multiplication")) else it },
            nasaVideos=videos,nasaReviews=ArchiveLesson.snapshots.map { NasaReview(it.videoId,true,it.sha256) })
    }

    @Test fun eachArchiveApprovalNeedsThatExactSnapshotsDigest() {
        val state=state()
        assertEquals(3,approvedNasaVideos(state).size)
        val wrong=state.copy(nasaReviews=state.nasaReviews.map { it.copy(sha256=ArchiveLesson.SHA256) })
        assertEquals(listOf("archive:KA-converted-e_SpXIw_Qts"),approvedNasaVideos(wrong).map { it.id })
        assertFalse(validNasaVideo(state.nasaVideos.last().copy(title="Multiplication 2: The Multiplication Tables")))
        assertEquals(emptyList<NasaVideo>(),approvedNasaVideos(state.copy(nasaReviews=emptyList())))
    }

    @Test fun watchSearchRequiresApprovalPolicyAndRelevantLessonMetadata() {
        val state=state()
        assertEquals(3,approvedWatchVideos(state,"").size)
        assertEquals(listOf("Fractions in lowest terms"),approvedWatchVideos(state,"fractions for kids").map { it.title })
        assertEquals(listOf("Multiplication 2: The Multiplication Tables"),approvedWatchVideos(state,"multiplication").map { it.title })
        assertEquals(3,approvedWatchVideos(state,"math").size)
        assertEquals(3,approvedWatchVideos(state,"maths").size)
        assertTrue(approvedWatchVideos(state,"unapproved").isEmpty())
        assertTrue(approvedWatchVideos(state.copy(blocked=setOf("fractions")),"fractions").isEmpty())
        assertTrue(approvedWatchVideos(state.copy(archiveCategories=emptySet()),"").isEmpty())
    }
}
