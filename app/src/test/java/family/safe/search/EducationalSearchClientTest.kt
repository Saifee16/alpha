package family.safe.search

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class EducationalSearchClientTest {
    @Test fun articleUsesOfficialParseAndRequiresExactReturnedIdentity() {
        var requested = ""
        val client = EducationalSearchClient { requested = it; EducationalResponse(200,
            """{"parse":{"title":"Animal","pageid":123,"revid":456,"text":"<div class=\"mw-parser-output\">Animals live here.</div>"}}""") }
        val html = client.article(EducationalSource.VIKIDIA, "https://en.vikidia.org/wiki/Animal")
        assertTrue(requested.contains("action=parse&page=Animal&prop=text%7Crevid"))
        assertFalse(requested.contains("redirects="))
        assertTrue(html.contains("<h1>Animal</h1>"))
        assertTrue(html.contains("Animals live here."))
        assertTrue(html.contains("CC BY-SA 3.0") && html.contains("revision 456"))
        for (body in listOf("{}", """{"parse":{"title":"Gambling","pageid":123,"revid":456,"text":"bad"}}""",
            """{"parse":{"title":"Animal","pageid":0,"revid":456,"text":"bad"}}""")) {
            assertThrows(Exception::class.java) { EducationalSearchClient { EducationalResponse(200, body) }
                .article(EducationalSource.VIKIDIA, "https://en.vikidia.org/wiki/Animal") }
        }
        assertThrows(IllegalArgumentException::class.java) { client.article(EducationalSource.VIKIDIA, "https://evil.test/wiki/Animal") }
    }
    private fun response(items: String) = EducationalResponse(200, """{"query":{"search":[$items]}}""")
    private fun item(title: String, ns: Int = 0, id: Int = 1) =
        """{"ns":$ns,"pageid":$id,"title":"$title"}"""

    @Test fun usesExactOfficialBoundedEndpointAndBuildsUrlFromTitle() {
        var requested = ""
        val client = EducationalSearchClient { url ->
            requested = url
            response(item("Association football") + "," + item("Football", id = 2))
        }
        val results = client.search(EducationalSource.VIKIDIA, "football", emptySet())
        assertEquals(2, results.size)
        assertEquals("https://en.vikidia.org/wiki/Association_football", results.first().url)
        assertEquals("https://en.vikidia.org/w/api.php?action=query&list=search&srsearch=football&srnamespace=0&srlimit=5&srprop=snippet&format=json&formatversion=2", requested)
    }

    @Test fun wikijuniorRequiresItsNamespaceAndSupportsConservativeSubpages() {
        val results = EducationalSearchClient { response(
            item("Wikijunior:Introduction to Mathematics/Multiplying", 110) + "," +
                item("Mathematics", 0, 2) + "," + item("Wikijunior talk:Mathematics", 110, 3)
        ) }.search(EducationalSource.WIKIJUNIOR, "maths", emptySet())
        assertEquals(1, results.size)
        assertTrue(results.single().url.endsWith("/wiki/Wikijunior:Introduction_to_Mathematics/Multiplying"))
    }

    @Test fun allQueryTokensMustMatchWholeTitleWordsWithSimplePluralAndMathAliases() {
        val client = EducationalSearchClient { response(
            item("Animal Kingdom") + "," + item("Animal behaviour", id = 2) + "," +
                item("Animalistic Kingdom", id = 3) + "," + item("Europe", id = 4)
        ) }
        assertEquals(listOf("Animal Kingdom"), client.search(EducationalSource.VIKIDIA, "animals kingdom", emptySet()).map { it.title })
        assertTrue(client.search(EducationalSource.VIKIDIA, "football passing drills", emptySet()).isEmpty())
    }

    @Test fun structuredSnippetsRescueRelevantMultiwordSearchWithoutAcceptingUnrelatedHits() {
        val client = EducationalSearchClient { response(
            """{"ns":0,"pageid":1,"title":"Photosynthesis","snippet":"Plants use sunlight to grow and make food."},""" +
                """{"ns":0,"pageid":2,"title":"Animal","snippet":"Animals eat food."}"""
        ) }
        assertEquals(listOf("Photosynthesis"), client.search(EducationalSource.VIKIDIA,
            "how plants grow", emptySet()).map { it.title })
    }

    @Test fun highlightedAndEncodedSnippetTextIsValidatedBeforeRelevance() {
        val result = EducationalSearchClient { response(
            """{"ns":0,"pageid":1,"title":"Photosynthesis","snippet":"<span class=\"searchmatch\">Plants</span> grow using sunlight."}"""
        ) }.search(EducationalSource.VIKIDIA, "plants grow", emptySet())
        assertEquals("Photosynthesis", result.single().title)
        assertTrue(EducationalSearchClient { response(
            """{"ns":0,"pageid":1,"title":"Plants","snippet":"Plants grow with gam&#98;ling."}"""
        ) }.search(EducationalSource.VIKIDIA, "plants", setOf("gambling")).isEmpty())
    }

    @Test fun blockedSnippetAndOversizedSnippetCannotRescueAResult() {
        val client = EducationalSearchClient { response(
            """{"ns":0,"pageid":1,"title":"Photosynthesis","snippet":"Plants grow with gambling."},""" +
                """{"ns":0,"pageid":2,"title":"Photosynthesis","snippet":"${"x".repeat(4097)} plants grow"}"""
        ) }
        assertTrue(client.search(EducationalSource.VIKIDIA, "plants grow", setOf("gambling")).isEmpty())
    }

    @Test fun blockedQueriesNeverCallTransportAndBlockedTitlesStayHidden() {
        var calls = 0
        val client = EducationalSearchClient { calls++; response(item("Football betting")) }
        assertThrows(IllegalArgumentException::class.java) { client.search(EducationalSource.VIKIDIA, "betting", setOf("betting")) }
        assertEquals(0, calls)
        assertTrue(client.search(EducationalSource.VIKIDIA, "football", setOf("betting")).isEmpty())
    }

    @Test fun rejectsUntrustedTitlesAndIgnoresApiProvidedUrl() {
        val malicious = listOf("User:Football", "Football%2fEdit", "Football?edit", "Football#section", "Football/../Edit", " Football", "Football//Edit", "<b>Football</b>", "Football\\\\Edit")
        malicious.forEach { title -> assertNull(title, EducationalSource.VIKIDIA.articleUrl(title)) }
        val result = EducationalSearchClient { EducationalResponse(200,
            """{"query":{"search":[{"ns":0,"pageid":1,"title":"Football","url":"https://evil.test/"}]}}""")
        }.search(EducationalSource.VIKIDIA, "football", emptySet()).single()
        assertEquals("https://en.vikidia.org/wiki/Football", result.url)
        listOf("https://en.vikidia.org.evil.test/wiki/Football", "https://user@en.vikidia.org/wiki/Football", "https://en.vikidia.org:443/wiki/Football", "http://en.vikidia.org/wiki/Football", "https://en.vikidia.org/wiki/Football?edit=1", "https://en.vikidia.org/wiki/Football#x", "https://en.vikidia.org/w/api.php", "https://en.vikidia.org/wiki/User:Football").forEach {
            assertFalse(it, EducationalSource.VIKIDIA.acceptsUrl(it))
        }
        assertTrue(EducationalSource.VIKIDIA.acceptsUrl(result.url))
    }

    @Test fun invalidMetadataDuplicatesAndResultsBeyondFiveStayHidden() {
        val entries = listOf(item("Football", id = 0), """{"ns":"0","pageid":1,"title":"Football"}""", item("Football", id = 2), item("Football", id = 2), item("Football rules", id = 3), item("Football sixth", id = 4))
        val results = EducationalSearchClient { response(entries.joinToString(",")) }.search(EducationalSource.VIKIDIA, "football", emptySet())
        assertEquals(listOf("Football", "Football rules"), results.map { it.title })
    }

    @Test fun httpErrorsMalformedOrOversizedResponsesFailClosed() {
        listOf(301, 403, 429, 500).forEach { status ->
            assertThrows(IllegalStateException::class.java) { EducationalSearchClient { EducationalResponse(status, "{}") }.search(EducationalSource.VIKIDIA, "science", emptySet()) }
        }
        listOf("invalid", "{}", "{\"error\":{}}", "x".repeat(1048577)).forEach { body ->
            assertThrows(Exception::class.java) { EducationalSearchClient { EducationalResponse(200, body) }.search(EducationalSource.VIKIDIA, "science", emptySet()) }
        }
    }

    @Test fun cancellationAndInvalidQueriesFailBeforeReturningResults() {
        lateinit var client: EducationalSearchClient
        client = EducationalSearchClient { client.cancel(); response(item("Science")) }
        assertThrows(IllegalStateException::class.java) { client.search(EducationalSource.VIKIDIA, "science", emptySet()) }
        listOf("", " ", "x".repeat(257), "science\nedit", "science\u200b").forEach { query ->
            assertThrows(IllegalArgumentException::class.java) { client.search(EducationalSource.VIKIDIA, query, emptySet()) }
        }
    }

    @Test fun recordedLiveTitlesSupportOnlyRelevantSubjectCoverage() {
        // Recorded official API titles; these fixtures do not establish current network availability.
        listOf("animals" to "Animal", "science" to "Science", "maths" to "Mathematics",
            "geography" to "Geography", "football" to "Association football").forEach { (query, title) ->
            assertEquals(title, EducationalSearchClient { response(item(title)) }
                .search(EducationalSource.VIKIDIA, query, emptySet()).single().title)
        }
        assertTrue(EducationalSearchClient { response(item("Wikijunior:Europe/Italy", 110)) }
            .search(EducationalSource.WIKIJUNIOR, "football", emptySet()).isEmpty())
    }

    @Test fun cancelledInFlightSearchCannotReturnAndLaterSearchCanRecover() {
        val entered = CountDownLatch(1)
        val released = CountDownLatch(1)
        val failure = AtomicReference<Throwable?>()
        var calls = 0
        val client = EducationalSearchClient {
            if (++calls == 1) {
                entered.countDown()
                check(released.await(2, TimeUnit.SECONDS))
            }
            response(item("Science"))
        }
        val worker = Thread {
            try { client.search(EducationalSource.VIKIDIA, "science", emptySet()) }
            catch (error: Throwable) { failure.set(error) }
        }.apply { isDaemon = true; start() }
        try {
            assertTrue(entered.await(2, TimeUnit.SECONDS))
            assertThrows(IllegalStateException::class.java) { client.search(EducationalSource.VIKIDIA, "science", emptySet()) }
            client.cancel()
        } finally { released.countDown() }
        worker.join(2_000)
        assertFalse(worker.isAlive)
        assertTrue(failure.get() is IllegalStateException)
        assertEquals("Science", client.search(EducationalSource.VIKIDIA, "science", emptySet()).single().title)
    }
}
