package family.safe.video.archive

import family.safe.policy.QueryPolicy
import family.safe.video.nasa.NasaOperation
import family.safe.video.nasa.NasaResponse
import family.safe.video.nasa.copyBounded
import family.safe.video.nasa.withNasaOperation
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.CookieHandler
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection

/** Parent-only lookup for pinned originals; never admits an arbitrary Archive upload. */
class ArchiveClient(
    private val enabled: Boolean = false,
    private val operation: NasaOperation? = null,
    private val transport: ((String) -> NasaResponse)? = null
) {
    fun discover(query: String, blocked: Set<String>): Boolean {
        require(!QueryPolicy.containsBlocked("${ArchiveLesson.TITLE} ${ArchiveLesson.DESCRIPTION} ${ArchiveLesson.CREDIT}", blocked)) { "Blocked Archive lesson" }
        return discoverLessons(query, blocked).any { it.videoId == ArchiveLesson.VIDEO_ID }
    }

    fun discoverLessons(query: String, blocked: Set<String>): List<ArchiveSnapshot> = withNasaOperation(operation) { op ->
        check(enabled) { "Archive integration is disabled" }
        require(query.length in 1..256 && query.isNotBlank() && query.none {
            Character.isISOControl(it) || Character.getType(it) in setOf(Character.FORMAT.toInt(),
                Character.SURROGATE.toInt(), Character.PRIVATE_USE.toInt(), Character.UNASSIGNED.toInt())
        }) { "Invalid Archive query" }
        require(!QueryPolicy.containsBlocked(query, blocked)) { "Blocked Archive query" }
        val normalized = QueryPolicy.normalize(query)
        val tokens = normalized.split(' ').filter { it.isNotEmpty() }.toSet()
        val candidates = ArchiveLesson.snapshots.filter { lesson ->
            tokens.isNotEmpty() && QueryPolicy.normalize(lesson.title).split(' ').toSet().containsAll(tokens) &&
                !QueryPolicy.containsBlocked("${lesson.videoId} ${lesson.title} ${lesson.description} ${lesson.credit}", blocked)
        }
        if (candidates.isEmpty()) return@withNasaOperation emptyList()
        val identifiers = candidates.joinToString(" OR ") { "identifier:${it.itemId}" }
        val identity = if (candidates.size == 1) identifiers else "($identifiers)"
        val search = "$identity AND collection:khanacademy AND mediatype:movies AND title:($normalized)"
        val url = "https://archive.org/advancedsearch.php?q=${URLEncoder.encode(search, "UTF-8")}&fl%5B%5D=identifier&fl%5B%5D=title&fl%5B%5D=mediatype&fl%5B%5D=collection&rows=${candidates.size}&page=1&output=json"
        val root = request(url, op)
        check((root.optJSONObject("responseHeader")?.optInt("status", 0) ?: 0) == 0) { "Archive search failed" }
        val docs = root.getJSONObject("response").getJSONArray("docs")
        op.check()
        candidates.filter { lesson -> (0 until docs.length()).any { index ->
            docs.optJSONObject(index)?.let { knownMetadata(it, lesson) } == true
        } }
    }

    fun assetUrl(videoId: String = ArchiveLesson.VIDEO_ID): String = withNasaOperation(operation) { op ->
        check(enabled) { "Archive integration is disabled" }
        val lesson = requireNotNull(ArchiveLesson.find(videoId)) { "Unknown Archive lesson" }
        val root = request("https://archive.org/metadata/${lesson.itemId}", op)
        require(knownMetadata(root.getJSONObject("metadata"), lesson)) { "Unexpected Archive lesson" }
        val files = root.getJSONArray("files")
        var matches = 0
        for (index in 0 until files.length()) {
            op.check()
            val file = files.optJSONObject(index) ?: continue
            if (file.opt("name") != lesson.fileName) continue
            require(file.opt("source") == "original" && file.opt("size")?.toString() == lesson.bytes.toString() &&
                file.opt("md5") == lesson.md5 && file.opt("sha1") == lesson.sha1) { "Archive file snapshot changed" }
            matches++
        }
        require(matches == 1) { "Missing or ambiguous Archive file" }
        val host = root.getString("d1")
        val dir = root.getString("dir")
        require(host.length <= 253 && host.matches(DATA_HOST) &&
            dir.matches(Regex("/[0-9]+/items/${Regex.escape(lesson.itemId)}"))) { "Unapproved Archive data endpoint" }
        val url = "https://$host$dir/${lesson.fileName}"
        require(assetSnapshot(url) == lesson) { "Unapproved Archive data endpoint" }
        url
    }

    fun download(
        url: String,
        output: OutputStream,
        open: (String) -> HttpsURLConnection = { URL(it).openConnection() as HttpsURLConnection }
    ) {
        withNasaOperation(operation) { op ->
            check(enabled) { "Archive integration is disabled" }
            val lesson = requireNotNull(assetSnapshot(url)) { "Unapproved Archive asset" }
            noCookies()
            op.check()
            val connection = open(url)
            op.withConnection(connection) {
                configure(connection, "video/mp4")
                check(connection.responseCode == 200) { "Archive asset HTTP response denied" }
                require(connection.contentType?.substringBefore(';')?.trim() == "video/mp4") { "Expected MP4" }
                require(connection.contentLengthLong == lesson.bytes) { "Archive asset length changed" }
                val actual = connection.inputStream.use { copyBounded(it, output, lesson.bytes, op) }
                require(actual == lesson.bytes) { "Incomplete Archive asset" }
                // The existing player separately verifies the fixed SHA-256 before preview/approval/playback.
            }
        }
    }

    private fun request(url: String, op: NasaOperation): JSONObject {
        op.check()
        val response = transport?.invoke(url) ?: run {
            noCookies()
            val connection = URL(url).openConnection() as HttpsURLConnection
            op.withConnection(connection) {
                configure(connection, "application/json")
                val status = connection.responseCode
                if (status != 200) NasaResponse(status, "") else {
                    require(connection.contentType.orEmpty().substringBefore(';').trim() == "application/json") { "Expected Archive JSON" }
                    require(connection.contentLengthLong <= MAX_JSON_BYTES) { "Archive JSON too large" }
                    val output = ByteArrayOutputStream()
                    connection.inputStream.use { copyBounded(it, output, MAX_JSON_BYTES, op) }
                    NasaResponse(status, output.toString("UTF-8"))
                }
            }
        }
        op.check()
        check(response.status == 200) { "Archive HTTP ${response.status}" }
        require(response.body.toByteArray(Charsets.UTF_8).size <= MAX_JSON_BYTES) { "Archive JSON too large" }
        return JSONObject(response.body).also { check(!it.has("error")) { "Archive metadata unavailable" } }
    }

    private fun knownMetadata(item: JSONObject, lesson: ArchiveSnapshot): Boolean =
        item.opt("identifier") == lesson.itemId && item.opt("title") == lesson.title &&
            item.opt("mediatype") == "movies" && when (val collection = item.opt("collection")) {
                is String -> collection == "khanacademy"
                is JSONArray -> (0 until collection.length()).any { collection.opt(it) == "khanacademy" }
                else -> false
            }

    private fun assetSnapshot(url: String): ArchiveSnapshot? {
        val uri = try { URI(url) } catch (_: Exception) { return null }
        if (!(uri.scheme == "https" && uri.rawAuthority == uri.host && uri.host.orEmpty().length <= 253 &&
            uri.host.orEmpty().matches(DATA_HOST) &&
            uri.rawQuery == null && uri.rawFragment == null)) return null
        return ArchiveLesson.snapshots.firstOrNull { lesson -> uri.rawPath.orEmpty().matches(
            Regex("/[0-9]+/items/${Regex.escape(lesson.itemId)}/${Regex.escape(lesson.fileName)}")) }
    }

    private fun noCookies() { check(CookieHandler.getDefault() == null) { "Cookie-enabled transport is unavailable" } }

    private fun configure(connection: HttpsURLConnection, accept: String) {
        connection.setRequestProperty("Accept", accept)
        connection.setRequestProperty("Accept-Encoding", "identity")
        connection.setRequestProperty("User-Agent", "FamilySafe/0.1 (https://github.com/Saifee16/alpha; unofficial educational reader)")
        connection.setRequestProperty("Cookie", "")
        connection.setRequestProperty("Cache-Control", "no-store")
    }

    private companion object {
        const val MAX_JSON_BYTES = 1_048_576L
        val DATA_HOST = Regex("(?:ia|dn)[0-9]+\\.us\\.archive\\.org")
    }
}
