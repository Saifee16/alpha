package family.safe.settings

import org.junit.Assert.*
import org.junit.Test

class PinSecurityTest {
    @Test fun saltedVerificationRejectsWrongAndMalformedPins() {
        val a = PinSecurity.create("123456")
        val b = PinSecurity.create("123456")
        assertNotEquals(a, b)
        assertTrue(PinSecurity.verify("123456", a))
        assertFalse(PinSecurity.verify("123457", a))
        assertFalse(PinSecurity.verify("123456", "broken"))
        assertFalse(PinSecurity.verify("123456", "v1:999999999:x:y"))
    }
    @Test fun weakSetupIsRejected() {
        for (pin in listOf("", "12345", "abcdef", "１２３４５６", "1".repeat(33))) {
            assertThrows(IllegalArgumentException::class.java) { PinSecurity.create(pin) }
        }
    }
    @Test fun attemptDelayIncreasesAndCannotBeBypassedByClockRollback() {
        val first = PinAttempts().failed(1000)
        assertFalse(first.canTry(1000))
        assertFalse(first.canTry(0))
        assertTrue(first.canTry(first.nextAllowedAt))
        val next = first.failed(first.nextAllowedAt)
        assertTrue(next.nextAllowedAt - first.nextAllowedAt > first.nextAllowedAt - 1000)
    }
}
