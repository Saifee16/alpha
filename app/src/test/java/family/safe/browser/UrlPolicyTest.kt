package family.safe.browser

import org.junit.Assert.*
import org.junit.Test

class UrlPolicyTest {
    private val source = BrowserSource("test", "Learning", "learn.example.org", setOf("science"), enabled = true)

    @Test fun approvedExactHttpsHostCanBeExplored() {
        listOf("https://learn.example.org", "https://learn.example.org/space/moon", "https://LEARN.example.org/space#moon").forEach {
            assertTrue(it, UrlPolicy.allows(it, source))
        }
    }

    @Test fun unknownAndDeceptiveDestinationsAreDenied() {
        listOf(
            "http://learn.example.org/", "https://sub.learn.example.org/", "https://learn.example.org.evil.org/",
            "https://learn.example.org@evil.org/", "https://evil.org@learn.example.org/",
            "https://learn.example.org:443/", "https://learn.example.org./", "//learn.example.org/",
            "javascript:alert(1)", "intent://learn.example.org/", "file:///etc/passwd", "data:text/html,hello",
            "https://learn.example.org\\@evil.org/", " https://learn.example.org/", "https://learn.example.org/\n"
        ).forEach { assertFalse(it, UrlPolicy.allows(it, source)) }
    }

    @Test fun pathScopeUsesSegmentBoundariesAndRejectsEvasion() {
        val scoped = source.copy(pathPrefixes = listOf("/learn"))
        assertTrue(UrlPolicy.allows("https://learn.example.org/learn", scoped))
        assertTrue(UrlPolicy.allows("https://learn.example.org/learn/moon", scoped))
        listOf("/learning", "/learn/../private", "/learn/./private", "/learn/%2e%2e/private", "/learn/%252e%252e/private", "/learn%2fprivate", "/learn//private", "/learn/shorts/123", "/learn/ReElS").forEach {
            assertFalse(it, UrlPolicy.allows("https://learn.example.org$it", scoped))
        }
    }

    @Test fun disabledMalformedOrUnreviewableSourcesAreDenied() {
        listOf(source.copy(enabled = false), source.copy(domain = "https://learn.example.org"),
            source.copy(domain = "*.example.org"), source.copy(domain = "127.0.0.1"),
            source.copy(domain = "localhost"), source.copy(pathPrefixes = emptyList()),
            source.copy(pathPrefixes = listOf("/../")), source.copy(categories = emptySet()),
            source.copy(userGenerated = true)).forEach {
            assertFalse(it.toString(), UrlPolicy.allows("https://learn.example.org/", it))
        }
    }

    @Test fun socialAndVideoPlatformsCannotBeApprovedAsWebsites() {
        listOf("youtube.com", "www.youtube.com", "youtu.be", "instagram.com", "facebook.com", "m.facebook.com", "tiktok.com", "snapchat.com", "twitter.com", "x.com", "reddit.com").forEach {
            assertFalse(it, UrlPolicy.allows("https://$it/", source.copy(domain = it)))
        }
    }

    @Test fun publicSearchEnginesCannotBecomeAnUnrestrictedSearchFallback() {
        listOf("google.com", "www.google.co.uk", "bing.com", "duckduckgo.com", "search.yahoo.com", "yandex.com", "search.brave.com").forEach {
            assertFalse(it, UrlPolicy.allows("https://$it/", source.copy(domain = it)))
        }
    }

    @Test fun starterUrlRespectsScopeAndInvalidSourcesStayClosed() {
        assertEquals("https://learn.example.org/learn", UrlPolicy.startUrl(source.copy(pathPrefixes = listOf("/learn"))))
        assertNull(UrlPolicy.startUrl(source.copy(enabled = false)))
    }

    @Test fun redirectsAreResolvedAndRecheckedAgainstTheSameSource() {
        val current = "https://learn.example.org/learn/moon"
        val scoped = source.copy(pathPrefixes = listOf("/learn"))
        assertEquals("https://learn.example.org/learn/sun", UrlPolicy.redirectUrl(current, "sun", scoped))
        listOf("https://evil.org/", "//evil.org/", "http://learn.example.org/learn", "/private", "javascript:alert(1)", "/learn/%2e%2e/private").forEach {
            assertNull(it, UrlPolicy.redirectUrl(current, it, scoped))
        }
    }
}
