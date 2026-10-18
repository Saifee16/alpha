package family.safe.video.archive

/** Bounded source-correlated engineering lessons; inclusion never constitutes parent approval. */
object ArchiveLesson {
    const val ITEM_ID = "KA-converted-e_SpXIw_Qts"
    const val VIDEO_ID = "archive:KA-converted-e_SpXIw_Qts"
    const val FILE_NAME = "e_SpXIw_Qts.mp4"
    const val TITLE = "Addition 3"
    const val DESCRIPTION = "Practice carrying digits to add multi-digit numbers."
    const val CREDIT = "Khan Academy. Unofficial; no endorsement. All Khan Academy content is available for free at https://www.khanacademy.org ."
    const val RIGHTS_NOTE = "Khan Academy's current video reuse policy links CC BY-NC-SA 4.0: https://creativecommons.org/licenses/by-nc-sa/4.0/ . Noncommercial use only; retain attribution and license, and mark adaptations. The Archive item has no individual license field; publisher policy and exact original-video identity were checked separately. Each complete video requires full adult content/credits/rights review before approval. No automatic suitability or rights certification."
    const val SHA256 = "a1a9a7538fcdeac9f8cefca54fbd9aea42f4edacde63163ea444c8df127816e7"
    const val BYTES = 7_794_483L
    const val MD5 = "f1a8b98462f0b63a78988073fbf32d94"
    const val SHA1 = "665f6d41202559974ba7876b6612bbee618b7fec"
    // shortcut: only byte-verified originals, add lessons after publisher, rights and native playback checks.
    val snapshots: List<ArchiveSnapshot> = listOf(
        ArchiveSnapshot(ITEM_ID, FILE_NAME, TITLE, DESCRIPTION, SHA256, BYTES, MD5, SHA1),
        ArchiveSnapshot("KA-converted-xO_1bYgoQvA", "xO_1bYgoQvA.mp4",
            "Multiplication 2: The Multiplication Tables", "Introduction to the multiplication tables from 2 to 9.",
            "fed1abd705f3bb581eaeaac871903a62842ba1d7744b52cf820d06aed6894970", 14_071_815L,
            "66765e0a7443b71e211c634d4839344a", "de63aeaae86d5ccee7e19e0d4d375e42b5e45ff1"),
        ArchiveSnapshot("KA-converted-WPimvspI0_c", "WPimvspI0_c.mp4",
            "Fractions in lowest terms", "Simplify fractions by finding common factors.",
            "6c746e0651d7a3af67563a406bea560496d572528730001a4ed8c6a36a315747", 4_660_787L,
            "decc6641d5bb7d0cd0070e87a312a929", "8c16a8caab43d89a9f2459948666a488c0f3eb47")
    )

    fun find(videoId: String): ArchiveSnapshot? = snapshots.firstOrNull { it.videoId == videoId }
}
