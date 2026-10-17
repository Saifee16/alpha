package family.safe.video.nasa

import family.safe.data.FamilySettings
import family.safe.policy.QueryPolicy

/** Discovery stores candidates, never approval. Only an authenticated parent can start it. */
fun canDiscoverNasa(state: FamilySettings, parent: Boolean, category: String, query: String): Boolean =
    parent && state.pinHash != null && query.length <= 200 &&
        category in QueryPolicy.evaluate(query, state.categories, state.blocked).categoryIds

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
    val approved = state.nasaReviews.filter { it.approved && it.sha256?.matches(Regex("[a-f0-9]{64}")) == true }.map { it.id }.toSet()
    return state.nasaVideos.filter { video ->
        validNasaVideo(video) && video.id in approved && state.categories.any { it.id == video.category && it.enabled } &&
            !nasaMetadataBlocked(video, state)
    }
}
