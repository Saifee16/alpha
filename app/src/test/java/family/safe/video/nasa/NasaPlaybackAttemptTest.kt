package family.safe.video.nasa

import org.junit.Assert.*
import org.junit.Test

class NasaPlaybackAttemptTest {
    @Test fun preparationTimeoutCannotLaterStartPlayback() {
        val attempts = NasaPlaybackAttempt()
        val token = attempts.begin()
        assertFalse(attempts.timedOut(token)) // Transfer time is independent of native preparation.
        assertTrue(attempts.preparing(token))
        assertTrue(attempts.timedOut(token))
        assertFalse(attempts.prepared(token))
        assertFalse(attempts.isReady(token))
        assertFalse(attempts.timedOut(token))
    }

    @Test fun retryIgnoresEveryCallbackFromThePreviousAttempt() {
        val attempts = NasaPlaybackAttempt()
        val old = attempts.begin()
        assertTrue(attempts.preparing(old))
        assertTrue(attempts.timedOut(old))
        val retry = attempts.begin()
        assertFalse(attempts.preparing(old))
        assertFalse(attempts.prepared(old))
        assertFalse(attempts.timedOut(old))
        assertFalse(attempts.fail(old))
        assertTrue(attempts.preparing(retry))
        assertTrue(attempts.prepared(retry))
        assertTrue(attempts.isReady(retry))
        assertFalse(attempts.timedOut(retry))
    }

    @Test fun closeInvalidatesTransferPreparationAndReadyCallbacks() {
        for (stage in 0..2) {
            val attempts = NasaPlaybackAttempt()
            val token = attempts.begin()
            if (stage >= 1) assertTrue(attempts.preparing(token))
            if (stage >= 2) assertTrue(attempts.prepared(token))
            attempts.cancel()
            assertFalse(attempts.preparing(token))
            assertFalse(attempts.prepared(token))
            assertFalse(attempts.timedOut(token))
            assertFalse(attempts.fail(token))
            assertFalse(attempts.isReady(token))
        }
    }
}
