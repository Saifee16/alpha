package family.safe.data

import family.safe.browser.BrowserSource
import family.safe.policy.Category
import family.safe.settings.PinAttempts
import family.safe.video.TrustedChannel
import family.safe.video.nasa.NasaCatalog
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import org.json.JSONArray
import org.json.JSONObject

data class ApprovalRequest(val id: String, val text: String, val status: String = "pending")
data class NasaReview(val id: String, val approved: Boolean, val sha256: String? = null)
data class FamilySettings(
    val pinHash: String? = null,
    val attempts: PinAttempts = PinAttempts(),
    val categories: List<Category> = starterCategories(),
    val blocked: Set<String> = setOf("porn", "sex", "nude", "adult", "tiktok", "instagram", "facebook", "snapchat", "shorts", "reels"),
    val sources: List<BrowserSource> = starterSources(),
    val channels: List<TrustedChannel> = emptyList(),
    val requests: List<ApprovalRequest> = emptyList(),
    val nasaReviews: List<NasaReview> = emptyList()
)

/** App-private file; malformed existing data throws and the UI stays locked. */
class SettingsStore(private val file: File) {
    companion object { private val fileLock = Any() }
    fun load(): FamilySettings = synchronized(fileLock) {
        if (!file.exists()) return FamilySettings()
        require(file.length() in 1..1_048_576)
        val json = JSONObject(file.readText())
        require(json.getInt("version") == 1)
        FamilySettings(
            pinHash = json.getString("pinHash").takeIf { it.isNotEmpty() },
            attempts = PinAttempts(json.getInt("failures"), json.getLong("nextAllowedAt")),
            categories = json.getJSONArray("categories").objects().map { Category(it.getString("id"), it.getString("name"), it.getJSONArray("terms").strings(), it.getBoolean("enabled")) },
            blocked = json.getJSONArray("blocked").strings(),
            sources = json.getJSONArray("sources").objects().map { BrowserSource(it.getString("id"),it.getString("name"),it.getString("domain"),it.getJSONArray("categories").strings(),it.getJSONArray("paths").strings().toList(),it.getBoolean("enabled"),it.getBoolean("externalLinks"),it.getBoolean("userGenerated")) },
            channels = json.getJSONArray("channels").objects().map { TrustedChannel(it.getString("id"),it.getString("name"),it.getJSONArray("categories").strings(),it.getLong("approvedAt")) }.filter { it.approvedAt <= System.currentTimeMillis() && System.currentTimeMillis()-it.approvedAt < 30L*24*60*60*1000 },
            requests = json.getJSONArray("requests").objects().map { ApprovalRequest(it.getString("id"),it.getString("text"),it.getString("status")) },
            nasaReviews = if (json.has("nasaReviews")) json.getJSONArray("nasaReviews").objects().map { NasaReview(it.getString("id"),it.getBoolean("approved"),it.optString("sha256").takeIf(String::isNotEmpty)) } else emptyList()
        ).also { validate(it) }
    }
    fun save(state: FamilySettings) = synchronized(fileLock) {
        validate(state)
        val json = JSONObject().put("version",1).put("pinHash",state.pinHash.orEmpty())
            .put("failures",state.attempts.failures).put("nextAllowedAt",state.attempts.nextAllowedAt)
            .put("categories",JSONArray(state.categories.map { JSONObject().put("id",it.id).put("name",it.name).put("terms",JSONArray(it.terms.toList())).put("enabled",it.enabled) }))
            .put("blocked",JSONArray(state.blocked.toList()))
            .put("sources",JSONArray(state.sources.map { JSONObject().put("id",it.id).put("name",it.name).put("domain",it.domain).put("categories",JSONArray(it.categories.toList())).put("paths",JSONArray(it.pathPrefixes)).put("enabled",it.enabled).put("externalLinks",it.externalLinks).put("userGenerated",it.userGenerated) }))
            .put("channels",JSONArray(state.channels.map { JSONObject().put("id",it.id).put("name",it.name).put("categories",JSONArray(it.categories.toList())).put("approvedAt",it.approvedAt) }))
            .put("requests",JSONArray(state.requests.map { JSONObject().put("id",it.id).put("text",it.text).put("status",it.status) }))
            .put("nasaReviews",JSONArray(state.nasaReviews.map { JSONObject().put("id",it.id).put("approved",it.approved).put("sha256",it.sha256.orEmpty()) }))
        file.parentFile?.mkdirs()
        val bytes = json.toString().toByteArray(Charsets.UTF_8)
        require(bytes.size <= 1_048_576)
        val temp = Files.createTempFile(requireNotNull(file.absoluteFile.parentFile).toPath(),"settings-",".pending").toFile()
        try {
            temp.outputStream().use { it.write(bytes); it.fd.sync() }
            Files.move(temp.toPath(),file.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING)
        } finally { temp.delete() }
    }
    private fun validate(state: FamilySettings) {
        require(state.pinHash != null || (state.categories.none { it.enabled } && state.sources.none { it.enabled } && state.channels.isEmpty() && state.requests.isEmpty() && state.nasaReviews.isEmpty()))
        require(state.nasaReviews.size <= NasaCatalog.videos.size && state.nasaReviews.map { it.id }.distinct().size == state.nasaReviews.size)
        require(state.nasaReviews.all { NasaCatalog.find(it.id) != null })
        require(state.nasaReviews.all { if (it.approved) it.sha256?.matches(Regex("[a-f0-9]{64}")) == true else it.sha256 == null })
        require(state.categories.size <= 50 && state.sources.size <= 30 && state.channels.size <= 30 && state.requests.size <= 50 && state.blocked.size <= 100)
        require(state.attempts.failures in 0..20 && state.attempts.nextAllowedAt >= 0)
    }
}
private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
private fun JSONArray.strings() = (0 until length()).map { getString(it) }.toSet()

fun starterCategories() = listOf(
    Category("science","Science",setOf("science","plant cells","photosynthesis","how plants grow")),
    Category("math","Mathematics",setOf("mathematics","math","maths","fractions","multiplication")),
    Category("space","Space",setOf("space","solar system","planets","moon","how stars form")),
    Category("animals","Animals",setOf("animals","animal habitats","dinosaurs")),
    Category("geography","Geography",setOf("geography","continents","oceans")),
    Category("history","History",setOf("history","ancient egypt","roman history")),
    Category("art","Drawing and Art",setOf("drawing","art","how to draw animals")),
    Category("documentaries","Educational Documentaries",setOf("educational documentaries","nature documentary")),
    Category("sports","Football and Sports",setOf("football","sports","football rules"))
)
fun starterSources() = listOf(
    BrowserSource("nasa","NASA Space Place","spaceplace.nasa.gov",setOf("science","space")),
    BrowserSource("natgeo","National Geographic Kids","kids.nationalgeographic.com",setOf("animals","science","geography","history")),
    BrowserSource("bbc","BBC Bitesize","www.bbc.co.uk",setOf("science","math","geography","history"),listOf("/bitesize"))
)
