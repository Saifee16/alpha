package family.safe.browser

import java.net.URI
import java.util.Locale
import family.safe.search.EducationalSource

data class BrowserSource(
    val id: String,
    val name: String,
    val domain: String,
    val categories: Set<String>,
    val pathPrefixes: List<String> = listOf("/"),
    val enabled: Boolean = false,
    val externalLinks: Boolean = true,
    val userGenerated: Boolean = false,
)

/** Exact ASCII DNS hosts only; approval never implicitly includes a subdomain. */
object UrlPolicy {
    private val hostPattern = Regex("(?=.{1,253}$)(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,63}")
    private val forbiddenHosts = setOf("youtube.com", "youtube-nocookie.com", "youtu.be", "instagram.com", "facebook.com", "fb.com", "tiktok.com", "snapchat.com", "twitter.com", "x.com", "reddit.com", "bing.com", "duckduckgo.com", "search.yahoo.com", "yandex.com", "search.brave.com")
    private val googleHost = Regex("(?:[a-z0-9-]+\\.)*google\\.[a-z.]+")
    private val forbiddenSegments = setOf("shorts", "reels", "reel", "spotlight")

    fun allows(url: String, source: BrowserSource): Boolean {
        if (!source.enabled || source.categories.isEmpty()) return false
        val domain = source.domain.lowercase(Locale.ROOT)
        if (!hostPattern.matches(domain) || googleHost.matches(domain) || (forbiddenHosts + knownAdultHosts).any { domain == it || domain.endsWith(".$it") }) return false
        if (domain == "kids.nationalgeographic.com") return false
        val educational = EducationalSource.entries.find { it.domain == domain }
        if (source.userGenerated && educational == null) return false
        if (educational != null && (source.id != educational.id || !source.userGenerated || !educational.acceptsUrl(url.substringBefore('#')))) return false
        if (source.pathPrefixes.isEmpty() || source.pathPrefixes.any { !validPath(it) }) return false
        if (url.any { it.isWhitespace() || it.code < 32 || it.code == 127 } || '\\' in url) return false
        val uri = try { URI(url) } catch (_: Exception) { return false }
        if (uri.scheme?.lowercase(Locale.ROOT) != "https" || uri.isOpaque || uri.rawUserInfo != null || uri.port != -1) return false
        if (uri.host?.lowercase(Locale.ROOT) != domain || uri.rawAuthority?.lowercase(Locale.ROOT) != domain) return false
        val path = uri.rawPath.orEmpty().ifEmpty { "/" }
        if (!validPath(path)) return false
        if (educational != null && source.pathPrefixes != listOf("/wiki") && path !in source.pathPrefixes) return false
        if (path.lowercase(Locale.ROOT).split('/').any { it in forbiddenSegments }) return false
        return source.pathPrefixes.any { prefix ->
            val boundary = prefix.trimEnd('/')
            boundary.isEmpty() || path == boundary || path.startsWith("$boundary/")
        }
    }

    fun startUrl(source: BrowserSource): String? {
        EducationalSource.entries.find { it.id == source.id && it.domain == source.domain }?.let { provider ->
            val title = if(provider == EducationalSource.VIKIDIA) "Main Page" else "Wikijunior:Introduction to Mathematics"
            return provider.articleUrl(title)?.takeIf { allows(it,source) }
        }
        val path = source.pathPrefixes.firstOrNull() ?: return null
        return "https://${source.domain}$path".takeIf { allows(it, source) }
    }

    fun redirectUrl(current: String, location: String, source: BrowserSource): String? =
        try { URI(current).resolve(location).toString().takeIf { allows(it, source) } } catch (_: Exception) { null }

    private fun validPath(path: String): Boolean = path.startsWith('/') &&
        path.none { it == '%' || it == '\\' || it == '?' || it == '#' || it == ';' || it.isWhitespace() || it.code < 32 } &&
        "//" !in path && path.split('/').none { it == "." || it == ".." }
}
