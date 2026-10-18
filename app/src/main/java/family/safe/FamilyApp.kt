package family.safe

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import family.safe.browser.*
import family.safe.data.*
import family.safe.policy.*
import family.safe.video.*
import family.safe.video.nasa.*
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import family.safe.search.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable fun FamilyApp(app: MainActivity) {
    val state = app.settings
    var source by remember { mutableStateOf<BrowserSource?>(null) }
    var destination by remember { mutableStateOf<String?>(null) }
    val search = app.childSearch
    DisposableEffect(app) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_PAUSE) search.cancel(clear=true) }
        app.lifecycle.addObserver(observer)
        onDispose { app.lifecycle.removeObserver(observer); search.cancel(clear=true) }
    }
    if (app.fatal) { Page("Settings locked") { Text("Saved settings could not be read safely. Ask a parent to recover this tablet's app data. Rules have not been reset.") }; return }
    if (state == null) { Page("FamilySafe") { CircularProgressIndicator() }; return }
    val playback = app.nasaPlayback
    if (playback != null) {
        val video = state.nasaVideos.find { it.id == playback.id }
        val permitted = if (playback.preview) app.parent && video != null &&
            videoSourceEnabled(video,state) && state.categories.any { it.id == video.category && it.enabled } && !nasaMetadataBlocked(video,state)
            else approvedNasaVideos(state).any { it.id == playback.id }
        if (video != null && permitted) {
            val expected = if(playback.preview) null else state.nasaReviews.single { it.id == video.id && it.approved }.sha256
            NasaPlayer(video, playback.preview, expected, { digest -> app.recordNasaPreview(video.id,digest) }) { app.closeNasa() }; return
        }
        app.closeNasa()
    }
    val activeCategories = state.categories.filter { it.enabled }.map { it.id }.toSet()
    val sites = state.sources.filter { it.enabled && it.categories.any(activeCategories::contains) }
    if (!app.parent && !app.gate && source != null) {
        val permitted = sites.find { it.id == source?.id }
        val decision=QueryPolicy.evaluate(search.query,state.categories,state.blocked)
        if (permitted != null && decision.allowed && permitted.categories.any(decision.categoryIds::contains) &&
            (destination == null || UrlPolicy.allows(destination!!,permitted))) {
            val scoped = if(destination != null && EducationalSource.entries.any { it.id == permitted.id && it.domain == permitted.domain })
                permitted.copy(pathPrefixes=listOf(java.net.URI(destination).rawPath)) else permitted
            ProtectedBrowser(scoped,destination) { source=null; destination=null }; return
        }
        source = null
    }
    BackHandler(enabled=app.parent || app.gate) { app.lock() }
    if (app.parent) {
        ParentScreen(app,state)
    } else if (app.gate || state.pinHash == null) {
        PinScreen(app,state.pinHash == null)
    } else {
        HomeScreen(app,state,sites,search) { site,url -> source=site; destination=url }
    }
}

@Composable fun Page(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text(title,style=MaterialTheme.typography.headlineLarge)
            content()
        }
    }
}

@Composable private fun PinScreen(app: MainActivity, setup: Boolean) {
    var pin by remember { mutableStateOf("") }; var confirm by remember { mutableStateOf("") }
    Page(if(setup) "Welcome to FamilySafe" else "Parent access") {
        Text(if(setup) "A parent should set up this tablet first. Choose a private PIN with at least 6 digits. There is no default PIN." else "Enter your parent PIN.")
        OutlinedTextField(pin,{ pin=it.take(32) },label={Text("Parent PIN")},visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword,autoCorrectEnabled=false),singleLine=true,enabled=!app.busy)
        if(setup) OutlinedTextField(confirm,{ confirm=it.take(32) },label={Text("Confirm PIN")},visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword,autoCorrectEnabled=false),singleLine=true,enabled=!app.busy)
        Button(onClick={ app.authenticate(pin,confirm); pin=""; confirm="" },enabled=!app.busy) { Text(if(setup) "Create PIN" else "Unlock") }
        if(!setup) TextButton(onClick={app.lock()}) { Text("Back home") }
        if(app.busy) Text("Checking PIN…")
        Text(app.message)
    }
}
