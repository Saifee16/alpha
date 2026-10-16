package family.safe.video

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/** Adds the installed application's identity required by Android-restricted REST API keys. */
fun androidYouTubeClient(
    context: Context,
    enabled: Boolean,
    metadataAllowed: (String) -> Boolean = { false }
): YouTubeClient {
    val app = context.applicationContext
    return YouTubeClient(enabled, metadataAllowed) { endpoint, parameters ->
        val certificate = try {
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT >= 28) {
                app.packageManager.getPackageInfo(app.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                    .signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
            } else {
                app.packageManager.getPackageInfo(app.packageName, PackageManager.GET_SIGNATURES)
                    .signatures?.firstOrNull()?.toByteArray()
            }
        } catch (_: PackageManager.NameNotFoundException) {
            throw VideoApiException(VideoError.CREDENTIALS)
        } ?: throw VideoApiException(VideoError.CREDENTIALS)
        if (certificate.isEmpty()) throw VideoApiException(VideoError.CREDENTIALS)
        httpsGet(endpoint, parameters, androidRequestHeaders(app.packageName, certificate))
    }
}

internal fun androidRequestHeaders(packageName: String, certificate: ByteArray): Map<String, String> = mapOf(
    "X-Android-Package" to packageName,
    "X-Android-Cert" to MessageDigest.getInstance("SHA-1").digest(certificate)
        .joinToString("") { byte -> "%02X".format(byte) }
)
