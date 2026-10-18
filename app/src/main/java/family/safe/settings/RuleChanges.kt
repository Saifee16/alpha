package family.safe.settings

import family.safe.data.FamilySettings
import family.safe.data.starterCategories
import family.safe.data.starterSources

enum class RuleGroup { TOPICS, BLOCKED, WEBSITES, ALL }

fun removeTopicRules(state: FamilySettings, id: String): FamilySettings {
    if (state.categories.none { it.id == id }) return state
    val videoIds = state.nasaVideos.filter { it.category == id }.map { it.id }.toSet()
    return state.copy(
        categories = state.categories.filterNot { it.id == id },
        sources = state.sources.map { source ->
            val remaining = source.categories - id
            source.copy(categories = remaining, enabled = source.enabled && remaining.isNotEmpty())
        },
        channels = state.channels.map { it.copy(categories = it.categories - id) }.filter { it.categories.isNotEmpty() },
        nasaReviews = state.nasaReviews.filterNot { it.id in videoIds }
    )
}

fun removeSourceRules(state: FamilySettings, id: String): FamilySettings =
    state.copy(sources = state.sources.filterNot { it.id == id })

fun addSupportedSearchSources(state: FamilySettings): FamilySettings {
    val missing = starterSources().filter { source ->
        source.id in setOf("vikidia", "wikijunior") && state.sources.none { it.id == source.id }
    }.map { it.copy(enabled = false) }
    require(state.sources.size + missing.size <= 30) { "Remove a source before adding supported search sources." }
    return state.copy(sources = state.sources + missing)
}

/** Rule resets preserve authentication and requests; restored access always needs new approval. */
fun resetRules(state: FamilySettings, group: RuleGroup): FamilySettings = when (group) {
    RuleGroup.TOPICS -> {
        val categories = starterCategories()
        val ids = categories.map { it.id }.toSet()
        state.copy(categories = categories,
            sources = state.sources.map { it.copy(categories = it.categories.intersect(ids), enabled = false) },
            channels = emptyList(), nasaReviews = emptyList())
    }
    RuleGroup.BLOCKED -> state.copy(blocked = FamilySettings().blocked)
    RuleGroup.WEBSITES -> state.copy(sources = starterSources())
    RuleGroup.ALL -> state.copy(categories = starterCategories(), blocked = FamilySettings().blocked,
        sources = starterSources(), channels = emptyList(), nasaReviews = emptyList())
}
