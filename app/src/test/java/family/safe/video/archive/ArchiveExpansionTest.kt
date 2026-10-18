package family.safe.video.archive

import family.safe.video.nasa.NasaResponse
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ArchiveExpansionTest {
    @Test fun fractionsSearchReturnsOnlyMatchingPinnedOriginals() {
        var requested = ""
        val body = """{"response":{"docs":[{"identifier":"KA-converted-WPimvspI0_c","title":"Fractions in lowest terms","mediatype":"movies","collection":["khanacademy"]},{"identifier":"untrusted-fractions","title":"Fractions in lowest terms","mediatype":"movies","collection":["khanacademy"]}]}}"""
        val client = ArchiveClient(true) { url -> requested = java.net.URLDecoder.decode(url,"UTF-8"); NasaResponse(200,body) }
        assertEquals(listOf("archive:KA-converted-WPimvspI0_c"), client.discoverLessons("fractions", emptySet()).map { it.videoId })
        assertTrue(requested.contains("identifier:KA-converted-WPimvspI0_c"))
        assertFalse(requested.contains("identifier:KA-converted-xO_1bYgoQvA"))
        assertEquals(emptyList<ArchiveSnapshot>(), client.discoverLessons("dinosaurs", emptySet()))
    }

    @Test fun blockedFractionLessonIsNotReturned() {
        val client = ArchiveClient(true) { error("Blocked candidate must not reach the network") }
        assertEquals(emptyList<ArchiveSnapshot>(),client.discoverLessons("lowest",setOf("fractions")))
        assertThrows(IllegalArgumentException::class.java) { client.discoverLessons("fractions",setOf("fractions")) }
    }

    @Test fun multiplicationResolutionPinsItsOwnFileAndRejectsSubstitutedSnapshot() {
        val body = JSONObject("""{"d1":"dn600307.us.archive.org","dir":"/0/items/KA-converted-xO_1bYgoQvA","metadata":{"identifier":"KA-converted-xO_1bYgoQvA","title":"Multiplication 2: The Multiplication Tables","mediatype":"movies","collection":["khanacademy"]},"files":[{"name":"xO_1bYgoQvA.mp4","source":"original","size":"14071815","md5":"66765e0a7443b71e211c634d4839344a","sha1":"de63aeaae86d5ccee7e19e0d4d375e42b5e45ff1"}]}""")
        val client = ArchiveClient(true) { NasaResponse(200,body.toString()) }
        assertEquals("https://dn600307.us.archive.org/0/items/KA-converted-xO_1bYgoQvA/xO_1bYgoQvA.mp4", client.assetUrl("archive:KA-converted-xO_1bYgoQvA"))
        body.getJSONArray("files").getJSONObject(0).put("sha1",ArchiveLesson.SHA1)
        assertThrows(IllegalArgumentException::class.java) { client.assetUrl("archive:KA-converted-xO_1bYgoQvA") }
        assertThrows(IllegalArgumentException::class.java) { client.assetUrl("archive:untrusted-fractions") }
    }
}
