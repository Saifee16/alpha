package family.safe.video.nasa

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class NasaDigestTest {
    @Test fun previewDigestBindsChildPlaybackToExactlyReviewedBytes() {
        val file=Files.createTempFile("reviewed-nasa-", ".mp4").toFile()
        try {
            file.writeText("abc")
            val expected="ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
            assertEquals(expected,verifiedNasaDigest(file,null))
            assertEquals(expected,verifiedNasaDigest(file,expected))
            file.writeText("changed asset at the same ID")
            assertThrows(IllegalArgumentException::class.java) { verifiedNasaDigest(file,expected) }
        } finally { file.delete() }
    }
}
