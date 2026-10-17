package family.safe.video.nasa

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.net.URL
import java.security.cert.Certificate
import javax.net.ssl.HttpsURLConnection

class NasaDiscoveryTest {
    private fun record(id: String = "mission", change: (JSONObject) -> Unit = {}): JSONObject {
        val data = JSONObject().put("center", "JPL").put("media_type", "video")
            .put("nasa_id", id).put("title", "Mission animation")
            .put("description", "A spacecraft explores the solar system.")
        change(data)
        return JSONObject().put("data", JSONArray().put(data))
    }
    private fun response(items: List<JSONObject>) = NasaResponse(200,
        JSONObject().put("collection", JSONObject().put("items", JSONArray(items))).toString())
    private fun manifest(id: String, href: String = "https://images-assets.nasa.gov/video/$id/$id~mobile.mp4") =
        response(listOf(JSONObject().put("href", href)))
    private fun client(items: List<JSONObject>, probe: (String) -> Boolean = { true }) =
        NasaClient(enabled = true, assetProbe = probe, transport = { url ->
            if ("/search?" in url) response(items) else manifest(url.substringAfterLast('/'))
        })

    @Test fun searchIsSingleBoundedEncodedRequestAndReturnsUnapprovedCategoryCandidate() {
        val requests = mutableListOf<String>()
        val probes = mutableListOf<String>()
        val client = NasaClient(true, assetProbe = { probes.add(it); true }, transport = { url ->
            requests.add(url)
            if ("/search?" in url) response(listOf(record())) else manifest("mission")
        })
        val candidate = client.discover("space & moon?", "parent-created-123", emptySet()).single()
        assertEquals("https://images-api.nasa.gov/search?q=space+%26+moon%3F&center=JPL&media_type=video&page_size=5", requests.first())
        assertEquals(2, requests.size)
        assertEquals(listOf("https://images-assets.nasa.gov/video/mission/mission~mobile.mp4"), probes)
        assertEquals("parent-created-123", candidate.category)
        assertEquals("NASA/JPL-Caltech", candidate.credit)
        assertTrue(candidate.rightsNote.contains("Parent review required"))
    }

    @Test fun disabledInvalidAndBlockedInputsNeverReachTransport() {
        val client = NasaClient(true, transport = { error("must not call") })
        listOf("", " ", "x".repeat(201), "space\n", "space\u200B", "<space>").forEach { query ->
            assertThrows(IllegalArgumentException::class.java) { client.discover(query, "space", emptySet()) }
        }
        listOf("", "../space", "a b", "x".repeat(101)).forEach { category ->
            assertThrows(IllegalArgumentException::class.java) { client.discover("space", category, emptySet()) }
        }
        assertThrows(IllegalArgumentException::class.java) { client.discover("violent", "space", setOf("violent")) }
        assertThrows(IllegalStateException::class.java) {
            NasaClient(transport = { error("must not call") }).discover("space", "space", emptySet())
        }
    }

    @Test fun malformedWrongSourceAndUnsafeMetadataAreOmitted() {
        val invalid = listOf(
            record { it.put("center", "HQ") }, record { it.put("media_type", "image") },
            record("../mission"), record { it.put("title", "x".repeat(201)) },
            record { it.put("description", "x".repeat(4001)) }, record { it.remove("description") },
            record { it.put("title", "<b>Mission</b>") }, record { it.put("description", "space\u200Bmission") },
            record { it.put("photographer", "x".repeat(301)) },
            record { it.put("photographer", "x".repeat(200)).put("secondary_creator", "y".repeat(200)) },
            JSONObject(), record().apply { getJSONArray("data").put(JSONObject()) }
        )
        invalid.forEach { item ->
            assertTrue(client(listOf(item), probe = { error("must not probe") }).discover("space", "space", emptySet()).isEmpty())
        }
    }

    @Test fun blockedPhrasesAreScannedAcrossMetadataAndCredits() {
        listOf("title", "description", "photographer", "secondary_creator").forEach { key ->
            assertTrue(client(listOf(record { it.put(key, "v.i.o.l.e.n.c.e") }), probe = { error("must not probe") })
                .discover("space", "space", setOf("violence")).isEmpty())
        }
    }

    @Test fun inspectsAtMostFiveRecordsAndDoesNotRepeatDuplicateAssets() {
        var probes = 0
        val items = listOf(record("a"), record("a"), record("b"), record("c"), record("d"), record("e"))
        val result = client(items) { probes++; true }.discover("space", "space", emptySet())
        assertEquals(listOf("a", "b", "c", "d"), result.map { it.id })
        assertEquals(4, probes)
    }

    @Test fun badManifestsAndUnavailableAssetsDoNotBecomeCandidates() {
        listOf("https://evil.test/video/mission/mission~mobile.mp4",
            "https://images-assets.nasa.gov/video/mission/mission~mobile.mp4?redirect=1",
            "https://images-assets.nasa.gov/video/mission/mission~orig.mp4").forEach { href ->
            val client = NasaClient(true, assetProbe = { error("must not probe") }, transport = { url ->
                if ("/search?" in url) response(listOf(record())) else manifest("mission", href)
            })
            assertTrue(client.discover("space", "space", emptySet()).isEmpty())
        }
        assertTrue(client(listOf(record())) { false }.discover("space", "space", emptySet()).isEmpty())
        assertTrue(client(listOf(record())) { throw IllegalStateException("offline") }.discover("space", "space", emptySet()).isEmpty())
        val unavailableManifest = NasaClient(true, transport = { url ->
            if ("/search?" in url) response(listOf(record())) else NasaResponse(503, "")
        })
        assertTrue(unavailableManifest.discover("space", "space", emptySet()).isEmpty())
    }

    @Test fun searchErrorsFailClosedWithoutFallback() {
        listOf(NasaResponse(302, ""), NasaResponse(500, ""), NasaResponse(200, "{}"),
            NasaResponse(200, "x".repeat(1024 * 1024 + 1))).forEach { response ->
            var calls = 0
            val client = NasaClient(true, transport = { calls++; response })
            assertThrows(Exception::class.java) { client.discover("space", "space", emptySet()) }
            assertEquals(1, calls)
        }
        assertThrows(IllegalStateException::class.java) {
            NasaClient(true, transport = { error("offline") }).discover("space", "space", emptySet())
        }
    }

    @Test fun assetProbeUsesHeadWithNoRedirectsAndRequiresKnownBoundedMp4() {
        val url = "https://images-assets.nasa.gov/video/mission/mission~mobile.mp4"
        val good = FakeConnection(length = 32L * 1024 * 1024)
        assertTrue(nasaPlayableAsset(url) { good })
        assertEquals("HEAD", good.requestMethod)
        assertFalse(good.instanceFollowRedirects)
        assertFalse(good.useCaches)
        assertTrue(good.disconnected)
        listOf(FakeConnection(status = 302), FakeConnection(status = 404),
            FakeConnection(type = "text/html"), FakeConnection(length = -1), FakeConnection(length = 0),
            FakeConnection(length = 32L * 1024 * 1024 + 1)).forEach { bad ->
            assertFalse(nasaPlayableAsset(url) { bad })
            assertTrue(bad.disconnected)
            assertFalse(bad.instanceFollowRedirects)
        }
        listOf(url.replace("https:", "http:"), "$url?q=1", url.replace("mission~mobile", "other~mobile"),
            url.replace("images-assets.nasa.gov", "evil.test")).forEach { denied ->
            assertFalse(nasaPlayableAsset(denied) { error("must not open") })
        }
    }

    private class FakeConnection(private val status: Int = 200, private val type: String = "video/mp4", private val length: Long = 1) :
        HttpsURLConnection(URL("https://images-assets.nasa.gov")) {
        var disconnected = false
        override fun getResponseCode() = status
        override fun getContentType() = type
        override fun getContentLengthLong() = length
        override fun disconnect() { disconnected = true }
        override fun usingProxy() = false
        override fun connect() = Unit
        override fun getCipherSuite() = "TLS_TEST"
        override fun getLocalCertificates(): Array<Certificate>? = null
        override fun getServerCertificates(): Array<Certificate> = emptyArray()
    }
}
