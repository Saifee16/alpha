package family.safe.video.nasa

import family.safe.data.FamilySettings
import family.safe.policy.QueryPolicy

/** Local decisions apply only to this fixed collection, never arbitrary NASA search results. */
fun approvedNasaVideos(state: FamilySettings): List<NasaVideo> {
    if (state.pinHash == null || state.categories.none { it.id == "space" && it.enabled }) return emptyList()
    val approved = state.nasaReviews.filter { it.approved && it.sha256?.matches(Regex("[a-f0-9]{64}")) == true }.map { it.id }.toSet()
    return NasaCatalog.videos.filter { it.id in approved && !QueryPolicy.containsBlocked("${it.title} ${it.description}", state.blocked) }
}
