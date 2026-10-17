package family.safe

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import family.safe.data.*
import family.safe.settings.*
import family.safe.video.*
import family.safe.video.nasa.*
import kotlinx.coroutines.*
import java.io.File

data class NasaPlayback(val id: String, val preview: Boolean)

class MainActivity : ComponentActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var store: SettingsStore
    var settings by mutableStateOf<FamilySettings?>(null); private set
    var fatal by mutableStateOf(false); private set
    var busy by mutableStateOf(false); private set
    var parent by mutableStateOf(false); private set
    var gate by mutableStateOf(false)
    var message by mutableStateOf("")
    var apiKey by mutableStateOf("")
    var parentTab by mutableStateOf("Topics")
    var nasaPlayback by mutableStateOf<NasaPlayback?>(null); private set
    var nasaPreviewHashes by mutableStateOf<Map<String,String>>(emptyMap()); private set
    private var generation = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        cacheDir.listFiles()?.filter { it.isFile && it.name.startsWith("nasa-player-") }?.forEach { it.delete() }
        store = SettingsStore(File(filesDir,"settings.json"))
        scope.launch { try { settings = withContext(Dispatchers.IO) { store.load().also { store.save(it) } } } catch (_: Exception) { fatal = true } }
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary=Color(0xFF17695C),background=Color(0xFFF4F6F3),surface=Color.White)) {
                FamilyApp(this)
            }
        }
    }
    override fun onPause() {
        lock(); apiKey = ""
        super.onPause()
    }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
    fun lock() { generation++; parent = false; gate = false; message = ""; nasaPlayback = null; parentTab = "Topics"; nasaPreviewHashes = emptyMap() }
    internal fun recordNasaPreview(id: String, sha256: String) {
        if (parent && nasaPlayback == NasaPlayback(id,true) && sha256.matches(Regex("[a-f0-9]{64}"))) nasaPreviewHashes = nasaPreviewHashes + (id to sha256)
    }
    fun reviewNasa(id: String, approved: Boolean, confirmed: Boolean = false) {
        val state = settings ?: return
        val video = state.nasaVideos.find { it.id == id } ?: return
        val digest = if(approved) nasaPreviewHashes[id] else null
        if (!parent || busy || (approved && (state.categories.none { it.id == video.category && it.enabled } ||
                nasaMetadataBlocked(video, state) || !confirmed || digest == null))) return
        update { it.copy(nasaReviews = it.nasaReviews.filterNot { review -> review.id == id } + NasaReview(id, approved, digest)) }
    }
    fun openNasa(id: String, preview: Boolean = false) {
        val state = settings ?: return
        val video = state.nasaVideos.find { it.id == id } ?: return
        if (fatal || busy || state.pinHash == null || !validNasaVideo(video)) return
        if (preview) {
            if (!parent || state.categories.none { it.id == video.category && it.enabled } || nasaMetadataBlocked(video,state)) return
            nasaPreviewHashes = nasaPreviewHashes - id
        } else if (approvedNasaVideos(state).none { it.id == id }) return
        nasaPlayback = NasaPlayback(id, preview)
    }
    fun closeNasa() { nasaPlayback = null }
    fun removeNasa(id: String) {
        if (!parent || busy) return
        nasaPreviewHashes = nasaPreviewHashes - id
        update { it.copy(nasaVideos=it.nasaVideos.filterNot { video -> video.id == id },nasaReviews=it.nasaReviews.filterNot { review -> review.id == id }) }
    }
    internal fun discoverNasa(category: String, query: String, client: NasaClient = NasaClient(enabled=true)) {
        val state = settings ?: return
        if (fatal || busy || !canDiscoverNasa(state,parent,category,query)) return
        val started = generation
        busy = true
        message = "Searching NASA for parent-review candidates…"
        scope.launch {
            try {
                val candidates = withContext(Dispatchers.IO) { client.discover(query,category,state.blocked) }
                // A late network response cannot cross a lock/background/re-authentication boundary.
                if (!parent || started != generation) return@launch
                val updated = mergeNasaCandidates(state,category,candidates)
                withContext(Dispatchers.IO) { store.save(updated) }
                settings = updated
                if (parent && started == generation) {
                    val added = updated.nasaVideos.size - state.nasaVideos.size
                    message = when {
                        state.nasaVideos.size >= MAX_NASA_VIDEOS -> "Review collection is full. Remove a candidate before searching again."
                        candidates.isEmpty() -> "No eligible playable candidates found. Nothing was approved."
                        else -> "$added new candidates saved for parent review. Existing decisions were preserved. Nothing was approved automatically."
                    }
                }
            } catch (_: Exception) {
                if (parent && started == generation) message = "NASA search unavailable. No candidates or approvals were changed."
            } finally { busy = false }
        }
    }
    fun update(change: (FamilySettings) -> FamilySettings) {
        if (!parent || busy) return
        persist(change)
    }
    private fun persist(change: (FamilySettings) -> FamilySettings) {
        val old = settings ?: return
        if (busy) return
        busy = true
        scope.launch {
            try {
                val updated = change(old)
                withContext(Dispatchers.IO) { store.save(updated) }
                settings = updated
                message = "Saved on this tablet."
            } catch (_: Exception) { message = "Could not save. Please try again." }
            finally { busy = false }
        }
    }
    fun authenticate(pin: String, confirmation: String) {
        val old = settings ?: return
        if (busy) return
        if (!old.attempts.canTry(System.currentTimeMillis())) { message = "Please wait before trying again."; return }
        if (old.pinHash == null && (pin != confirmation || !pin.matches(Regex("[0-9]{6,32}")))) {
            message = "Enter the same PIN twice, using 6 to 32 digits."; return
        }
        val started = generation
        busy = true
        scope.launch {
            try {
                // Reserve a failed attempt before hashing, so process death cannot erase throttling.
                val reserved = old.copy(attempts=old.attempts.failed(System.currentTimeMillis()))
                withContext(Dispatchers.IO) { store.save(reserved) }
                settings = reserved
                val hash = old.pinHash ?: withContext(Dispatchers.Default) { PinSecurity.create(pin) }
                val valid = old.pinHash == null || withContext(Dispatchers.Default) { PinSecurity.verify(pin, hash) }
                if (valid) {
                    val unlocked = reserved.copy(pinHash=hash,attempts=PinAttempts())
                    withContext(Dispatchers.IO) { store.save(unlocked) }
                    settings = unlocked
                    if (started == generation) { parent = true; gate = false; message = "" }
                } else { message = "PIN not recognized. Please wait before trying again." }
            } catch (_: Exception) { fatal = true }
            finally { busy = false }
        }
    }
    fun requestApproval(text: String) {
        val state = settings ?: return
        if (state.pinHash == null || text.isBlank() || text.length > 256 || busy) return
        if (state.requests.size >= 50) { message = "Your parent has requests to review."; return }
        if (state.requests.any { it.text == text && it.status == "pending" }) { message = "Your parent already has this request."; return }
        persist { it.copy(requests=it.requests+ApprovalRequest(java.util.UUID.randomUUID().toString(),text)) }
        message = "Request saved for your parent."
    }
    suspend fun resolveChannels(name: String): List<ChannelCandidate> =
        androidYouTubeClient(this,enabled=BuildConfig.YOUTUBE_API_ENABLED).resolveChannels(name,apiKey)
}
