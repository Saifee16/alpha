package family.safe.video

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class YouTubeClientTest {
    private val channel = TrustedChannel("UC" + "a".repeat(22), "Science", setOf("science"))

    @Test fun disabledAndMissingCredentialsNeverCallNetwork() = runBlocking {
        var calls = 0
        val transport: (String, Map<String, String>) -> ApiResponse = { _, _ -> calls++; error("network") }
        assertReason(VideoError.DISABLED) { YouTubeClient(transport = transport).resolveChannels("science", "key") }
        assertReason(VideoError.MISSING_KEY) { YouTubeClient(enabled = true, transport = transport).resolveChannels("science", "") }
        assertEquals(0, calls)
    }

    @Test fun resolvesAndVerifiesActualChannelIdentity() = runBlocking {
        val endpoints = mutableListOf<String>()
        val client = YouTubeClient(enabled = true, transport = { endpoint, parameters ->
            endpoints += endpoint
            if (endpoint == "search") {
                assertEquals("channel", parameters["type"])
                ApiResponse(200, """{"items":[{"id":{"kind":"youtube#channel","channelId":"${channel.id}"}}]}""")
            } else {
                assertEquals(channel.id, parameters["id"])
                ApiResponse(200, """{"items":[{"id":"${channel.id}","snippet":{"title":"Actual identity","description":"About us"}}]}""")
            }
        })
        val candidate = client.resolveChannels("science", "key").single()
        assertEquals("Actual identity", candidate.name)
        assertEquals("https://www.youtube.com/channel/${channel.id}", candidate.url)
        assertEquals(listOf("search", "channels"), endpoints)
    }

    @Test fun enforcesChannelMetadataDurationAndAvailability() = runBlocking {
        val records = listOf(
            video("valid000001"), video("short000001", duration = "PT3M"),
            video("other000001", channelId = "UC" + "b".repeat(22)),
            video("topic000001", title = "unrelated"),
            video("block000001", description = "b a d"),
            video("embed000001", embeddable = false),
            video("age00000001", rating = "ytAgeRestricted"),
            video("local000001", region = "\"regionRestriction\":{\"allowed\":[\"US\"]},"),
            video("miss0000001", duration = ""),
            video("shorts00001", description = "#shorts"),
            video("private0001", privacy = "private")
        )
        val ids = listOf("valid000001", "short000001", "other000001", "topic000001", "block000001", "embed000001", "age00000001", "local000001", "miss0000001", "shorts00001", "private0001")
        val client = YouTubeClient(enabled = true, metadataAllowed = { it == "science" }, transport = { endpoint, p ->
            if (endpoint == "search") {
                assertEquals(channel.id, p["channelId"])
                assertEquals("strict", p["safeSearch"])
                assertEquals("video", p["type"])
                assertEquals("true", p["videoEmbeddable"])
                ApiResponse(200, """{"items":[${ids.joinToString { """{"id":{"kind":"youtube#video","videoId":"$it"},"snippet":{"channelId":"${channel.id}"}}""" }}]}""")
            } else ApiResponse(200, """{"items":[${records.joinToString()}]}""")
        })
        assertEquals(listOf("valid000001"), client.search("science", listOf(channel), "key", setOf("bad")).map { it.id })
    }

    @Test fun unknownQueryAndDefaultMetadataPolicyFailClosed() = runBlocking {
        val client = YouTubeClient(enabled = true, transport = { _, _ -> error("must not request") })
        assertReason(VideoError.QUERY_DENIED) { client.search("science", listOf(channel), "key", emptySet()) }
    }

    @Test fun quotaAndMalformedResponseHaveClearErrors() = runBlocking {
        val quota = YouTubeClient(enabled = true, transport = { _, _ -> ApiResponse(403, """{"error":{"errors":[{"reason":"quotaExceeded"}]}}""") })
        assertReason(VideoError.QUOTA) { quota.resolveChannels("science", "key") }
        val malformed = YouTubeClient(enabled = true, transport = { _, _ -> ApiResponse(200, "broken") })
        assertReason(VideoError.INVALID_RESPONSE) { malformed.resolveChannels("science", "key") }
        val throttled = YouTubeClient(enabled = true, transport = { _, _ -> ApiResponse(429, "not json") })
        assertReason(VideoError.QUOTA) { throttled.resolveChannels("science", "key") }
        val forbidden = YouTubeClient(enabled = true, transport = { _, _ -> ApiResponse(403, "not json") })
        assertReason(VideoError.CREDENTIALS) { forbidden.resolveChannels("science", "key") }
        val permission = YouTubeClient(enabled = true, transport = { _, _ -> throw SecurityException("network denied") })
        assertReason(VideoError.NETWORK) { permission.resolveChannels("science", "key") }
    }

    @Test fun durationParsingIsStrict() {
        assertEquals(3661L, durationSeconds("PT1H1M1S"))
        assertEquals(86400L, durationSeconds("P1D"))
        assertNull(durationSeconds("PT"))
        assertNull(durationSeconds("P1DT"))
        assertNull(durationSeconds("-PT5M"))
        assertNull(durationSeconds("PT999999999999999999999999H"))
    }

    @Test fun requestsAreBoundedAndNoSourcesMeansNoFallback() = runBlocking {
        var calls = 0
        val client = YouTubeClient(enabled = true, metadataAllowed = { true }, transport = { endpoint, _ ->
            assertEquals("search", endpoint)
            calls++
            ApiResponse(200, """{"nextPageToken":"must-not-follow","items":[]}""")
        })
        assertTrue(client.search("science", emptyList(), "key", emptySet()).isEmpty())
        assertEquals(0, calls)
        val channels = (1..8).map { channel.copy(id = "UC" + it.toString().repeat(22)) }
        assertTrue(client.search("science", channels, "key", emptySet()).isEmpty())
        assertEquals(3, calls)
        assertReason(VideoError.INVALID_INPUT) {
            client.search("science", listOf(channel.copy(categories = emptySet())), "key", emptySet())
        }
    }

    @Test fun expiredOrFutureDatedApprovalNeverMakesRequests() = runBlocking {
        val client = YouTubeClient(enabled = true, metadataAllowed = { true }, transport = { _, _ -> error("no network") })
        val stale = channel.copy(approvedAt = System.currentTimeMillis() - 31L * 24 * 60 * 60 * 1000)
        val future = channel.copy(approvedAt = System.currentTimeMillis() + 24L * 60 * 60 * 1000)
        assertTrue(client.search("science", listOf(stale, future), "key", emptySet()).isEmpty())
    }

    @Test fun androidHeadersUsePackageAndSha1OfInstalledCertificate() {
        val headers = androidRequestHeaders("family.safe", "abc".toByteArray(Charsets.UTF_8))
        assertEquals("family.safe", headers["X-Android-Package"])
        assertEquals("A9993E364706816ABA3E25717850C26C9CD0D89D", headers["X-Android-Cert"])
    }

    private suspend fun assertReason(reason: VideoError, block: suspend () -> Unit) {
        try { block(); fail("expected $reason") } catch (e: VideoApiException) { assertEquals(reason, e.reason) }
    }

    private fun video(id: String, channelId: String = channel.id, title: String = "science", description: String = "learning", duration: String = "PT5M", embeddable: Boolean = true, rating: String = "", region: String = "", privacy: String = "public") =
        """{"id":"$id","snippet":{"channelId":"$channelId","title":"$title","description":"$description","liveBroadcastContent":"none"},"contentDetails":{$region"duration":"$duration","contentRating":{"ytRating":"$rating"}},"status":{"privacyStatus":"$privacy","uploadStatus":"processed","embeddable":$embeddable}}"""
}
