package family.safe.video.nasa

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.MediaController
import android.widget.TextView
import android.widget.VideoView
import java.io.File

/** Unexported adult-only test harness. There is no link from child or parent screens. */
class NasaPlaybackSpikeActivity : Activity() {
    private lateinit var video: VideoView
    private var worker: Thread? = null
    private var mediaFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val status = TextView(this).apply {
            tag = "nasaStatus"
            text = "Adult NASA playback test. Content and rights NOT reviewed for child access. Source: NASA/JPL-Caltech/ASU; produced by True Story Films."
        }
        val load = Button(this).apply { tag = "nasaLoad"; text = "Download and test reference video" }
        val close = Button(this).apply { text = "Close test"; setOnClickListener { finish() } }
        video = VideoView(this).apply { tag = "nasaVideo" }
        val controls = MediaController(this)
        controls.setAnchorView(video)
        video.setMediaController(controls)
        video.setOnErrorListener { _, _, _ -> status.text = "Native playback failed. No external fallback."; true }
        video.setOnCompletionListener { status.text = "Completed. No next video or external navigation." }
        load.setOnClickListener {
            load.isEnabled = false
            status.text = "Downloading a bounded reference MP4 over HTTPS; redirects denied."
            worker = Thread {
                var target: File? = null
                try {
                    val asset = NasaClient(enabled = true).mobileAsset(NasaReference.ID)
                    require(asset == nasaMobileUrl(asset, NasaReference.ID))
                    val destination = File.createTempFile("nasa-spike-", ".mp4", cacheDir)
                    target = destination
                    destination.outputStream().use { downloadNasaReference(asset, it) }
                    val ready = destination
                    runOnUiThread {
                        if (isFinishing || isDestroyed) ready.delete()
                        else {
                            mediaFile = ready
                            // The native player receives only a private local file, never a network URL it could redirect.
                            video.setOnPreparedListener { status.text = "Source: NASA/JPL-Caltech/ASU; produced by True Story Films. Adult test only."; video.start() }
                            video.setVideoPath(ready.absolutePath)
                        }
                    }
                } catch (_: Exception) {
                    target?.delete()
                    runOnUiThread { if (!isDestroyed) { status.text = "NASA download denied or failed. No fallback."; load.isEnabled = true } }
                }
            }.also { it.start() }
        }
        layout.addView(status)
        layout.addView(load)
        layout.addView(close)
        layout.addView(video, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(layout)
    }

    override fun onStop() {
        video.stopPlayback()
        worker?.interrupt()
        mediaFile?.delete()
        super.onStop()
        finish()
    }
}
