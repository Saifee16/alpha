package family.safe.video

import java.io.IOException
import java.net.URL
import java.net.URLEncoder
import java.text.Normalizer
import java.util.Locale
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

data class TrustedChannel(val id: String, val name: String, val categories: Set<String>, val approvedAt: Long = System.currentTimeMillis())
data class ChannelCandidate(val id: String, val name: String, val description: String, val url: String)
data class VideoResult(val id: String, val title: String, val description: String, val channelId: String, val durationSeconds: Long)
data class ApiResponse(val status: Int, val body: String)
enum class VideoError { DISABLED, MISSING_KEY, INVALID_INPUT, QUERY_DENIED, QUOTA, CREDENTIALS, NETWORK, INVALID_RESPONSE, API }
class VideoApiException(val reason: VideoError) : Exception(reason.name)

/** Network access stays disabled until child-directed API setup is complete. Never enables playback. */
class YouTubeClient(
    private val enabled: Boolean = false,
    private val metadataAllowed: (String) -> Boolean = { false },
    private val transport: (String, Map<String, String>) -> ApiResponse = { endpoint, parameters -> httpsGet(endpoint, parameters) }
) {
    suspend fun resolveChannels(name: String, key: String): List<ChannelCandidate> = withContext(Dispatchers.IO) {
        checkAccess(key)
        validateQuery(name)
        val search = request("search", mapOf("part" to "snippet", "type" to "channel", "safeSearch" to "strict", "q" to name, "maxResults" to "5"), key)
        val ids = search.items().mapNotNull { item ->
            item.optJSONObject("id")?.takeIf { it.optString("kind") == "youtube#channel" }
                ?.optString("channelId")?.takeIf { channelId.matches(it) }
        }.distinct().take(5)
        if (ids.isEmpty()) return@withContext emptyList()
        request("channels", mapOf("part" to "snippet", "id" to ids.joinToString(",")), key).items().mapNotNull { item ->
            val id = item.optString("id")
            val snippet = item.optJSONObject("snippet") ?: return@mapNotNull null
            val title = snippet.optString("title")
            if (id !in ids || title.isBlank()) null else
                ChannelCandidate(id, title, snippet.optString("description"), "https://www.youtube.com/channel/$id")
        }
    }

    /** Caller supplies only channels associated with the query's approved categories. */
    suspend fun search(query: String, channels: List<TrustedChannel>, key: String, blockedTerms: Set<String>): List<VideoResult> = withContext(Dispatchers.IO) {
        checkAccess(key)
        validateQuery(query)
        if (!metadataAllowed(query) || blocked(query, blockedTerms)) throw VideoApiException(VideoError.QUERY_DENIED)
        val now = System.currentTimeMillis()
        val selected = channels.filter { it.approvedAt <= now && now - it.approvedAt < 30L * 24 * 60 * 60 * 1000 }.distinctBy { it.id }.take(3)
        if (selected.any { !channelId.matches(it.id) || it.categories.isEmpty() }) throw VideoApiException(VideoError.INVALID_INPUT)
        val idsByChannel = mutableMapOf<String, String>()
        for (channel in selected) {
            val response = request("search", mapOf("part" to "snippet", "q" to query, "channelId" to channel.id,
                "type" to "video", "safeSearch" to "strict", "videoEmbeddable" to "true", "videoSyndicated" to "true",
                "maxResults" to "15", "order" to "relevance"), key)
            for (item in response.items().take(15)) {
                val identity = item.optJSONObject("id") ?: continue
                val id = identity.optString("videoId")
                if (identity.optString("kind") == "youtube#video" && videoId.matches(id) &&
                    item.optJSONObject("snippet")?.optString("channelId") == channel.id) idsByChannel[id] = channel.id
            }
        }
        if (idsByChannel.isEmpty()) return@withContext emptyList()
        request("videos", mapOf("part" to "snippet,contentDetails,status", "id" to idsByChannel.keys.joinToString(",")), key)
            .items().mapNotNull { item -> eligible(item, idsByChannel, blockedTerms) }.distinctBy { it.id }
    }

    private fun eligible(item: JSONObject, expected: Map<String, String>, terms: Set<String>): VideoResult? {
        val id = item.optString("id")
        val snippet = item.optJSONObject("snippet") ?: return null
        val channel = snippet.optString("channelId")
        if (expected[id] != channel || id !in expected) return null
        val status = item.optJSONObject("status") ?: return null
        val details = item.optJSONObject("contentDetails") ?: return null
        if (!status.optBoolean("embeddable") || status.optString("privacyStatus") != "public" ||
            status.optString("uploadStatus") != "processed" || snippet.optString("liveBroadcastContent") != "none") return null
        if (details.has("regionRestriction") || details.optJSONObject("contentRating")?.optString("ytRating")?.isNotEmpty() == true) return null
        val seconds = durationSeconds(details.optString("duration")) ?: return null
        // shortcut: duration cannot identify every Short; keep playback disabled until a supported isolated player exists.
        if (seconds < 240) return null
        val title = snippet.optString("title")
        val description = snippet.optString("description")
        val tags = snippet.optJSONArray("tags")?.let { array -> (0 until array.length()).joinToString(" ") { array.optString(it) } }.orEmpty()
        val metadata = "$title $description $tags"
        if (blocked(metadata, terms + setOf("shorts", "reels")) || !metadataAllowed(title)) return null
        return VideoResult(id, title, description, channel, seconds)
    }

    private fun checkAccess(key: String) {
        if (!enabled) throw VideoApiException(VideoError.DISABLED)
        if (key.isBlank()) throw VideoApiException(VideoError.MISSING_KEY)
        if (key.length > 256 || key.any { it.isWhitespace() }) throw VideoApiException(VideoError.INVALID_INPUT)
    }

    private fun validateQuery(query: String) {
        if (query.isBlank() || query.length > 200 || query.any { it.isISOControl() }) throw VideoApiException(VideoError.INVALID_INPUT)
    }

    private fun request(endpoint: String, parameters: Map<String, String>, key: String): JSONObject {
        val response = try { transport(endpoint, parameters + ("key" to key)) }
            catch (_: IOException) { throw VideoApiException(VideoError.NETWORK) }
            catch (_: SecurityException) { throw VideoApiException(VideoError.NETWORK) }
        if (response.status == 429) throw VideoApiException(VideoError.QUOTA)
        val body = try { JSONObject(response.body) } catch (_: JSONException) {
            throw VideoApiException(when (response.status) {
                401, 403 -> VideoError.CREDENTIALS
                in 200..299 -> VideoError.INVALID_RESPONSE
                else -> VideoError.API
            })
        }
        if (response.status !in 200..299 || body.has("error")) {
            val reason = body.optJSONObject("error")?.optJSONArray("errors")?.optJSONObject(0)?.optString("reason")
            throw VideoApiException(when {
                reason in setOf("quotaExceeded", "dailyLimitExceeded", "rateLimitExceeded") || response.status == 429 -> VideoError.QUOTA
                response.status in setOf(401, 403) || reason == "keyInvalid" -> VideoError.CREDENTIALS
                else -> VideoError.API
            })
        }
        if (body.optJSONArray("items") == null) throw VideoApiException(VideoError.INVALID_RESPONSE)
        return body
    }
}

private val channelId = Regex("UC[A-Za-z0-9_-]{22}")
private val videoId = Regex("[A-Za-z0-9_-]{11}")
private fun JSONObject.items(): List<JSONObject> {
    val array = getJSONArray("items")
    return (0 until array.length()).mapNotNull { array.optJSONObject(it) }
}
private fun compact(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKC)
    .lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }
private fun blocked(value: String, terms: Set<String>): Boolean {
    val normalized = compact(value)
    return terms.any { term -> compact(term).let { it.isNotEmpty() && normalized.contains(it) } }
}

internal fun durationSeconds(value: String): Long? {
    if (value.endsWith("T")) return null
    val match = Regex("P(?:(\\d+)D)?(?:T(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?)?").matchEntire(value) ?: return null
    val groups = match.groupValues.drop(1)
    if (groups.all { it.isEmpty() }) return null
    return try { groups.zip(listOf(86400L, 3600L, 60L, 1L)).fold(0L) { sum, (number, scale) ->
        Math.addExact(sum, Math.multiplyExact(if (number.isEmpty()) 0 else number.toLong(), scale))
    } } catch (_: ArithmeticException) { null } catch (_: NumberFormatException) { null }
}

internal fun httpsGet(endpoint: String, parameters: Map<String, String>, extraHeaders: Map<String, String> = emptyMap()): ApiResponse {
    require(endpoint in setOf("search", "channels", "videos"))
    val query = parameters.entries.joinToString("&") { (key, value) -> "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}" }
    val connection = URL("https://www.googleapis.com/youtube/v3/$endpoint?$query").openConnection() as HttpsURLConnection
    try {
        connection.connectTimeout = 10000
        connection.readTimeout = 15000
        connection.instanceFollowRedirects = false
        connection.useCaches = false
        extraHeaders.forEach { (name, value) -> connection.setRequestProperty(name, value) }
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader(Charsets.UTF_8)?.use { reader ->
            val chars = CharArray(1024 * 1024 + 1)
            var count = 0
            while (count < chars.size) { val read = reader.read(chars, count, chars.size - count); if (read < 0) break; count += read }
            if (count > 1024 * 1024) throw VideoApiException(VideoError.INVALID_RESPONSE)
            String(chars, 0, count)
        }.orEmpty()
        return ApiResponse(status, body)
    } finally { connection.disconnect() }
}
