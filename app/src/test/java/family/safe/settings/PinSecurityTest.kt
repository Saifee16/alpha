package family.safe.settings

import org.junit.Assert.*
import org.junit.Test
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PinSecurityTest {
    @Test fun existingIndependentV1RecordsVerifyIncludingLeadingZeroAndMaximumLength() {
        // Fixed Python hashlib.pbkdf2_hmac vectors, created before the backend change.
        val records = listOf(
            "732918" to "v1:600000:AAECAwQFBgcICQoLDA0ODw==:EvvCvbabu/NneEP7HwNcLFwerIOXZF/GSNbe+m9jQCk=",
            "000000" to "v1:600000:Dw4NDAsKCQgHBgUEAwIBAA==:SO1HKaY240Qz+MRFo5+CdgwxFg+G5FBij7Tc+9CnjZM=",
            "12345678901234567890123456789012" to "v1:600000:AAECAwQFBgcICQoLDA0ODw==:wond75GyUJjKuTfshpSztpRYRL2GfBe7WFsi0tOebrA=")
        records.forEach { (pin, record) ->
            assertTrue(PinSecurity.verify(pin, record))
            assertFalse(PinSecurity.verify(pin.dropLast(1) + if (pin.last() == '9') '8' else '9', record))
            assertFalse(PinSecurity.verify(pin, record.replace("v1:", "v2:")))
            assertFalse(PinSecurity.verify(pin, record.replace(":600000:", ":599999:")))
        }
    }
    @Test fun newlyCreatedRecordsMatchTheUnchangedStandardJcaDerivation() {
        val parts = PinSecurity.create("001234").split(':')
        assertEquals("v1", parts[0])
        val salt = Base64.getDecoder().decode(parts[2])
        assertEquals(16, salt.size)
        val spec = PBEKeySpec("001234".toCharArray(), salt, 600000, 256)
        val standard = try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
            finally { spec.clearPassword() }
        assertEquals(600000, parts[1].toInt())
        assertTrue(MessageDigest.isEqual(standard, Base64.getDecoder().decode(parts[3])))
    }
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
