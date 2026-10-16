package family.safe.settings

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinSecurity {
    private const val ITERATIONS = 600_000
    fun create(pin: String): String {
        require(pin.matches(Regex("[0-9]{6,32}"))) { "Use 6 to 32 digits" }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return "v1:$ITERATIONS:${Base64.getEncoder().encodeToString(salt)}:${Base64.getEncoder().encodeToString(derive(pin, salt, ITERATIONS))}"
    }
    fun verify(pin: String, record: String): Boolean = runCatching {
        if (!pin.matches(Regex("[0-9]{6,32}"))) return false
        val parts = record.split(':')
        if (parts.size != 4 || parts[0] != "v1" || parts[1].toInt() != ITERATIONS) return false
        val salt = Base64.getDecoder().decode(parts[2])
        val hash = Base64.getDecoder().decode(parts[3])
        salt.size == 16 && hash.size == 32 && MessageDigest.isEqual(hash, derive(pin, salt, ITERATIONS))
    }.getOrDefault(false)
    private fun derive(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }
}

data class PinAttempts(val failures: Int = 0, val nextAllowedAt: Long = 0) {
    fun canTry(now: Long): Boolean = now >= nextAllowedAt
    fun failed(now: Long): PinAttempts {
        val count = (failures + 1).coerceAtMost(20)
        val delay = (1000L shl count.coerceAtMost(10)).coerceAtMost(900_000L)
        return PinAttempts(count, maxOf(now, nextAllowedAt) + delay)
    }
}
