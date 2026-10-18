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

/** Parent-only lookup for one vetted lesson; never admits an arbitrary Archive upload. */
class ArchiveClient(
    private val enabled: Boolean = false,
    private val operation: NasaOperation? = null,
    private val transport: ((String) -> NasaResponse)? = null
) {
    fun discover(query: String, blocked: Set<String>): Boolean = withNasaOperation(operation) { op ->
        check(enabled) { "Archive integration is disabled" }
        require(query.length in 1..256 && query.isNotBlank() && query.none {
            Character.isISOControl(it) || Character.getType(it) in setOf(Character.FORMAT.toInt(),
                Character.SURROGATE.toInt(), Character.PRIVATE_USE.toInt(), Character.UNASSIGNED.toInt())
        }) { "Invalid Archive query" }
        require(!QueryPolicy.containsBlocked(query, blocked) && !QueryPolicy.containsBlocked(
            listOf(ArchiveLesson.VIDEO_ID, ArchiveLesson.TITLE, ArchiveLesson.DESCRIPTION, ArchiveLesson.CREDIT).joinToString(" "), blocked
        )) { "Blocked Archive lesson" }
        val normalized = QueryPolicy.normalize(query)
        val tokens = normalized.split(' ').filter { it.isNotEmpty() }.toSet()
        val titleTokens = QueryPolicy.normalize(ArchiveLesson.TITLE).split(' ').toSet()
        if (tokens.isEmpty() || !titleTokens.containsAll(tokens)) return@withNasaOperation false
        val search = "identifier:${ArchiveLesson.ITEM_ID} AND collection:khanacademy AND mediatype:movies AND title:($normalized)"
        val url = "https://archive.org/advancedsearch.php?q=${URLEncoder.encode(search, "UTF-8")}&fl%5B%5D=identifier&fl%5B%5D=title&fl%5B%5D=mediatype&fl%5B%5D=collection&rows=1&page=1&output=json"
        val root = request(url, op)
        check((root.optJSONObject("responseHeader")?.optInt("status", 0) ?: 0) == 0) { "Archive search failed" }
        val docs = root.getJSONObject("response").getJSONArray("docs")
        op.check()
        docs.length() == 1 && docs.optJSONObject(0)?.let(::knownMetadata) == true
    }

    fun assetUrl(): String = withNasaOperation(operation) { op ->
        check(enabled) { "Archive integration is disabled" }
        val root = request("https://archive.org/metadata/${ArchiveLesson.ITEM_ID}", op)
        require(knownMetadata(root.getJSONObject("metadata"))) { "Unexpected Archive lesson" }
        val files = root.getJSONArray("files")
        var matches = 0
        for (index in 0 until files.length()) {
            op.check()
            val file = files.optJSONObject(index) ?: continue
            if (file.opt("name") != ArchiveLesson.FILE_NAME) continue
            require(file.opt("source") == "original" && file.opt("size")?.toString() == ArchiveLesson.BYTES.toString() &&
                file.opt("md5") == ArchiveLesson.MD5 && file.opt("sha1") == ArchiveLesson.SHA1) { "Archive file snapshot changed" }
            matches++
        }
        require(matches == 1) { "Missing or ambiguous Archive file" }
        val host = root.getString("d1")
        val dir = root.getString("dir")
        require(host.length <= 253 && host.matches(DATA_HOST) &&
            dir.matches(Regex("/[0-9]+/items/${Regex.escape(ArchiveLesson.ITEM_ID)}"))) { "Unapproved Archive data endpoint" }
        val url = "https://$host$dir/${ArchiveLesson.FILE_NAME}"
        require(validAssetUrl(url)) { "Unapproved Archive data endpoint" }
        url
    }

    fun download(
        url: String,
        output: OutputStream,
        open: (String) -> HttpsURLConnection = { URL(it).openConnection() as HttpsURLConnection }
    ) {
        withNasaOperation(operation) { op ->
            check(enabled) { "Archive integration is disabled" }
            require(validAssetUrl(url)) { "Unapproved Archive asset" }
            noCookies()
            op.check()
            val connection = open(url)
            op.withConnection(connection) {
                configure(connection, "video/mp4")
                check(connection.responseCode == 200) { "Archive asset HTTP response denied" }
                require(connection.contentType?.substringBefore(';')?.trim() == "video/mp4") { "Expected MP4" }
                require(connection.contentLengthLong == ArchiveLesson.BYTES) { "Archive asset length changed" }
                val actual = connection.inputStream.use { copyBounded(it, output, ArchiveLesson.BYTES, op) }
                require(actual == ArchiveLesson.BYTES) { "Incomplete Archive asset" }
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

    private fun knownMetadata(item: JSONObject): Boolean =
        item.opt("identifier") == ArchiveLesson.ITEM_ID && item.opt("title") == ArchiveLesson.TITLE &&
            item.opt("mediatype") == "movies" && when (val collection = item.opt("collection")) {
                is String -> collection == "khanacademy"
                is JSONArray -> (0 until collection.length()).any { collection.opt(it) == "khanacademy" }
                else -> false
            }

    private fun validAssetUrl(url: String): Boolean {
        val uri = try { URI(url) } catch (_: Exception) { return false }
        return uri.scheme == "https" && uri.rawAuthority == uri.host && uri.host.orEmpty().length <= 253 &&
            uri.host.orEmpty().matches(DATA_HOST) &&
            uri.rawQuery == null && uri.rawFragment == null &&
            uri.rawPath.orEmpty().matches(Regex("/[0-9]+/items/${Regex.escape(ArchiveLesson.ITEM_ID)}/${Regex.escape(ArchiveLesson.FILE_NAME)}"))
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
