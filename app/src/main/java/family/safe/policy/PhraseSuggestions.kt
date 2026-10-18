package family.safe.policy

/** Suggestions are editable text; only the parent's normal Save action authorizes them. */
fun suggestSearchPhrases(text: String, blocked: Set<String>): String {
    require(text.length <= 4000) { "Keep the phrase editor within 4000 characters." }
    val entered = text.split(',').filter { it.isNotBlank() }
    require(entered.isNotEmpty()) { "Enter a topic or niche phrase first." }
    require(entered.all { phrase ->
        QueryPolicy.evaluate(phrase, listOf(Category("suggestions", "Suggestions", setOf(phrase), true)), blocked).allowed
    }) { "Remove blocked or invalid phrases before suggesting searches." }
    val originals = entered.map(QueryPolicy::normalize).toSet()
    val suggestions = originals.toMutableSet()
    originals.filterNot {
        it.startsWith("learn about ") || it.startsWith("tell me about ") ||
            it.endsWith(" for kids") || it.endsWith(" for children")
    }.forEach { phrase ->
        suggestions.addAll(listOf("$phrase for kids", "$phrase for children", "learn about $phrase", "tell me about $phrase"))
    }
    require(suggestions.size <= 100) { "Suggestions exceed 100 phrases. Use fewer topic or niche phrases." }
    require(suggestions.all { it.length <= 256 }) { "A suggested search exceeds 256 characters. Shorten its topic or niche phrase." }
    require(suggestions.none { QueryPolicy.containsBlocked(it, blocked) }) { "A suggested search contains a blocked phrase. Nothing was changed." }
    return suggestions.joinToString(", ").also {
        require(it.length <= 4000) { "Suggestions exceed 4000 characters. Use fewer or shorter phrases." }
    }
}
