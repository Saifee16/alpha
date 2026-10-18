package family.safe.settings

import family.safe.data.*
import org.junit.Assert.*
import org.junit.Test

class ReadingSetupTest {
    @Test fun explicitSetupConnectsSelectedTopicsWithoutResettingOtherChoices() {
        val state=FamilySettings(pinHash="fixture",attempts=PinAttempts(3,1234),
            requests=listOf(ApprovalRequest("request","piano")),blocked=setOf("gaming"),
            sources=starterSources().filterNot { it.id=="wikijunior" })
        val ready=completeReadingSetup(state,setOf("animals","sports"))
        assertTrue(ready.categories.filter { it.enabled }.map { it.id }.toSet()==setOf("animals","sports"))
        assertTrue(ready.sources.filter { it.id in setOf("vikidia","wikijunior") }.all {
            it.enabled && it.categories.containsAll(setOf("animals","sports"))
        })
        assertEquals(state.pinHash,ready.pinHash)
        assertEquals(state.attempts,ready.attempts)
        assertEquals(state.requests,ready.requests)
        assertEquals(state.blocked,ready.blocked)
        assertEquals(state.nasaReviews,ready.nasaReviews)
        assertEquals(state.nasaVideos,ready.nasaVideos)
        assertEquals(state.sources.filter { it.id!="vikidia" },ready.sources.filter { it.id !in setOf("vikidia","wikijunior") })
    }
    @Test fun invalidOrUnprotectedSetupCannotEnableAccess() {
        assertThrows(IllegalArgumentException::class.java) { completeReadingSetup(FamilySettings(),setOf("animals")) }
        assertThrows(IllegalArgumentException::class.java) { completeReadingSetup(FamilySettings(pinHash="fixture"),emptySet()) }
        assertThrows(IllegalArgumentException::class.java) { completeReadingSetup(FamilySettings(pinHash="fixture"),setOf("unknown")) }
    }
    @Test fun enablingReadingSourcesDoesNotActivateDormantTopicAssociations() {
        val defaults=FamilySettings(pinHash="fixture")
        val state=defaults.copy(categories=defaults.categories.map { it.copy(enabled=it.id=="math") },
            sources=defaults.sources.filterNot { it.id=="wikijunior" })
        val ready=completeReadingSetup(state,setOf("animals"))
        assertTrue(ready.categories.any { it.id=="math" && it.enabled })
        ready.sources.filter { it.id in setOf("vikidia","wikijunior") }.forEach {
            assertTrue(it.enabled)
            assertEquals(setOf("animals"),it.categories)
        }
        val previouslyEnabled=state.copy(sources=state.sources.map {
            if(it.id=="vikidia") it.copy(enabled=true,categories=setOf("math")) else it
        })
        assertEquals(setOf("math","animals"),
            completeReadingSetup(previouslyEnabled,setOf("animals")).sources.single { it.id=="vikidia" }.categories)
    }
}
