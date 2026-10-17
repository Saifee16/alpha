package family.safe.video.nasa

import android.content.Context
import android.view.View
import android.widget.Button as NativeButton
import android.widget.LinearLayout
import android.widget.MediaController
import android.widget.TextView
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import family.safe.R
import family.safe.BuildConfig
import java.io.File
import java.security.MessageDigest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

@Composable fun NasaPlayer(video: NasaVideo, preview: Boolean, expectedSha256: String?, onPreview: (String) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val native = remember(video.id, expectedSha256) { NasaPlayerView(context,video,expectedSha256,onPreview) }
    DisposableEffect(native) {
        val lifecycle=(context as LifecycleOwner).lifecycle
        val observer=LifecycleEventObserver { _,event -> if(event==Lifecycle.Event.ON_PAUSE) native.close() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); native.close() }
    }
    BackHandler { onClose() }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Row {
                Text(if(preview) "Parent preview" else video.title,Modifier.weight(1f),style=MaterialTheme.typography.titleLarge)
                TextButton(onClick=onClose) { Text("Close video") }
            }
            if(preview) Text(video.title)
            Text(video.credit,style=MaterialTheme.typography.bodyMedium)
            AndroidView(factory={native},modifier=Modifier.fillMaxWidth().weight(1f))
            Text("No next video or external navigation. Leaving this screen stops playback.",style=MaterialTheme.typography.bodySmall)
        }
    }
}

/** The native media stack receives only a verified private file, never an HTTP URL. */
internal class NasaPlayerView(context: Context, private val candidate: NasaVideo, private val expectedSha256: String?, private val onPreview: (String) -> Unit) : LinearLayout(context) {
    private val status = TextView(context).apply { tag="nasaStatus" }
    private val retry = NativeButton(context).apply { tag="nasaRetry"; setText(R.string.nasa_retry) }
    private val video = VideoView(context).apply { tag="nasaVideo" }
    @Volatile private var closed = false
    private var worker: Thread? = null
    private var mediaFile: File? = null

    init {
        orientation = VERTICAL
        addView(status)
        addView(retry)
        addView(video,LayoutParams(-1,0,1f))
        val controls = MediaController(context)
        controls.setAnchorView(video)
        video.setMediaController(controls)
        video.setOnCompletionListener { if(!closed) status.setText(R.string.nasa_complete) }
        video.setOnErrorListener { _,_,_ ->
            if(!closed) {
                mediaFile?.delete(); mediaFile=null
                status.setText(R.string.nasa_playback_failed); retry.visibility=View.VISIBLE
            }
            true
        }
        retry.setOnClickListener { download() }
        download()
    }

    private fun download() {
        if(closed || worker?.isAlive == true) return
        video.stopPlayback()
        mediaFile?.delete(); mediaFile=null
        status.setText(R.string.nasa_loading)
        retry.visibility=View.GONE
        worker = Thread {
            var target: File? = null
            try {
                val asset = NasaClient(enabled=true).mobileAsset(candidate.id)
                val ready = File.createTempFile("nasa-player-",".mp4",context.cacheDir)
                target = ready
                ready.outputStream().use { downloadNasaVideo(candidate.id,asset,it) }
                val digest = verifiedNasaDigest(ready,expectedSha256)
                post {
                    if(closed) ready.delete()
                    else {
                        mediaFile=ready
                        video.setOnPreparedListener {
                            if(!closed) {
                                if(expectedSha256==null) onPreview(digest)
                                status.setText(R.string.nasa_ready)
                                video.start()
                            }
                        }
                        video.setVideoPath(ready.absolutePath)
                    }
                }
            } catch (failure: Exception) {
                if(BuildConfig.DEBUG && !closed) android.util.Log.w("FamilySafeNasa","Transfer failed: ${failure.javaClass.simpleName}")
                target?.delete()
                post { if(!closed) { status.setText(R.string.nasa_download_failed); retry.visibility=View.VISIBLE } }
            } finally { if(closed) target?.delete() }
        }.apply { name="FamilySafe-NASA"; start() }
    }

    fun close() {
        closed=true
        worker?.interrupt()
        video.stopPlayback()
        mediaFile?.delete(); mediaFile=null
    }
}

internal fun verifiedNasaDigest(file: File, expected: String?): String {
    val digest=MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer=ByteArray(8192)
        while(true) {
            check(!Thread.currentThread().isInterrupted)
            val count=input.read(buffer)
            if(count<0) break
            digest.update(buffer,0,count)
        }
    }
    val actual=digest.digest().joinToString("") { "%02x".format(it) }
    require(expected==null || expected==actual) { "Asset changed; parent review required" }
    return actual
}
