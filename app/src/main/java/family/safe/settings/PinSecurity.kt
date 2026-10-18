package family.safe.settings

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.bouncycastle.crypto.params.KeyParameter

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
        val password = pin.toByteArray(Charsets.UTF_8)
        return try {
            // Same v1 PBKDF2; upstream HMAC state caching avoids Android's per-round digest bridge.
            val generator = PKCS5S2ParametersGenerator(SHA256Digest())
            generator.init(password, salt, iterations)
            (generator.generateDerivedParameters(256) as KeyParameter).key
        } finally { password.fill(0) }
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
