package family.safe.video.nasa

import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import family.safe.policy.QueryPolicy
import javax.net.ssl.HttpsURLConnection

data class NasaCandidate(val id: String, val title: String, val description: String)
data class NasaResponse(val status: Int, val body: String)

/** Adult engineering reference only; metadata/transport checks are not content or rights review. */
object NasaReference {
    const val ID = "JPL-20230929-PSYCHEf-0002-Imager"
    const val CHILD_REVIEWED = false
}

/** Bounded adult discovery; deliberately has no child-feed or child-playback entry point. */
class NasaClient(
    private val enabled: Boolean = false,
    private val assetProbe: ((String) -> Boolean)? = null,
    private val operation: NasaOperation? = null,
    private val transport: ((String) -> NasaResponse)? = null
) {
    /** One bounded parent-only search. A candidate still requires full adult content and rights review. */
    fun discover(query: String, category: String, blocked: Set<String>): List<NasaVideo> = withNasaOperation(operation) { op ->
        check(enabled) { "NASA integration is disabled" }
        require(plainMetadata(query, 200) && !QueryPolicy.containsBlocked(query, blocked)) { "Invalid NASA query" }
        require(category.matches(Regex("[A-Za-z0-9_-]{1,100}"))) { "Invalid category" }
        val encoded = URLEncoder.encode(query, "UTF-8")
        val items = request("https://images-api.nasa.gov/search?q=$encoded&center=JPL&media_type=video&page_size=5", op)
        val seen = mutableSetOf<String>()
        (0 until minOf(items.length(), 5)).mapNotNull { index ->
            op.check()
            runCatching {
                val records = items.getJSONObject(index).getJSONArray("data")
                require(records.length() == 1) { "Ambiguous NASA metadata" }
                val data = records.getJSONObject(0)
                require(data.getString("center") == "JPL" && data.getString("media_type") == "video")
                val id = data.getString("nasa_id")
                validateId(id)
                require(seen.add(id)) { "Duplicate NASA ID" }
                val title = data.getString("title")
                val description = data.getString("description")
                require(plainMetadata(title, 200) && plainMetadata(description, 4000))
                val credits = listOf("photographer", "secondary_creator").mapNotNull { key ->
                    if (!data.has(key)) null else data.getString(key).also { require(plainMetadata(it, 300)) }
                }
                val credit = credits.joinToString("; ").ifBlank { "NASA/JPL-Caltech" }
                require(plainMetadata(credit, 300))
                require(!QueryPolicy.containsBlocked(listOf(id, title, description, credit).joinToString(" "), blocked))
                val asset = mobileAsset(id, op)
                op.check()
                val playable = assetProbe?.invoke(asset) ?: nasaPlayableAsset(asset, op)
                op.check()
                require(playable) { "Unavailable or unsupported mobile video" }
                NasaVideo(id, title, description, credit, DISCOVERY_RIGHTS_NOTE, category).also {
                    require(validNasaVideo(it))
                }
            }.getOrElse { op.check(); null }
        }.also { op.check() }
    }

    fun spaceCandidates(): List<NasaCandidate> = withNasaOperation(operation) { op ->
        val items = request("https://images-api.nasa.gov/search?q=solar%20system&center=JPL&media_type=video&page_size=5", op)
        (0 until minOf(items.length(), 5)).mapNotNull { index ->
            op.check()
            val data = items.getJSONObject(index).getJSONArray("data").getJSONObject(0)
            if (data.optString("center") != "JPL" || data.optString("media_type") != "video") null
            else {
                val id = data.getString("nasa_id")
                validateId(id)
                NasaCandidate(id, data.getString("title"), data.optString("description"))
            }
        }
    }

    fun mobileAsset(id: String): String = withNasaOperation(operation) { op -> mobileAsset(id, op) }

    private fun mobileAsset(id: String, op: NasaOperation): String {
        op.check()
        check(enabled) { "NASA integration is disabled" }
        validateId(id)
        val items = request("https://images-api.nasa.gov/asset/${id.replace(" ", "%20")}", op)
        return (0 until items.length()).firstNotNullOfOrNull { index ->
            op.check()
            runCatching { nasaMobileUrl(items.getJSONObject(index).getString("href"), id) }.getOrNull()
        } ?: throw IllegalArgumentException("No approved HTTPS mobile MP4 in manifest")
    }

    private fun request(url: String, op: NasaOperation): org.json.JSONArray {
        op.check()
        check(enabled) { "NASA integration is disabled" }
        val response = transport?.invoke(url) ?: nasaGet(url, op)
        op.check()
        check(response.status == 200) { "NASA HTTP ${response.status}" }
        require(response.body.length <= 1024 * 1024) { "NASA response too large" }
        return JSONObject(response.body).getJSONObject("collection").getJSONArray("items")
    }
}

private const val DISCOVERY_RIGHTS_NOTE = "Parent review required: watch the entire video and inspect all credits before approval. NASA metadata does not establish suitability, public-domain status, music clearance, or rights to third-party material. No NASA endorsement."

private fun plainMetadata(value: String, limit: Int): Boolean =
    value.length in 1..limit && value.isNotBlank() && value == value.trim() && value.none {
        it == '<' || it == '>' || Character.isISOControl(it) ||
            Character.getType(it) in setOf(Character.FORMAT.toInt(), Character.SURROGATE.toInt(),
                Character.PRIVATE_USE.toInt(), Character.UNASSIGNED.toInt())
    }

/** HEAD only, exact canonical mobile endpoint, no redirects and no unknown/oversized assets. */
internal fun nasaPlayableAsset(
    url: String,
    operation: NasaOperation? = null,
    open: (String) -> HttpsURLConnection = { URL(it).openConnection() as HttpsURLConnection }
): Boolean = withNasaOperation(operation) { op ->
    runCatching {
        op.check()
        val uri = URI(url)
        val parts = uri.path.split('/')
        require(parts.size == 4 && parts[1] == "video")
        require(url == nasaMobileUrl(url, parts[2]))
        op.check()
        val connection = open(url)
        op.withConnection(connection) {
            connection.requestMethod = "HEAD"
            connection.responseCode == 200 &&
                connection.contentType?.substringBefore(';')?.trim() == "video/mp4" &&
                connection.contentLengthLong in 1..MAX_DOWNLOAD_BYTES
        }
    }.getOrElse { op.check(); false }
}

private fun validateId(id: String) {
    require(id.length in 1..200 && id.isNotBlank() && id == id.trim() &&
        id.matches(Regex("[A-Za-z0-9_ -]+"))) { "Invalid NASA ID" }
}

internal fun nasaMobileUrl(value: String, id: String): String {
    validateId(id)
    val uri = URI(value.replace(" ", "%20"))
    require(uri.scheme in setOf("https", "http") && uri.host == "images-assets.nasa.gov" &&
        uri.port == -1 && uri.userInfo == null && uri.query == null && uri.fragment == null &&
        uri.path == "/video/$id/$id~mobile.mp4") { "Unapproved NASA asset" }
    // NASA manifests still emit HTTP strings; only this exact origin/path is upgraded, never fetched as HTTP.
    return URI("https", null, uri.host, -1, uri.path, null, null).toASCIIString()
}

internal fun nasaGet(url: String, operation: NasaOperation? = null): NasaResponse = withNasaOperation(operation) { op ->
    val uri = URI(url)
    require(uri.scheme == "https" && uri.host == "images-api.nasa.gov" && uri.port == -1 && uri.userInfo == null && uri.fragment == null)
    op.check()
    val connection = URL(url).openConnection() as HttpsURLConnection
    op.withConnection(connection) {
        val status = connection.responseCode
        if (status != 200) NasaResponse(status, "") else {
            val output = ByteArrayOutputStream()
            connection.inputStream.use { copyBounded(it, output, 1024L * 1024, op) }
            NasaResponse(status, output.toString("UTF-8"))
        }
    }
}

internal fun configureNasaConnection(connection: HttpsURLConnection) {
    connection.instanceFollowRedirects = false
    connection.connectTimeout = 10000
    connection.readTimeout = 15000
    connection.useCaches = false
}

internal fun downloadNasaReference(
    url: String, output: OutputStream,
    operation: NasaOperation? = null,
    open: (String) -> HttpsURLConnection = { URL(it).openConnection() as HttpsURLConnection }
) = downloadNasaVideo(NasaReference.ID, url, output, operation, open)

internal fun downloadNasaVideo(
    id: String, url: String, output: OutputStream,
    operation: NasaOperation? = null,
    open: (String) -> HttpsURLConnection = { URL(it).openConnection() as HttpsURLConnection }
) {
    withNasaOperation(operation) { op ->
        require(url == nasaMobileUrl(url, id))
        op.check()
        val connection = open(url)
        op.withConnection(connection) {
            check(connection.responseCode == 200) { "Asset HTTP response denied" }
            require(connection.contentType?.substringBefore(';')?.trim() == "video/mp4") { "Expected MP4" }
            val expected = connection.contentLengthLong
            require(expected in 1..MAX_DOWNLOAD_BYTES) { "Missing or oversized asset length" }
            val actual = connection.inputStream.use { copyBounded(it, output, MAX_DOWNLOAD_BYTES, op) }
            require(actual == expected) { "Incomplete asset" }
        }
    }
}

private const val MAX_DOWNLOAD_BYTES = 32L * 1024 * 1024

internal fun copyBounded(input: InputStream, output: OutputStream, limit: Long, operation: NasaOperation? = null): Long = withNasaOperation(operation) { op ->
    val buffer = ByteArray(8192)
    var total = 0L
    while (true) {
        op.check()
        val count = input.read(buffer)
        op.check()
        if (count < 0) break
        require(count > 0) { "NASA transfer made no progress" }
        total += count
        require(total <= limit) { "NASA download too large" }
        output.write(buffer, 0, count)
    }
    require(total > 0) { "Empty NASA download" }
    total
}
