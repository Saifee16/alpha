package family.safe

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import family.safe.browser.UrlPolicy
import family.safe.browser.fetchRestricted
import family.safe.data.starterSources
import family.safe.search.EducationalResult
import family.safe.search.EducationalSearchClient
import family.safe.search.EducationalSource
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Opt-in official API/reader transport check; no settings writes or content approval. */
@Suppress("DEPRECATION")
@RunWith(AndroidJUnit4::class)
class EducationalSearchDeviceTest {
    @Test(timeout = 240_000) fun officialSearchReturnsRelevantSafeArticlesAndReaderHtml() {
        assumeTrue("Opt in with -e educationalLive true", InstrumentationRegistry.getArguments().getString("educationalLive") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val evidence = JSONObject().put("kind", "live official API and restricted HTML transport")
            .put("visualReview", "NOT RUN").put("contentApproval", false)
        val searches = JSONArray()
        evidence.put("searches", searches)
        val client = EducationalSearchClient()
        var animal: EducationalResult? = null
        var junior: EducationalResult? = null
        try {
            for (source in EducationalSource.entries) {
                val configured = starterSources().single { it.id == source.id }
                assertFalse("Sources must start disabled", configured.enabled)
                assertTrue("Community editing must remain explicit", configured.userGenerated)
                assertEquals(source.domain, configured.domain)
                val enabled = configured.copy(enabled = true)
                for (query in listOf("animals", "science", "mathematics", "geography", "football")) {
                    val entry = JSONObject().put("source", source.id).put("query", query)
                    searches.put(entry)
                    val results = client.search(source, query, emptySet())
                    entry.put("results", JSONArray().apply {
                        results.forEach { put(JSONObject().put("title", it.title).put("url", it.url)) }
                    })
                    assertTrue("At most five results per source", results.size <= 5)
                    if (source == EducationalSource.VIKIDIA || query in setOf("animals", "science", "mathematics")) {
                        assertTrue("No relevant live ${source.id} results for $query", results.isNotEmpty())
                    }
                    // Recorded Wikijunior geography/football hits were country chapters, not title matches.
                    results.forEach { result ->
                        assertEquals(source, result.source)
                        assertTrue(source.acceptsUrl(result.url))
                        assertTrue("Production URL policy denied ${result.url}", UrlPolicy.allows(result.url, enabled))
                        val topic = when (query) { "animals" -> "animal"; "mathematics" -> "mathematics"; else -> query }
                        assertTrue("Irrelevant title: ${result.title}", Regex("(?i)(?<![A-Za-z])${Regex.escape(topic)}s?(?![A-Za-z])").containsMatchIn(result.title))
                    }
                    if (source == EducationalSource.VIKIDIA && query == "animals") animal = results.first()
                    if (source == EducationalSource.WIKIJUNIOR && query == "animals") junior = results.first()
                }
            }
            val readers = JSONArray()
            evidence.put("readers", readers)
            for (selected in listOf(requireNotNull(animal), requireNotNull(junior))) {
            val source = starterSources().single { it.id == selected.source.id }.copy(enabled = true)
            val response = fetchRestricted(selected.url, source)
            readers.put(JSONObject().put("url", selected.url).put("status", response.statusCode).put("mime", response.mimeType))
            assertEquals(200, response.statusCode)
            assertEquals("text/html", response.mimeType)
            assertTrue(response.responseHeaders["Content-Security-Policy"].orEmpty().contains("script-src 'none'"))
            val html = response.data.use { it.readBytes().toString(Charsets.UTF_8) }
            assertTrue("Expected server-rendered article body", html.contains("mw-parser-output") && html.contains("<h1"))
            assertTrue("Reader attribution is required", html.contains("contributors, revision") && html.contains("CC BY-SA"))
            }
            evidence.put("passed", true)
        } catch (failure: Throwable) {
            evidence.put("passed", false).put("failure", "${failure.javaClass.simpleName}: ${failure.message}")
            throw failure
        } finally {
            client.cancel()
            val directory = requireNotNull(context.getExternalFilesDir("evidence")).apply { mkdirs() }
            File(directory, "v1-source-results.json").writeText(evidence.toString(2))
        }
    }
}
