package family.safe.video.nasa

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
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
import family.safe.MainActivity
import family.safe.video.archive.ArchiveClient
import family.safe.video.archive.ArchiveLesson
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
            Text("${video.source} · ${video.credit}",style=MaterialTheme.typography.bodyMedium)
            AndroidView(factory={native},modifier=Modifier.fillMaxWidth().weight(1f))
            Text("Close to return to Discover.",style=MaterialTheme.typography.bodySmall)
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
    private var transferOperation: NasaOperation? = null
    private val main = Handler(Looper.getMainLooper())
    private val attempts = NasaPlaybackAttempt()
    private var preparationTimeout: Runnable? = null

    init {
        orientation = VERTICAL
        addView(status)
        addView(retry)
        addView(video,LayoutParams(-1,0,1f))
        val controls = MediaController(context)
        controls.setAnchorView(video)
        video.setMediaController(controls)
        retry.setOnClickListener { download() }
        download()
    }

    private fun download() {
        if(closed || worker?.isAlive == true) return
        val token = attempts.begin()
        val startedMillis = SystemClock.elapsedRealtime()
        (context as? MainActivity)?.performance?.apply {
            remove("video_transfer_ms"); remove("video_prepare_ms"); remove("video_total_ms")
        }
        clearNativePlayback()
        mediaFile?.delete(); mediaFile=null
        status.setText(R.string.nasa_loading)
        retry.visibility=View.GONE
        val operation = NasaOperation(timeoutMillis=120_000)
        transferOperation = operation
        worker = Thread {
            var target: File? = null
            try {
                val archive = candidate.provider == "archive"
                require(validNasaVideo(candidate))
                val snapshot = if(archive) requireNotNull(ArchiveLesson.find(candidate.id)) else null
                val asset = if(archive) ArchiveClient(enabled=true,operation=operation).assetUrl(candidate.id)
                    else NasaClient(enabled=true,operation=operation).mobileAsset(candidate.id)
                operation.check()
                val ready = File.createTempFile("nasa-player-",".mp4",context.cacheDir)
                target = ready
                ready.outputStream().use {
                    if(archive) ArchiveClient(enabled=true,operation=operation).download(asset,it)
                    else downloadNasaVideo(candidate.id,asset,it,operation)
                }
                val digest = verifiedNasaDigest(ready,expectedSha256 ?: snapshot?.sha256,operation)
                if(archive) require(digest == snapshot?.sha256) { "Vetted Archive snapshot changed" }
                val transferredMillis = SystemClock.elapsedRealtime()
                main.post {
                    if(closed || !attempts.preparing(token)) ready.delete()
                    else {
                        val preparationStartedMillis = SystemClock.elapsedRealtime()
                        mediaFile=ready
                        video.setOnCompletionListener { if(attempts.isReady(token)) status.setText(R.string.nasa_complete) }
                        video.setOnErrorListener { _,_,_ ->
                            if(attempts.fail(token)) playbackFailed()
                            true
                        }
                        video.setOnPreparedListener {
                            if(!closed && attempts.prepared(token)) {
                                cancelPreparationTimeout()
                                val preparedMillis = SystemClock.elapsedRealtime()
                                (context as? MainActivity)?.performance?.putAll(mapOf(
                                    "video_transfer_ms" to (transferredMillis-startedMillis),
                                    "video_prepare_ms" to (preparedMillis-preparationStartedMillis),
                                    "video_total_ms" to (preparedMillis-startedMillis)
                                ))
                                if(expectedSha256==null) onPreview(digest)
                                status.setText(R.string.nasa_ready)
                                video.start()
                            }
                        }
                        preparationTimeout = Runnable {
                            if(attempts.timedOut(token)) playbackFailed()
                        }.also { main.postDelayed(it, 30_000) }
                        video.setVideoPath(ready.absolutePath)
                    }
                }
            } catch (failure: Exception) {
                if(BuildConfig.DEBUG && !closed) android.util.Log.w("FamilySafeNasa","Transfer failed: ${failure.javaClass.simpleName}")
                target?.delete()
                main.post { if(!closed && attempts.fail(token)) { status.setText(R.string.nasa_download_failed); retry.visibility=View.VISIBLE } }
            } finally { operation.close(); if(closed) target?.delete() }
        }.apply { name="FamilySafe-NASA"; start() }
    }

    fun close() {
        closed=true
        attempts.cancel()
        transferOperation?.cancel()
        worker?.interrupt()
        clearNativePlayback()
        mediaFile?.delete(); mediaFile=null
    }

    private fun cancelPreparationTimeout() {
        preparationTimeout?.let(main::removeCallbacks)
        preparationTimeout=null
    }

    private fun clearNativePlayback() {
        cancelPreparationTimeout()
        video.setOnPreparedListener(null)
        video.setOnCompletionListener(null)
        video.setOnErrorListener(null)
        video.stopPlayback()
    }

    private fun playbackFailed() {
        clearNativePlayback()
        mediaFile?.delete(); mediaFile=null
        status.setText(R.string.nasa_playback_failed)
        retry.visibility=View.VISIBLE
    }
}

internal fun verifiedNasaDigest(file: File, expected: String?, operation: NasaOperation? = null): String {
    operation?.check()
    val digest=MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer=ByteArray(8192)
        while(true) {
            check(!Thread.currentThread().isInterrupted)
            operation?.check()
            val count=input.read(buffer)
            if(count<0) break
            digest.update(buffer,0,count)
        }
    }
    val actual=digest.digest().joinToString("") { "%02x".format(it) }
    operation?.check()
    require(expected==null || expected==actual) { "Asset changed; parent review required" }
    return actual
}
