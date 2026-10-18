package family.safe.policy

import java.text.Normalizer
import java.util.Locale

data class Category(
    val id: String,
    val name: String,
    val terms: Set<String>,
    val enabled: Boolean = false
)

enum class QueryOutcome { ALLOW, DENY_BLOCKED, DENY_UNAPPROVED, REQUEST_PARENT, INVALID }

data class QueryDecision(
    val allowed: Boolean,
    val categoryIds: Set<String> = emptySet(),
    val reason: String,
    val outcome: QueryOutcome = if (allowed) QueryOutcome.ALLOW else QueryOutcome.DENY_UNAPPROVED,
    val searchPhrase: String? = null
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
            }) return QueryDecision(false, reason = "Invalid query", outcome = QueryOutcome.INVALID)
        if (containsBlocked(query, blocked)) return QueryDecision(false, reason = "Blocked phrase", outcome = QueryOutcome.DENY_BLOCKED)
        val normalized = normalize(query)
        if (normalized.isEmpty()) return QueryDecision(false, reason = "Invalid query", outcome = QueryOutcome.INVALID)
        val forms = mutableSetOf(normalized)
        listOf("how to ", "learn about ", "tell me about ", "what is the ", "what are the ", "what is ", "what are ", "explain ").forEach { prefix ->
            if (normalized.startsWith(prefix)) forms += normalized.removePrefix(prefix)
        }
        forms.toList().forEach { phrase ->
            listOf(" for kids", " for children", " for beginners", " explained", " tutorial", " lessons").forEach { suffix ->
                if (phrase.endsWith(suffix)) forms += phrase.removeSuffix(suffix)
            }
        }
        val matchedTerms = categories.filter { it.enabled }.flatMap { category ->
            category.terms.map(::normalize).filter { it.isNotBlank() && it in forms }.map { category.id to it }
        }
        val matches = matchedTerms.map { it.first }.toSet()
        return if (matches.isEmpty()) QueryDecision(false, reason = "Ask a parent to approve this exact phrase")
        else QueryDecision(true, matches, if(normalized in matchedTerms.map { it.second }) "Approved phrase" else "Approved niche with educational wording",
            searchPhrase = matchedTerms.minBy { it.second.length }.second)
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
