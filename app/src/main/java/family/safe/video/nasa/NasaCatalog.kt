package family.safe.video.nasa

data class NasaVideo(
    val id: String,
    val title: String,
    val description: String,
    val credit: String,
    val rightsNote: String
) {
    val category: String get() = "space"
    val source: String get() = "NASA official"
}

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
