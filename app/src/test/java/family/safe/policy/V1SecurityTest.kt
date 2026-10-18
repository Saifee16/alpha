package family.safe.policy

import family.safe.browser.BrowserSource
import family.safe.browser.UrlPolicy
import family.safe.search.EducationalResponse
import family.safe.search.EducationalSearchClient
import family.safe.search.EducationalSource
import org.junit.Assert.*
import org.junit.Test

class V1SecurityTest {
    @Test fun selectedWikiArticleCannotExpandToAnotherArticleThroughSubpagesOrRedirects() {
        val article = "https://en.vikidia.org/wiki/Football"
        val scoped = BrowserSource("vikidia", "Vikidia", "en.vikidia.org", setOf("sports"),
            listOf("/wiki/Football"), enabled = true, userGenerated = true)
        assertTrue(UrlPolicy.allows(article, scoped))
        assertTrue(UrlPolicy.allows("$article#Rules", scoped))
        listOf("$article/Gambling", "$article/Another_article", "https://en.vikidia.org/wiki/Gambling").forEach {
            assertFalse(it, UrlPolicy.allows(it, scoped))
            assertNull(it, UrlPolicy.redirectUrl(article, it, scoped))
        }
    }

    @Test fun providerIdentityAndNamespaceCannotBeSubstitutedForParentApprovedSource() {
        val source = BrowserSource("wikijunior", "Wikijunior", "en.wikibooks.org", setOf("sports"),
            listOf("/wiki"), enabled = true, userGenerated = true)
        val article = "https://en.wikibooks.org/wiki/Wikijunior:Football"
        assertTrue(UrlPolicy.allows(article, source))
        assertFalse(UrlPolicy.allows(article, source.copy(id = "vikidia")))
        assertFalse(UrlPolicy.allows(article, source.copy(userGenerated = false)))
        val client = EducationalSearchClient { EducationalResponse(200,
            """{"query":{"search":[{"ns":0,"pageid":1,"title":"Wikijunior:Football"},{"ns":110,"pageid":2,"title":"Football"},{"ns":110,"pageid":3,"title":"Wikijunior talk:Football"},{"ns":110,"pageid":4,"title":"Wikijunior:Football"}]}}""") }
        assertEquals(listOf(article), client.search(EducationalSource.WIKIJUNIOR, "football", emptySet()).map { it.url })
    }

    @Test fun naturalQueryCanonicalNicheStillRequiresEveryTitleTokenAndRejectsBlockedResults() {
        val categories = listOf(Category("sports", "Sports", setOf("football passing drills"), true))
        val decision = QueryPolicy.evaluate("how to football passing drills for beginners", categories, setOf("gambling"))
        assertTrue(decision.allowed)
        assertEquals("football passing drills", decision.searchPhrase)
        assertFalse(QueryPolicy.evaluate("how to football passing drills and gaming for beginners", categories, emptySet()).allowed)
        val client = EducationalSearchClient { EducationalResponse(200,
            """{"query":{"search":[{"ns":0,"pageid":1,"title":"Football passing drills","url":"https://evil.example/"},{"ns":0,"pageid":2,"title":"Football drills"},{"ns":0,"pageid":3,"title":"Football passingly drills"},{"ns":0,"pageid":4,"title":"Football passing drills gambling"}]}}""") }
        val results = client.search(EducationalSource.VIKIDIA, requireNotNull(decision.searchPhrase), setOf("gambling"))
        assertEquals(listOf("Football passing drills"), results.map { it.title })
        assertEquals("https://en.vikidia.org/wiki/Football_passing_drills", results.single().url)
    }
}
