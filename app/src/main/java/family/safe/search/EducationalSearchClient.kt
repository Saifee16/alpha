package family.safe.search

import family.safe.policy.QueryPolicy
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.net.CookieHandler
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.net.ssl.HttpsURLConnection

enum class EducationalSource(val id: String, val displayName: String, val domain: String, val namespace: Int) {
    VIKIDIA("vikidia", "Vikidia", "en.vikidia.org", 0),
    WIKIJUNIOR("wikijunior", "Wikijunior", "en.wikibooks.org", 110);

    fun articleUrl(title: String): String? {
        if (title.length !in 1..200 || title != title.trim()) return null
        val article = if (this == WIKIJUNIOR) {
            if (!title.startsWith("Wikijunior:")) return null
            title.removePrefix("Wikijunior:")
        } else title
        if (article.split('/').any { part ->
                part.isBlank() || part == "." || part == ".." ||
                    !part.matches(Regex("[A-Za-z0-9][A-Za-z0-9 _(),'.!+\\-]*"))
            }) return null
        return "https://$domain/wiki/${title.replace(' ', '_')}"
    }

    fun acceptsUrl(url: String): Boolean {
        val uri = try { URI(url) } catch (_: Exception) { return false }
        if (uri.scheme != "https" || uri.rawAuthority != domain || uri.rawQuery != null ||
            uri.rawFragment != null || !uri.rawPath.orEmpty().startsWith("/wiki/")) return false
        val title = uri.rawPath.removePrefix("/wiki/").replace('_', ' ')
        return articleUrl(title) == url
    }
}

data class EducationalResult(val source: EducationalSource, val title: String, val url: String)
data class EducationalResponse(val status: Int, val body: String)

/** The caller authorizes the complete phrase and enabled source/category before requesting search. */
class EducationalSearchClient(private val transport: ((String) -> EducationalResponse)? = null) {
    private val active = AtomicReference<Operation?>(null)

    fun cancel() { active.get()?.cancel() }

    /** Sanctioned article body, with no redirect resolution or site-navigation shell. */
    fun article(source: EducationalSource, url: String): String {
        require(source.acceptsUrl(url)) { "Invalid article destination" }
        val title = URI(url).rawPath.removePrefix("/wiki/").replace('_', ' ')
        val op = Operation()
        check(active.compareAndSet(null, op)) { "Search already running" }
        val timer = Timer("educational-article-deadline", true)
        timer.schedule(object : TimerTask() { override fun run() { op.cancel() } }, 20_000)
        try {
            op.check()
            val endpoint = "https://${source.domain}/w/api.php?action=parse&page=${URLEncoder.encode(title, "UTF-8")}&prop=text%7Crevid&disableeditsection=1&disablelimitreport=1&format=json&formatversion=2"
            val response = transport?.invoke(endpoint) ?: request(endpoint, op)
            op.check()
            check(response.status == 200) { "Article unavailable" }
            require(response.body.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Article too large" }
            val parsed = JSONObject(response.body).getJSONObject("parse")
            check(parsed.getString("title") == title && parsed.getLong("pageid") > 0 && parsed.getLong("revid") > 0) { "Article identity changed" }
            val body = parsed.getString("text")
            check(body.isNotBlank()) { "Empty article" }
            val license = if (source == EducationalSource.VIKIDIA) "3.0" else "4.0"
            // The title/URL alphabet excludes HTML delimiters; only the provider body is untrusted markup.
            val html = """<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>$title</title>
                <style>body{font:18px/1.6 sans-serif;max-width:65ch;margin:auto;padding:20px;color:#172b25;background:#fff}h1,h2,h3{line-height:1.25}table{max-width:100%;overflow:auto;display:block}img,video,audio,iframe,form,.mw-editsection{display:none}a{color:#17634f}footer{margin-top:32px;border-top:1px solid #ccc;font-size:14px;overflow-wrap:anywhere}</style></head><body>
                <h1>$title</h1>$body<footer>Source: ${source.displayName} contributors, revision ${parsed.getLong("revid")}.<br><a href="$url">$url</a><br>Text: CC BY-SA $license (https://creativecommons.org/licenses/by-sa/$license/), unless otherwise noted in the article. Layout adapted by FamilySafe; scripts, images and interactive content are unavailable. Community edited content requires adult supervision.</footer></body></html>""".trimIndent()
            op.check()
            return html
        } finally {
            timer.cancel()
            op.cancel()
            active.compareAndSet(op, null)
        }
    }

    fun search(source: EducationalSource, query: String, blocked: Set<String>): List<EducationalResult> {
        require(query.isNotBlank() && query.length <= 256 && query.none {
            Character.isISOControl(it) || Character.getType(it) in setOf(
                Character.FORMAT.toInt(), Character.SURROGATE.toInt(),
                Character.PRIVATE_USE.toInt(), Character.UNASSIGNED.toInt())
        }) { "Invalid search query" }
        require(!QueryPolicy.containsBlocked(query, blocked)) { "Blocked search query" }
        val normalized = QueryPolicy.normalize(query)
        val required = words(normalized)
        require(required.isNotEmpty()) { "Invalid search query" }
        val op = Operation()
        check(active.compareAndSet(null, op)) { "Search already running" }
        val timer = Timer("educational-search-deadline", true)
        timer.schedule(object : TimerTask() { override fun run() { op.cancel() } }, 20_000)
        try {
            op.check()
            val encoded = URLEncoder.encode(normalized, "UTF-8")
            val url = "https://${source.domain}/w/api.php?action=query&list=search&srsearch=$encoded&srnamespace=${source.namespace}&srlimit=5&srprop=&format=json&formatversion=2"
            val response = transport?.invoke(url) ?: request(url, op)
            op.check()
            check(response.status == 200) { "Educational search unavailable" }
            require(response.body.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Search response too large" }
            val root = JSONObject(response.body)
            check(!root.has("error")) { "Educational search unavailable" }
            val items = root.getJSONObject("query").getJSONArray("search")
            val seen = mutableSetOf<Long>()
            val results = mutableListOf<EducationalResult>()
            for (index in 0 until minOf(items.length(), 5)) {
                op.check()
                val item = items.optJSONObject(index) ?: continue
                val ns = item.opt("ns")
                val pageId = item.opt("pageid")
                if (ns !is Int || ns != source.namespace || pageId !is Number ||
                    pageId !is Int && pageId !is Long || pageId.toLong() <= 0) continue
                val title = item.opt("title") as? String ?: continue
                val articleUrl = source.articleUrl(title) ?: continue
                if (QueryPolicy.containsBlocked(title, blocked) || !words(title).containsAll(required)) continue
                if (seen.add(pageId.toLong())) results += EducationalResult(source, title, articleUrl)
            }
            op.check()
            return results
        } finally {
            timer.cancel()
            op.cancel()
            active.compareAndSet(op, null)
        }
    }

    private fun request(url: String, op: Operation): EducationalResponse {
        check(CookieHandler.getDefault() == null) { "Cookie-enabled transport is unavailable" }
        val connection = URL(url).openConnection() as HttpsURLConnection
        op.connection = connection
        op.check()
        connection.instanceFollowRedirects = false
        connection.useCaches = false
        connection.connectTimeout = op.timeout()
        connection.readTimeout = op.timeout()
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "FamilySafe/0.1 (https://github.com/Saifee16/alpha; educational reader)")
        connection.setRequestProperty("Cache-Control", "no-store")
        connection.setRequestProperty("Cookie", "")
        op.check()
        val status = connection.responseCode
        op.check()
        if (status != 200) return EducationalResponse(status, "")
        require(connection.contentType.orEmpty().substringBefore(';').trim().equals("application/json", true)) { "Invalid search response type" }
        require(connection.contentLengthLong <= MAX_BYTES) { "Search response too large" }
        val output = ByteArrayOutputStream()
        connection.inputStream.use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                op.check()
                connection.readTimeout = op.timeout()
                val count = input.read(buffer)
                op.check()
                if (count == -1) break
                require(count > 0 && output.size() + count <= MAX_BYTES) { "Invalid search response size" }
                output.write(buffer, 0, count)
            }
        }
        op.check()
        return EducationalResponse(status, output.toString("UTF-8"))
    }

    private class Operation {
        val terminal = AtomicBoolean(false)
        val deadline = System.nanoTime() + 20_000_000_000L
        @Volatile var connection: HttpsURLConnection? = null
        fun check() {
            check(!terminal.get() && !Thread.currentThread().isInterrupted && System.nanoTime() < deadline) { "Search cancelled or timed out" }
        }
        fun timeout(): Int {
            check()
            return ((deadline - System.nanoTime()) / 1_000_000).coerceIn(1, 5_000).toInt()
        }
        fun cancel() {
            if (terminal.compareAndSet(false, true)) {
                val current = connection
                if (current != null) Thread({ runCatching { current.disconnect() } }, "educational-search-disconnect").apply { isDaemon = true }.start()
            }
        }
    }

    companion object {
        private const val MAX_BYTES = 1_048_576
        private fun words(text: String): Set<String> = QueryPolicy.normalize(text).split(' ').filter { it.isNotEmpty() }.map {
            when {
                it in setOf("math", "maths", "mathematics") -> "math"
                it.length > 3 && it.endsWith("s") -> it.dropLast(1)
                else -> it
            }
        }.toSet()
    }
}
