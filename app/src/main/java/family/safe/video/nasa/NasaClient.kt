package family.safe.video.nasa

import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.URL
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
    private val transport: (String) -> NasaResponse = ::nasaGet
) {
    fun spaceCandidates(): List<NasaCandidate> {
        val items = request("https://images-api.nasa.gov/search?q=solar%20system&center=JPL&media_type=video&page_size=5")
        return (0 until minOf(items.length(), 5)).mapNotNull { index ->
            val data = items.getJSONObject(index).getJSONArray("data").getJSONObject(0)
            if (data.optString("center") != "JPL" || data.optString("media_type") != "video") null
            else {
                val id = data.getString("nasa_id")
                validateId(id)
                NasaCandidate(id, data.getString("title"), data.optString("description"))
            }
        }
    }

    fun mobileAsset(id: String): String {
        check(enabled) { "NASA integration is disabled" }
        validateId(id)
        val items = request("https://images-api.nasa.gov/asset/${id.replace(" ", "%20")}")
        return (0 until items.length()).firstNotNullOfOrNull { index ->
            runCatching { nasaMobileUrl(items.getJSONObject(index).getString("href"), id) }.getOrNull()
        } ?: throw IllegalArgumentException("No approved HTTPS mobile MP4 in manifest")
    }

    private fun request(url: String): org.json.JSONArray {
        check(enabled) { "NASA integration is disabled" }
        val response = transport(url)
        check(response.status == 200) { "NASA HTTP ${response.status}" }
        require(response.body.length <= 1024 * 1024) { "NASA response too large" }
        return JSONObject(response.body).getJSONObject("collection").getJSONArray("items")
    }
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

internal fun nasaGet(url: String): NasaResponse {
    val uri = URI(url)
    require(uri.scheme == "https" && uri.host == "images-api.nasa.gov" && uri.port == -1 && uri.userInfo == null)
    val connection = URL(url).openConnection() as HttpsURLConnection
    try {
        configureNasaConnection(connection)
        val status = connection.responseCode
        if (status != 200) return NasaResponse(status, "")
        val output = ByteArrayOutputStream()
        connection.inputStream.use { copyBounded(it, output, 1024L * 1024) }
        return NasaResponse(status, output.toString("UTF-8"))
    } finally { connection.disconnect() }
}

internal fun configureNasaConnection(connection: HttpsURLConnection) {
    connection.instanceFollowRedirects = false
    connection.connectTimeout = 10000
    connection.readTimeout = 15000
    connection.useCaches = false
}

internal fun downloadNasaReference(
    url: String, output: OutputStream,
    open: (String) -> HttpsURLConnection = { URL(it).openConnection() as HttpsURLConnection }
) = downloadNasaVideo(NasaReference.ID, url, output, open)

internal fun downloadNasaVideo(
    id: String, url: String, output: OutputStream,
    open: (String) -> HttpsURLConnection = { URL(it).openConnection() as HttpsURLConnection }
) {
    require(url == nasaMobileUrl(url, id))
    val connection = open(url)
    try {
        configureNasaConnection(connection)
        check(connection.responseCode == 200) { "Asset HTTP response denied" }
        require(connection.contentType?.substringBefore(';')?.trim() == "video/mp4") { "Expected MP4" }
        val expected = connection.contentLengthLong
        require(expected in 1..MAX_DOWNLOAD_BYTES) { "Missing or oversized asset length" }
        val actual = connection.inputStream.use { copyBounded(it, output, MAX_DOWNLOAD_BYTES) }
        require(actual == expected) { "Incomplete asset" }
    } finally { connection.disconnect() }
}

private const val MAX_DOWNLOAD_BYTES = 32L * 1024 * 1024

internal fun copyBounded(input: InputStream, output: OutputStream, limit: Long): Long {
    val buffer = ByteArray(8192)
    var total = 0L
    while (true) {
        check(!Thread.currentThread().isInterrupted) { "Download cancelled" }
        val count = input.read(buffer)
        if (count < 0) break
        total += count
        require(total <= limit) { "NASA download too large" }
        output.write(buffer, 0, count)
    }
    require(total > 0) { "Empty NASA download" }
    return total
}
