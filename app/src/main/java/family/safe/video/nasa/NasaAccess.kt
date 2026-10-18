package family.safe.video.nasa

import family.safe.data.FamilySettings
import family.safe.policy.QueryPolicy
import family.safe.video.archive.ArchiveLesson

/** Discovery stores candidates, never approval. Only an authenticated parent can start it. */
fun canDiscoverNasa(state: FamilySettings, parent: Boolean, category: String, query: String): Boolean =
    parent && state.pinHash != null && query.length <= 200 &&
        category in QueryPolicy.evaluate(query, state.categories, state.blocked).categoryIds

fun canDiscoverArchive(state: FamilySettings, parent: Boolean, category: String, query: String): Boolean =
    category in state.archiveCategories && canDiscoverNasa(state,parent,category,query)

fun videoSourceEnabled(video: NasaVideo, state: FamilySettings): Boolean = when(video.provider) {
    "nasa" -> true
    "archive" -> video.category in state.archiveCategories
    else -> false
}

fun nasaMetadataBlocked(video: NasaVideo, state: FamilySettings): Boolean =
    QueryPolicy.containsBlocked("${video.title} ${video.description} ${video.credit}", state.blocked)

/** Repeated discovery must not silently replace reviewed metadata or expand its category. */
fun mergeNasaCandidates(state: FamilySettings, category: String, candidates: List<NasaVideo>): FamilySettings {
    require(candidates.size <= 5 && candidates.all { validNasaVideo(it) && it.category == category })
    val known = state.nasaVideos.map { it.id }.toSet()
    val additions = candidates.distinctBy { it.id }.filter { it.id !in known }
        .take((MAX_NASA_VIDEOS - state.nasaVideos.size).coerceAtLeast(0))
    return state.copy(nasaVideos = state.nasaVideos + additions)
}

/** Candidate membership alone never authorizes children; category and digest approval are required. */
fun approvedNasaVideos(state: FamilySettings): List<NasaVideo> {
    if (state.pinHash == null) return emptyList()
    val approved = state.nasaReviews.filter { it.approved && it.sha256?.matches(Regex("[a-f0-9]{64}")) == true }.associateBy { it.id }
    return state.nasaVideos.filter { video ->
        validNasaVideo(video) && video.id in approved && videoSourceEnabled(video,state) &&
            (video.provider != "archive" || approved[video.id]?.sha256 == ArchiveLesson.find(video.id)?.sha256) && state.categories.any { it.id == video.category && it.enabled } &&
            !nasaMetadataBlocked(video, state)
    }
}

/** Watch searches only the eligible approved library; it never discovers new candidates. */
fun approvedWatchVideos(state: FamilySettings, query: String): List<NasaVideo> {
    val library = approvedNasaVideos(state)
    if (query.isBlank()) return library
    val decision = QueryPolicy.evaluate(query,state.categories,state.blocked)
    if (!decision.allowed) return emptyList()
    val phrase = QueryPolicy.normalize(decision.searchPhrase ?: query)
    val tokens = phrase.split(' ').toSet()
    return library.filter { video ->
        val category = state.categories.firstOrNull { it.id == video.category }
        video.category in decision.categoryIds &&
            (phrase == QueryPolicy.normalize(video.category) || phrase == category?.let { QueryPolicy.normalize(it.name) } ||
                (video.category == "math" && phrase in setOf("math", "maths", "mathematics")) ||
                QueryPolicy.normalize("${video.title} ${video.description}").split(' ').toSet().containsAll(tokens))
    }
}
