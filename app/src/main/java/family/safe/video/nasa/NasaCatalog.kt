package family.safe.video.nasa

import family.safe.video.archive.ArchiveLesson

data class NasaVideo(
    val id: String,
    val title: String,
    val description: String,
    val credit: String,
    val rightsNote: String,
    val category: String = "space",
    val provider: String = "nasa"
) {
    val source: String get() = if(provider == "archive") "Khan Academy via Internet Archive" else "NASA official"
}

const val MAX_NASA_VIDEOS = 30

/** Persist only bounded display text and stable IDs, never API-supplied navigation URLs. */
fun validNasaVideo(video: NasaVideo): Boolean =
    (when(video.provider) {
        "nasa" -> video.id.length in 1..200 && video.id == video.id.trim() && video.id.matches(Regex("[A-Za-z0-9_ -]+"))
        "archive" -> video.id == ArchiveLesson.VIDEO_ID && video.title == ArchiveLesson.TITLE
        else -> false
    }) &&
        video.category.matches(Regex("[A-Za-z0-9_-]{1,100}")) &&
        video.title.isNotBlank() && video.title.length <= 200 && video.description.length <= 4000 &&
        video.credit.isNotBlank() && video.credit.length <= 300 &&
        video.rightsNote.isNotBlank() && video.rightsNote.length <= 1000 &&
        listOf(video.title, video.description, video.credit, video.rightsNote).all { text ->
            text.none { (it.isISOControl() && it != '\n' && it != '\t') ||
                Character.getType(it) == Character.FORMAT.toInt() || it == '<' || it == '>' }
        }

fun archiveVideo(category: String) = NasaVideo(ArchiveLesson.VIDEO_ID,ArchiveLesson.TITLE,
    ArchiveLesson.DESCRIPTION,ArchiveLesson.CREDIT,ArchiveLesson.RIGHTS_NOTE,category,"archive")

/** Parent-review candidates only; inclusion never approves content or grants media rights. */
object NasaCatalog {
    val videos: List<NasaVideo> = listOf(
        NasaVideo(
            id = "JPL-20220310-CLIPPRf-0001-Europa Clipper Mission Animations",
            title = "Europa Clipper Mission Animations",
            description = "A mission animation showing a spacecraft leaving Earth, deploying in space, and flying past Jupiter's icy moon Europa.",
            credit = "NASA/JPL-Caltech",
            rightsNote = "NASA lists this animation for media and public use. Credit NASA/JPL-Caltech. Watch the entire preview and review its credits before approving. Third-party material and music may have separate rights; music clearance and full content review are not established. No NASA endorsement."
        ),
        NasaVideo(
            id = "JPL-20250318-NISARf-0001-NISAR_Animation_Reel",
            title = "NISAR Mission Animation Reel",
            description = "An animation of the NASA-ISRO Earth-observing satellite launching, deploying its radar antenna, and collecting data from orbit.",
            credit = "NASA/JPL-Caltech; animation: Koji Kuramura and Michael Stetson; director: Paul A. Rosen",
            rightsNote = "NASA lists this animation for media and public use. Credit NASA/JPL-Caltech and the listed creators. Watch the entire preview and review its credits before approving. Third-party material and music may have separate rights; music clearance and full content review are not established. No NASA endorsement."
        )
    )

    fun find(id: String): NasaVideo? = videos.firstOrNull { it.id == id }
}
