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
import kotlinx.coroutines.*
import java.io.File

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
    private var generation = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
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
    fun lock() { generation++; parent = false; gate = false; message = "" }
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
