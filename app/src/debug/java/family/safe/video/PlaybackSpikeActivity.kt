package family.safe.video

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import java.util.Locale

/** Adult-only feasibility harness, absent from release and never linked by child UI. */
class PlaybackSpikeActivity : Activity() {
    private var player: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getStringExtra("videoId").orEmpty()
        if (!Regex("[A-Za-z0-9_-]{11}").matches(id)) { finish(); return }
        val frame = FrameLayout(this)
        val web = WebView(this)
        player = web
        frame.addView(web, FrameLayout.LayoutParams(-1, -1))
        setContentView(frame)
        with(web.settings) {
            javaScriptEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            mediaPlaybackRequiresUserGesture = true
            setSupportMultipleWindows(false)
        }
        web.webViewClient = WebViewClient()
        web.webChromeClient = object : WebChromeClient() {
            private var custom: View? = null
            private var fullCallback: CustomViewCallback? = null
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (view == null || custom != null) { callback?.onCustomViewHidden(); return }
                custom = view
                fullCallback = callback
                frame.addView(view, FrameLayout.LayoutParams(-1, -1))
                web.visibility = View.GONE
            }
            override fun onHideCustomView() {
                custom?.let { frame.removeView(it) }
                custom = null
                web.visibility = View.VISIBLE
                fullCallback?.onCustomViewHidden()
                fullCallback = null
            }
        }
        val origin = "https://${packageName.lowercase(Locale.ROOT)}"
        // No overlays or script interventions: inspect the official player's actual escape surfaces.
        val html = """<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="referrer" content="strict-origin-when-cross-origin"></head><body style="margin:0;background:black"><iframe title="Adult playback feasibility test" style="border:0;width:100%;height:100vh" src="https://www.youtube.com/embed/$id?rel=0&amp;autoplay=0&amp;origin=$origin" allow="fullscreen; encrypted-media" allowfullscreen></iframe></body></html>"""
        web.loadDataWithBaseURL("$origin/", html, "text/html", "UTF-8", null)
    }

    override fun onPause() { player?.onPause(); super.onPause() }
    override fun onResume() { super.onResume(); player?.onResume() }
    override fun onDestroy() { player?.destroy(); player = null; super.onDestroy() }
}
