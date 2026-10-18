package family.safe.video.nasa

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URL
import java.security.cert.Certificate
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.FutureTask
import java.nio.file.Files
import javax.net.ssl.HttpsURLConnection

/** Deterministic transport fixtures; these do not establish live Android/network behavior. */
class NasaNetworkReliabilityTest {
    private val asset = "https://images-assets.nasa.gov/video/mission/mission~mobile.mp4"
    private val search = """{"collection":{"items":[{"data":[{"center":"JPL","media_type":"video","nasa_id":"mission","title":"Planet lesson","description":"An educational animation"}]}]}}"""
    private val manifest = """{"collection":{"items":[{"href":"$asset"}]}}"""

    @Test fun discoverySharesOneDeadlineAcrossSearchAndManifestAndCannotContinueRequests() {
        val clock = AtomicLong()
        NasaOperation(timeoutMillis=1000, clock=clock::get).use { operation ->
            val requests = mutableListOf<String>()
            var probes = 0
            val client = NasaClient(enabled=true, operation=operation, assetProbe={ probes++; true }) { url ->
                requests.add(url)
                clock.addAndGet(if ("/search?" in url) 600_000_000 else 400_000_000)
                NasaResponse(200, if ("/search?" in url) search else manifest)
            }
            assertThrows(NasaOperationDeadline::class.java) { client.discover("space","space",emptySet()) }
            assertEquals(2,requests.size)
            assertEquals(0,probes)
            assertThrows(NasaOperationDeadline::class.java) { client.mobileAsset("mission") }
            assertEquals(2,requests.size)
        }
    }

    @Test fun cancellationDuringProbeCannotReturnAnApprovalCandidate() {
        NasaOperation().use { operation ->
            var requests = 0
            val client = NasaClient(enabled=true, operation=operation, assetProbe={ operation.cancel(); true }) { url ->
                requests++
                NasaResponse(200,if ("/search?" in url) search else manifest)
            }
            assertThrows(NasaOperationCancelled::class.java) { client.discover("space","space",emptySet()) }
            assertEquals(2,requests)
        }
    }

    @Test fun slowTrickleCannotResetOverallDeadlineOrWriteBytesReadAfterExpiry() {
        val clock = AtomicLong()
        val connection = FakeConnection(length=5, body=object : InputStream() {
            private var reads = 0
            override fun read(): Int = error("Bulk reads expected")
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                if (reads++ == 5) return -1
                clock.addAndGet(400_000_000)
                buffer[offset]=1
                return 1
            }
        })
        val output = ByteArrayOutputStream()
        NasaOperation(timeoutMillis=1000, clock=clock::get).use { operation ->
            assertThrows(NasaOperationDeadline::class.java) {
                downloadNasaVideo("mission",asset,output,operation) { connection }
            }
            assertEquals(2,output.size())
            assertTrue(connection.disconnected)
            assertFalse(connection.instanceFollowRedirects)
        }
    }

    @Test fun cancellationDisconnectsAnActiveConnectionAndForbidsLaterNetwork() {
        val operation = NasaOperation()
        val disconnected = CountDownLatch(1)
        val connection = FakeConnection(onDisconnect={ disconnected.countDown() })
        try {
            assertThrows(NasaOperationCancelled::class.java) {
                operation.withConnection(connection) {
                    assertFalse(connection.disconnected)
                    operation.cancel()
                    assertTrue("Cancelled transport was not disconnected",disconnected.await(1,TimeUnit.SECONDS))
                    assertTrue(connection.disconnected)
                    assertThrows(NasaOperationCancelled::class.java) { operation.check() }
                }
            }
            var opened = false
            assertThrows(NasaOperationCancelled::class.java) {
                downloadNasaVideo("mission",asset,ByteArrayOutputStream(),operation) { opened=true; FakeConnection() }
            }
            assertFalse(opened)
        } finally { operation.close() }
    }

    @Test fun cancellationReturnsAndDeniesResultsWhileDisconnectBlocksThenThrows() {
        val operation = NasaOperation()
        val disconnectStarted = CountDownLatch(1)
        val releaseDisconnect = CountDownLatch(1)
        val disconnectFinished = CountDownLatch(1)
        val connection = FakeConnection(onDisconnect={
            disconnectStarted.countDown()
            try {
                releaseDisconnect.await()
                throw java.io.IOException("Fixture cleanup failure")
            } finally { disconnectFinished.countDown() }
        })
        val cancellation = FutureTask<Unit> { operation.cancel() }
        val caller = Thread(cancellation, "nasa-cancel-test")
        var returnedBeforeCleanup = false
        var terminalBeforeCleanup = false
        var cleanupStarted = false
        try {
            assertThrows(NasaOperationCancelled::class.java) {
                operation.withConnection(connection) {
                    caller.start()
                    try {
                        returnedBeforeCleanup = runCatching { cancellation.get(1,TimeUnit.SECONDS) }.isSuccess
                        cleanupStarted = disconnectStarted.await(1,TimeUnit.SECONDS)
                        terminalBeforeCleanup = runCatching { operation.check() }.exceptionOrNull() is NasaOperationCancelled
                    } finally { releaseDisconnect.countDown() }
                }
            }
            assertTrue("Cancellation waited for transport cleanup",returnedBeforeCleanup)
            assertTrue("Cleanup did not start",cleanupStarted)
            assertTrue("Operation accepted results while cleanup was blocked",terminalBeforeCleanup)
            assertTrue("Cleanup did not finish",disconnectFinished.await(1,TimeUnit.SECONDS))
            assertThrows(NasaOperationCancelled::class.java) { operation.check() }
        } finally {
            releaseDisconnect.countDown()
            caller.join(1000)
            operation.close()
        }
    }

    @Test fun observedDeadlineDisconnectsActiveConnectionAndCapsPerRequestTimeouts() {
        val clock = AtomicLong()
        val connection = FakeConnection()
        NasaOperation(timeoutMillis=1000, clock=clock::get).use { operation ->
            assertThrows(NasaOperationDeadline::class.java) {
                operation.withConnection(connection) {
                    assertTrue(connection.connectTimeout in 1..1000)
                    assertTrue(connection.readTimeout in 1..1000)
                    clock.set(1_000_000_000)
                    operation.check()
                }
            }
            assertTrue(connection.disconnected)
        }
    }

    @Test fun scheduledWatchdogDisconnectsAndUnblocksAnActiveTransferWithoutPolling() {
        val disconnected = CountDownLatch(1)
        var enteredRead = false
        val connection = FakeConnection(body=object : InputStream() {
            override fun read(): Int = error("Bulk reads expected")
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                enteredRead = true
                // Only disconnect can release this simulated blocked I/O. No check() or sleep
                // drives the deadline: the real scheduled watchdog must do the work.
                assertTrue("Watchdog did not release blocked transport",disconnected.await(2,TimeUnit.SECONDS))
                return -1
            }
        },onDisconnect={ disconnected.countDown() })
        NasaOperation(timeoutMillis=200).use { operation ->
            assertThrows(NasaOperationDeadline::class.java) {
                downloadNasaVideo("mission",asset,ByteArrayOutputStream(),operation) { connection }
            }
            assertTrue("Fixture must reach blocked I/O before expiry",enteredRead)
            assertEquals(0L,disconnected.count)
            assertTrue(connection.disconnected)
        }
    }

    @Test fun successfulFailedAndRedirectedDownloadsAlwaysDisconnectWithoutFallback() {
        val success = FakeConnection()
        NasaOperation().use { operation ->
            val output = ByteArrayOutputStream()
            downloadNasaVideo("mission",asset,output,operation) { success }
            assertArrayEquals(byteArrayOf(1),output.toByteArray())
            assertTrue(success.disconnected)
        }
        val invalid = listOf(FakeConnection(status=302), FakeConnection(type="text/html"),
            FakeConnection(length=-1), FakeConnection(length=0), FakeConnection(length=32L*1024*1024+1),
            FakeConnection(length=2), FakeConnection(body=ByteArrayInputStream(byteArrayOf())),
            FakeConnection(body=object : InputStream() { override fun read(): Int = throw java.io.IOException("fixture") }))
        invalid.forEach { connection ->
            NasaOperation().use { operation ->
                var opens = 0
                assertThrows(Exception::class.java) {
                    downloadNasaVideo("mission",asset,ByteArrayOutputStream(),operation) { opens++; connection }
                }
                assertEquals(1,opens)
                assertTrue(connection.disconnected)
                assertFalse(connection.instanceFollowRedirects)
            }
        }
    }

    @Test fun cancellationDuringReadDiscardsItsBytesAndStopsTheTransfer() {
        val operation = NasaOperation()
        var reads = 0
        val connection = FakeConnection(body=object : InputStream() {
            override fun read(): Int = error("Bulk reads expected")
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                reads++
                operation.cancel()
                buffer[offset]=1
                return 1
            }
        })
        val output = ByteArrayOutputStream()
        try {
            assertThrows(NasaOperationCancelled::class.java) {
                downloadNasaVideo("mission",asset,output,operation) { connection }
            }
            assertEquals(1,reads)
            assertEquals(0,output.size())
            assertTrue(connection.disconnected)
        } finally { operation.close() }
    }

    @Test fun zeroProgressCannotSpinAndDeadlineAtEofCannotReportSuccess() {
        val clock = AtomicLong()
        NasaOperation(timeoutMillis=1000,clock=clock::get).use { operation ->
            var reads = 0
            val noProgress = object : InputStream() {
                override fun read(): Int = error("Bulk reads expected")
                override fun read(buffer: ByteArray, offset: Int, length: Int): Int { reads++; return 0 }
            }
            val stalled = FakeConnection(body=noProgress)
            val output = ByteArrayOutputStream()
            assertThrows(IllegalArgumentException::class.java) { downloadNasaVideo("mission",asset,output,operation) { stalled } }
            assertEquals(1,reads)
            assertEquals(0,output.size())
            assertTrue(stalled.disconnected)
            val lateEof = object : InputStream() {
                private var reads = 0
                override fun read(): Int = error("Bulk reads expected")
                override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                    if (reads++ == 0) { buffer[offset]=1; return 1 }
                    clock.set(1_000_000_000)
                    return -1
                }
            }
            assertThrows(NasaOperationDeadline::class.java) { copyBounded(lateEof,ByteArrayOutputStream(),10,operation) }
        }
    }

    @Test fun digestCannotAuthorizeCancelledExpiredOrChangedBytes() {
        val file = Files.createTempFile("nasa-budget-digest", ".mp4").toFile()
        try {
            file.writeBytes(byteArrayOf(1,2,3))
            val expected = verifiedNasaDigest(file,null)
            NasaOperation().use { operation ->
                assertEquals(expected,verifiedNasaDigest(file,expected,operation))
                file.writeBytes(byteArrayOf(1,2,4))
                assertThrows(IllegalArgumentException::class.java) { verifiedNasaDigest(file,expected,operation) }
                operation.cancel()
                assertThrows(NasaOperationCancelled::class.java) { verifiedNasaDigest(file,null,operation) }
            }
            val clock = AtomicLong()
            NasaOperation(timeoutMillis=1000,clock=clock::get).use { operation ->
                clock.set(1_000_000_000)
                assertThrows(NasaOperationDeadline::class.java) { verifiedNasaDigest(file,null,operation) }
            }
        } finally { file.delete() }
    }

    private class FakeConnection(
        private val status: Int = 200,
        private val type: String = "video/mp4",
        private val length: Long = 1,
        private val body: InputStream = ByteArrayInputStream(byteArrayOf(1)),
        private val onDisconnect: () -> Unit = {}
    ) : HttpsURLConnection(URL("https://images-assets.nasa.gov")) {
        @Volatile var disconnected = false
        override fun getResponseCode() = status
        override fun getContentType() = type
        override fun getContentLengthLong() = length
        override fun getInputStream() = body
        override fun disconnect() { disconnected=true; onDisconnect() }
        override fun usingProxy() = false
        override fun connect() = Unit
        override fun getCipherSuite() = "TLS_TEST"
        override fun getLocalCertificates(): Array<Certificate>? = null
        override fun getServerCertificates(): Array<Certificate> = emptyArray()
    }
}
