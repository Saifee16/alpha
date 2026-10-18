package family.safe.video.archive

import family.safe.data.FamilySettings
import family.safe.video.nasa.NasaOperation
import family.safe.video.nasa.NasaOperationCancelled
import family.safe.video.nasa.NasaResponse
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URL
import java.security.cert.Certificate
import javax.net.ssl.HttpsURLConnection

class ArchiveClientTest {
    private val asset = "https://dn601208.us.archive.org/0/items/${ArchiveLesson.ITEM_ID}/${ArchiveLesson.FILE_NAME}"
    private fun search(identifier: String = ArchiveLesson.ITEM_ID, title: String = ArchiveLesson.TITLE) =
        """{"responseHeader":{"status":0},"response":{"docs":[{"identifier":"$identifier","title":"$title","mediatype":"movies","collection":["khanacademy"]}]}}"""
    private fun metadata() = JSONObject("""{"d1":"dn601208.us.archive.org","dir":"/0/items/${ArchiveLesson.ITEM_ID}","metadata":{"identifier":"${ArchiveLesson.ITEM_ID}","title":"${ArchiveLesson.TITLE}","mediatype":"movies","collection":["khanacademy"]},"files":[{"name":"${ArchiveLesson.FILE_NAME}","source":"original","size":"${ArchiveLesson.BYTES}","md5":"${ArchiveLesson.MD5}","sha1":"${ArchiveLesson.SHA1}"}]}""")

    @Test fun disabledClientMakesNoRequestsOrDownloads() {
        val client = ArchiveClient(transport = { error("No request allowed") })
        assertThrows(IllegalStateException::class.java) { client.discover("addition", emptySet()) }
        assertThrows(IllegalStateException::class.java) { client.assetUrl() }
        assertThrows(IllegalStateException::class.java) { client.download(asset, ByteArrayOutputStream()) { error("No connection allowed") } }
    }

    @Test fun discoveryUsesOneFixedIdentifierCollectionAndMatchingTitleQuery() {
        var requested = ""
        val client = ArchiveClient(true) { url -> requested = url; NasaResponse(200, search()) }
        assertTrue(client.discover("addition", emptySet()))
        val decoded = java.net.URLDecoder.decode(requested, "UTF-8")
        assertTrue(decoded.startsWith("https://archive.org/advancedsearch.php?q="))
        assertTrue(decoded.contains("identifier:${ArchiveLesson.ITEM_ID}"))
        assertTrue(decoded.contains("collection:khanacademy AND mediatype:movies AND title:(addition)"))
        assertTrue(decoded.endsWith("&rows=1&page=1&output=json"))
        assertFalse(client.discover("addition drills", emptySet()))
        assertFalse(client.discover("add", emptySet()))
        assertFalse(client.discover("geography", emptySet()))
    }

    @Test fun discoveryRevalidatesIdentityAndNeverApprovesOtherIdsOrMetadata() {
        listOf(search("other"), search(title = "Addition 3 remix"), search().replace("movies", "audio"), search().replace("khanacademy", "opensource_movies")).forEach { body ->
            assertFalse(ArchiveClient(true) { NasaResponse(200, body) }.discover("addition", emptySet()))
        }
        assertFalse(ArchiveClient(true) { NasaResponse(200, """{"response":{"docs":[]}}""") }.discover("addition", emptySet()))
    }

    @Test fun blockedAndInvalidQueriesFailBeforeTransport() {
        val client = ArchiveClient(true) { error("No request allowed") }
        assertThrows(IllegalArgumentException::class.java) { client.discover("addition", setOf("addition")) }
        assertThrows(IllegalArgumentException::class.java) { client.discover("addition", setOf("khan academy")) }
        listOf("", " ", "addition\n3", "x".repeat(257)).forEach {
            assertThrows(IllegalArgumentException::class.java) { client.discover(it, emptySet()) }
        }
    }

    @Test fun defaultContentBlocksDoNotBlockParentOnlyRightsWarning() {
        assertTrue(ArchiveLesson.RIGHTS_NOTE.contains("adult"))
        assertTrue(ArchiveClient(true) { NasaResponse(200, search()) }
            .discover("addition", FamilySettings().blocked))
    }

    @Test fun assetResolutionUsesExactMetadataAndOriginalFileSnapshot() {
        var requested = ""
        val client = ArchiveClient(true) { url -> requested = url; NasaResponse(200, metadata().toString()) }
        assertEquals(asset, client.assetUrl())
        assertEquals("https://archive.org/metadata/${ArchiveLesson.ITEM_ID}", requested)
        val changed = listOf("identifier" to "other", "title" to "Other", "mediatype" to "audio", "collection" to "other")
        changed.forEach { (key, value) ->
            val body = metadata().apply { getJSONObject("metadata").put(key, value) }
            assertThrows(IllegalArgumentException::class.java) { ArchiveClient(true) { NasaResponse(200, body.toString()) }.assetUrl() }
        }
        listOf("name" to "other.mp4", "source" to "derivative", "size" to "33554433", "md5" to "0".repeat(32), "sha1" to "0".repeat(40)).forEach { (key, value) ->
            val body = metadata().apply { getJSONArray("files").getJSONObject(0).put(key, value) }
            assertThrows(IllegalArgumentException::class.java) { ArchiveClient(true) { NasaResponse(200, body.toString()) }.assetUrl() }
        }
    }

    @Test fun assetResolutionRejectsHostPathSpoofsAndDuplicateOriginalFile() {
        listOf("evil.test", "dn601208.us.archive.org.evil.test", "user@dn601208.us.archive.org", "dn601208.us.archive.org:443", "dn601208.ca.archive.org").forEach { host ->
            val body = metadata().put("d1", host)
            assertThrows(IllegalArgumentException::class.java) { ArchiveClient(true) { NasaResponse(200, body.toString()) }.assetUrl() }
        }
        listOf("/0/items/other", "/0/items/../${ArchiveLesson.ITEM_ID}", "/0/items/${ArchiveLesson.ITEM_ID}?x=1").forEach { dir ->
            val body = metadata().put("dir", dir)
            assertThrows(IllegalArgumentException::class.java) { ArchiveClient(true) { NasaResponse(200, body.toString()) }.assetUrl() }
        }
        val duplicate = metadata().apply { getJSONArray("files").put(getJSONArray("files").getJSONObject(0)) }
        assertThrows(IllegalArgumentException::class.java) { ArchiveClient(true) { NasaResponse(200, duplicate.toString()) }.assetUrl() }
        val misplacedPath = metadata().put("d1", "dn601208.us.archive.org/0/items/${ArchiveLesson.ITEM_ID}").put("dir", "")
        assertThrows(IllegalArgumentException::class.java) { ArchiveClient(true) { NasaResponse(200, misplacedPath.toString()) }.assetUrl() }
    }

    @Test fun redirectsErrorsMalformedAndOversizedJsonFailClosed() {
        listOf(302, 403, 429, 500).forEach { status ->
            assertThrows(IllegalStateException::class.java) { ArchiveClient(true) { NasaResponse(status, "{}") }.assetUrl() }
        }
        listOf("{}", "invalid", "x".repeat(1_048_577)).forEach { body ->
            assertThrows(Exception::class.java) { ArchiveClient(true) { NasaResponse(200, body) }.assetUrl() }
        }
    }

    @Test fun downloadRejectsInvalidUrlBeforeOpeningConnection() {
        listOf(asset.replace("https", "http"), "$asset?x=1", "$asset#x", asset.replace(".us.archive.org", ".us.archive.org.evil.test"), asset.replace("/0/", "/../"), asset.replace(ArchiveLesson.FILE_NAME, "other.mp4"), "https://archive.org/download/${ArchiveLesson.ITEM_ID}/${ArchiveLesson.FILE_NAME}").forEach { url ->
            assertThrows(IllegalArgumentException::class.java) { ArchiveClient(true).download(url, ByteArrayOutputStream()) { error("No connection allowed") } }
        }
    }

    @Test fun downloadRequiresExactStatusTypeLengthAndCompleteBoundedCopy() {
        listOf(FakeConnection(asset, status = 302), FakeConnection(asset, type = "text/html"), FakeConnection(asset, length = -1), FakeConnection(asset, length = ArchiveLesson.BYTES + 1)).forEach { connection ->
            assertThrows(Exception::class.java) { ArchiveClient(true).download(asset, ByteArrayOutputStream()) { connection } }
            assertTrue(connection.disconnected)
        }
        val truncated = FakeConnection(asset, input = ByteArrayInputStream(byteArrayOf(1, 2)))
        assertThrows(IllegalArgumentException::class.java) { ArchiveClient(true).download(asset, ByteArrayOutputStream()) { truncated } }
        val oversized = FakeConnection(asset, input = ByteArrayInputStream(ByteArray(ArchiveLesson.BYTES.toInt() + 1)))
        assertThrows(IllegalArgumentException::class.java) { ArchiveClient(true).download(asset, OutputStream.nullOutputStream()) { oversized } }
        val complete = FakeConnection(asset, input = ByteArrayInputStream(ByteArray(ArchiveLesson.BYTES.toInt())))
        ArchiveClient(true).download(asset, OutputStream.nullOutputStream()) { complete }
        assertFalse(complete.instanceFollowRedirects)
        assertFalse(complete.useCaches)
        assertEquals("", complete.getRequestProperty("Cookie"))
        assertTrue(complete.disconnected)
    }

    @Test fun expandedSnapshotDownloadUsesItsOwnLengthAndRejectsMixedItemFile() {
        val url = "https://dn600309.us.archive.org/0/items/KA-converted-WPimvspI0_c/WPimvspI0_c.mp4"
        val wrongLength = FakeConnection(url)
        assertThrows(IllegalArgumentException::class.java) { ArchiveClient(true).download(url, OutputStream.nullOutputStream()) { wrongLength } }
        val complete = FakeConnection(url, length = 4_660_787, input = ByteArrayInputStream(ByteArray(4_660_787)))
        ArchiveClient(true).download(url, OutputStream.nullOutputStream()) { complete }
        assertTrue(complete.disconnected)
        assertThrows(IllegalArgumentException::class.java) {
            ArchiveClient(true).download(url.replace("WPimvspI0_c.mp4", "xO_1bYgoQvA.mp4"), OutputStream.nullOutputStream()) { error("Mixed snapshot must not connect") }
        }
    }

    @Test fun cancelledOperationMakesNoRequestsAndCannotReturnStaleDiscovery() {
        NasaOperation().use { op ->
            op.cancel()
            assertThrows(NasaOperationCancelled::class.java) { ArchiveClient(true, op) { error("No request") }.assetUrl() }
        }
        NasaOperation().use { op ->
            val client = ArchiveClient(true, op) { op.cancel(); NasaResponse(200, search()) }
            assertThrows(NasaOperationCancelled::class.java) { client.discover("addition", emptySet()) }
        }
    }

    @Test fun cancellationDuringDownloadDeniesBytesAndDisconnects() {
        NasaOperation().use { op ->
            val output = ByteArrayOutputStream()
            val input = object : ByteArrayInputStream(byteArrayOf(1)) {
                override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                    op.cancel()
                    return super.read(buffer, offset, length)
                }
            }
            val connection = FakeConnection(asset, input = input)
            assertThrows(NasaOperationCancelled::class.java) { ArchiveClient(true, op).download(asset, output) { connection } }
            assertEquals(0, output.size())
            assertTrue(connection.disconnected)
        }
        val zeroProgress = FakeConnection(asset, input = object : InputStream() {
            override fun read() = 0
            override fun read(buffer: ByteArray, offset: Int, length: Int) = 0
        })
        assertThrows(IllegalArgumentException::class.java) { ArchiveClient(true).download(asset, ByteArrayOutputStream()) { zeroProgress } }
        assertTrue(zeroProgress.disconnected)
    }

    private class FakeConnection(url: String, private val status: Int = 200, private val type: String = "video/mp4", private val length: Long = ArchiveLesson.BYTES, private val input: InputStream = ByteArrayInputStream(byteArrayOf(1))) : HttpsURLConnection(URL(url)) {
        var disconnected = false
        override fun getResponseCode() = status
        override fun getContentType() = type
        override fun getContentLengthLong() = length
        override fun getInputStream() = input
        override fun connect() = Unit
        override fun disconnect() { disconnected = true }
        override fun usingProxy() = false
        override fun getCipherSuite() = "fixture"
        override fun getLocalCertificates(): Array<Certificate>? = null
        override fun getServerCertificates(): Array<Certificate> = emptyArray()
    }
}
