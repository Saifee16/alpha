package family.safe.settings

import family.safe.browser.BrowserSource
import family.safe.data.*
import family.safe.policy.Category
import family.safe.video.TrustedChannel
import family.safe.video.nasa.NasaCatalog
import org.junit.Assert.*
import org.junit.Test

class RuleChangesTest {
    private fun configured() = FamilySettings(
        pinHash = "unchanged-pin-record", attempts = PinAttempts(4, 123456),
        categories = listOf(Category("space", "Space", setOf("space"), true),
            Category("custom", "Music", setOf("piano lessons"), true)),
        sources = listOf(BrowserSource("shared", "Lessons", "learn.example.org", setOf("space", "custom"), enabled = true),
            BrowserSource("space-only", "Space lessons", "space.example.org", setOf("space"), enabled = true)),
        channels = listOf(TrustedChannel("shared", "Shared", setOf("space", "custom")),
            TrustedChannel("space-only", "Space", setOf("space"))),
        blocked = setOf("gaming"), requests = listOf(ApprovalRequest("request", "piano lessons")),
        nasaReviews = listOf(NasaReview(NasaCatalog.videos.first().id, true, "a".repeat(64)))
    )

    @Test fun removingTopicDetachesMembershipsAndRevokesItsVideoApprovals() {
        val before = configured()
        val after = removeTopicRules(before, "space")
        assertEquals(listOf("custom"), after.categories.map { it.id })
        assertEquals(setOf("custom"), after.sources.first().categories)
        assertTrue(after.sources.first().enabled)
        assertTrue(after.sources.last().categories.isEmpty())
        assertFalse(after.sources.last().enabled)
        assertEquals(listOf("shared"), after.channels.map { it.id })
        assertEquals(setOf("custom"), after.channels.single().categories)
        assertTrue(after.nasaReviews.isEmpty())
        assertEquals(before.nasaVideos, after.nasaVideos)
        assertEquals(before.pinHash, after.pinHash)
        assertEquals(before.attempts, after.attempts)
        assertEquals(before.requests, after.requests)
        assertEquals(before.blocked, after.blocked)
        assertEquals(after, removeTopicRules(after, "space"))
        assertEquals(before.nasaReviews, removeTopicRules(before, "custom").nasaReviews)
    }

    @Test fun removingSourceChangesOnlyTheSelectedSource() {
        val before = configured()
        val after = removeSourceRules(before, "shared")
        assertEquals(before.copy(sources = listOf(before.sources.last())), after)
        assertEquals(before, removeSourceRules(before, "unknown"))
    }

    @Test fun resettingOneGroupPreservesOtherRulesAndAuthentication() {
        val before = configured()
        assertEquals(before.copy(blocked = FamilySettings().blocked), resetRules(before, RuleGroup.BLOCKED))
        assertEquals(before.copy(sources = starterSources()), resetRules(before, RuleGroup.WEBSITES))
        val topics = resetRules(before, RuleGroup.TOPICS)
        assertEquals(starterCategories(), topics.categories)
        assertTrue(topics.nasaReviews.isEmpty())
        assertTrue(topics.channels.isEmpty())
        assertFalse(topics.sources.any { "custom" in it.categories })
        assertFalse(topics.sources.any { it.enabled })
        assertEquals(before.pinHash, topics.pinHash)
        assertEquals(before.attempts, topics.attempts)
        assertEquals(before.blocked, topics.blocked)
        assertEquals(before.requests, topics.requests)
    }

    @Test fun recommendedDefaultsRevokeApprovalsWithoutErasingPinThrottleOrRequests() {
        val before = configured()
        val after = resetRules(before, RuleGroup.ALL)
        assertEquals(starterCategories(), after.categories)
        assertFalse(after.categories.any { it.enabled })
        assertEquals(starterSources(), after.sources)
        assertFalse(after.sources.any { it.enabled })
        assertEquals(FamilySettings().blocked, after.blocked)
        assertTrue(after.channels.isEmpty())
        assertTrue(after.nasaReviews.isEmpty())
        assertEquals(before.pinHash, after.pinHash)
        assertEquals(before.attempts, after.attempts)
        assertEquals(before.requests, after.requests)
        assertEquals(before.nasaVideos, after.nasaVideos)
        assertEquals(after, resetRules(after, RuleGroup.ALL))
    }

    @Test fun addingMissingSearchSourcesRequiresRoomAndNeverOverwritesExistingApproval() {
        val existing = BrowserSource("vikidia", "Parent's existing source", "custom.example.org", setOf("custom"), enabled = true)
        val before = configured().copy(sources = listOf(existing))
        val after = addSupportedSearchSources(before)
        assertEquals(existing, after.sources.first())
        assertEquals(listOf("vikidia", "wikijunior"), after.sources.map { it.id })
        assertFalse(after.sources.last().enabled)
        assertEquals(before.copy(sources = after.sources), after)
        assertEquals(after, addSupportedSearchSources(after))
        val full = before.copy(sources = (1..29).map { BrowserSource("custom-$it", "Custom $it", "custom.example.org", setOf("custom")) })
        assertThrows(IllegalArgumentException::class.java) { addSupportedSearchSources(full) }
        assertEquals(29, full.sources.size)
    }
}
