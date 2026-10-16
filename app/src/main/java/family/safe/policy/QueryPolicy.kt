package family.safe.policy

import java.text.Normalizer
import java.util.Locale

data class Category(
    val id: String,
    val name: String,
    val terms: Set<String>,
    val enabled: Boolean = false
)

data class QueryDecision(
    val allowed: Boolean,
    val categoryIds: Set<String> = emptySet(),
    val reason: String
)

object QueryPolicy {
    private const val MAX_QUERY_LENGTH = 256
    private val separators = Regex("[^\\p{L}\\p{M}\\p{N}]+")

    fun normalize(text: String): String = separators.replace(
        Normalizer.normalize(text, Normalizer.Form.NFKC).lowercase(Locale.ROOT), " "
    ).trim()

    fun evaluate(query: String, categories: List<Category>, blocked: Set<String>): QueryDecision {
        if (query.isBlank() || query.length > MAX_QUERY_LENGTH || query.any {
                Character.isISOControl(it) || Character.getType(it) == Character.FORMAT.toInt() ||
                    Character.getType(it) == Character.SURROGATE.toInt() ||
                    Character.getType(it) == Character.PRIVATE_USE.toInt() ||
                    Character.getType(it) == Character.UNASSIGNED.toInt()
            }) return QueryDecision(false, reason = "Invalid query")
        if (containsBlocked(query, blocked)) return QueryDecision(false, reason = "Blocked phrase")
        val normalized = normalize(query)
        if (normalized.isEmpty()) return QueryDecision(false, reason = "Invalid query")
        val matches = categories.filter { category ->
            category.enabled && category.terms.any { normalize(it) == normalized }
        }.map { it.id }.toSet()
        return if (matches.isEmpty()) QueryDecision(false, reason = "Ask a parent to approve this exact phrase")
        else QueryDecision(true, matches, "Approved phrase")
    }

    fun containsBlocked(text: String, blocked: Set<String>): Boolean {
        val visible = Normalizer.normalize(text, Normalizer.Form.NFKC).lowercase(Locale.ROOT)
            .filterNot { Character.getType(it) == Character.FORMAT.toInt() }
        return blocked.any { phrase ->
            val letters = normalize(phrase).filterNot { it == ' ' }
            letters.isNotEmpty() && Regex(
                "(?<![\\p{L}\\p{M}\\p{N}])" + letters.map { Regex.escape(it.toString()) }
                    .joinToString("[^\\p{L}\\p{M}\\p{N}]*") + "(?![\\p{L}\\p{M}\\p{N}])"
            ).containsMatchIn(visible)
        }
    }
}
