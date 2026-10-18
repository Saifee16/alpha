package family.safe.video.archive

/** Exact verified bytes; catalog membership never grants child access. */
data class ArchiveSnapshot(
    val itemId: String,
    val fileName: String,
    val title: String,
    val description: String,
    val sha256: String,
    val bytes: Long,
    val md5: String,
    val sha1: String
) {
    val videoId: String get() = "archive:$itemId"
    val credit: String get() = ArchiveLesson.CREDIT
    val rightsNote: String get() = ArchiveLesson.RIGHTS_NOTE
}
