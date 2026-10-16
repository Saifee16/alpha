package family.safe.video.nasa

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URL
import java.security.cert.Certificate
import javax.net.ssl.HttpsURLConnection

class NasaClientTest {
    private val id = NasaReference.ID
    private val path get() = "/video/$id/$id~mobile.mp4"
    private fun json(href: String) = """{"collection":{"items":[{"href":"$href"}]}}"""

    @Test fun disabledMakesNoNetworkRequests() {
        var calls = 0
        val client = NasaClient(transport = { calls++; NasaResponse(200, "{}") })
        assertThrows(IllegalStateException::class.java) { client.spaceCandidates() }
        assertThrows(IllegalStateException::class.java) { client.mobileAsset(id) }
        assertEquals(0, calls)
        assertFalse(NasaReference.CHILD_REVIEWED)
    }

    @Test fun fixedSearchFiltersAndRevalidatesSource() {
        var requested = ""
        val client = NasaClient(true) { url ->
            requested = url
            NasaResponse(200, """{"collection":{"items":[
              {"data":[{"nasa_id":"$id","center":"JPL","media_type":"video","title":"Solar system"}]},
              {"data":[{"nasa_id":"other","center":"HQ","media_type":"video","title":"Solar system"}]},
              {"data":[{"nasa_id":"image","center":"JPL","media_type":"image","title":"Solar system"}]}
            ]}}""")
        }
        assertEquals(listOf(id), client.spaceCandidates().map { it.id })
        assertTrue(requested.contains("q=solar%20system&center=JPL&media_type=video&page_size=5"))
    }

    @Test fun manifestOnlySelectsExactMobileAssetAndUpgradesLegacyHttpWithoutRequestingIt() {
        val client = NasaClient(true) { NasaResponse(200, json("http://images-assets.nasa.gov$path")) }
        assertEquals("https://images-assets.nasa.gov$path", client.mobileAsset(id))
    }

    @Test fun rejectsUntrustedOrAmbiguousManifestLocations() {
        listOf("https://images-assets.nasa.gov.evil.test$path", "https://user@images-assets.nasa.gov$path",
            "https://images-assets.nasa.gov:444$path", "https://images-assets.nasa.gov$path?x=1",
            "https://images-assets.nasa.gov$path#x", "https://images-assets.nasa.gov/video/other/other~mobile.mp4",
            "file://$path", "https://images-assets.nasa.gov/video/$id/../$id~mobile.mp4").forEach { href ->
            assertThrows(IllegalArgumentException::class.java) { NasaClient(true) { NasaResponse(200, json(href)) }.mobileAsset(id) }
        }
    }

    @Test fun rejectsInvalidIdsBeforeTransport() {
        val client = NasaClient(true) { error("must not call") }
        listOf("../evil", "https://evil", "", " ", " x", "x?y", "x%2Fy").forEach {
            assertThrows(IllegalArgumentException::class.java) { client.mobileAsset(it) }
        }
    }

    @Test fun httpErrorsAndMalformedResponsesFailClosed() {
        listOf(302, 404, 429, 500).forEach { status ->
            assertThrows(IllegalStateException::class.java) { NasaClient(true) { NasaResponse(status, "{}") }.spaceCandidates() }
        }
        assertThrows(Exception::class.java) { NasaClient(true) { NasaResponse(200, "invalid") }.spaceCandidates() }
        assertThrows(Exception::class.java) { NasaClient(true) { NasaResponse(200, "{}") }.spaceCandidates() }
        assertThrows(IllegalArgumentException::class.java) { NasaClient(true) { NasaResponse(200, json("https://images-assets.nasa.gov/video/$id/$id~orig.mp4")) }.mobileAsset(id) }
    }

    @Test fun downloadCopyStopsAtSizeLimit() {
        val output = ByteArrayOutputStream()
        assertThrows(IllegalArgumentException::class.java) { copyBounded(ByteArrayInputStream(ByteArray(11)), output, 10) }
        assertEquals(0, output.size())
        copyBounded(ByteArrayInputStream(ByteArray(10)), output, 10)
        assertEquals(10, output.size())
    }

    @Test fun downloaderRejectsRedirectsWrongTypesUnknownLengthAndTruncation() {
        listOf(FakeConnection(302), FakeConnection(type = "text/html"), FakeConnection(length = -1),
            FakeConnection(length = 33L * 1024 * 1024), FakeConnection(length = 2)).forEach { connection ->
            assertThrows(Exception::class.java) {
                downloadNasaReference("https://images-assets.nasa.gov$path", ByteArrayOutputStream()) { connection }
            }
            assertFalse(connection.instanceFollowRedirects)
            assertTrue(connection.disconnected)
        }
    }

    @Test fun downloaderUsesOnlyExactHttpsReferenceAndCompletesLocalCopy() {
        listOf("http://images-assets.nasa.gov$path", "https://images-assets.nasa.gov$path?x=y", "https://evil.test$path").forEach {
            assertThrows(IllegalArgumentException::class.java) { downloadNasaReference(it, ByteArrayOutputStream()) { error("must not open") } }
        }
        val output = ByteArrayOutputStream()
        downloadNasaReference("https://images-assets.nasa.gov$path", output) { FakeConnection() }
        assertArrayEquals(byteArrayOf(1), output.toByteArray())
    }

    private class FakeConnection(private val status: Int = 200, private val type: String = "video/mp4", private val length: Long = 1) :
        HttpsURLConnection(URL("https://images-assets.nasa.gov")) {
        var disconnected = false
        override fun getResponseCode() = status
        override fun getContentType() = type
        override fun getContentLengthLong() = length
        override fun getInputStream() = ByteArrayInputStream(byteArrayOf(1))
        override fun disconnect() { disconnected = true }
        override fun usingProxy() = false
        override fun connect() = Unit
        override fun getCipherSuite() = "TLS_TEST"
        override fun getLocalCertificates(): Array<Certificate>? = null
        override fun getServerCertificates(): Array<Certificate> = emptyArray()
    }
}
