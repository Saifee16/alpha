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
import kotlinx.coroutines.launch

@Composable fun FamilyApp(app: MainActivity) {
    val state = app.settings
    var source by remember { mutableStateOf<BrowserSource?>(null) }
    if (app.fatal) { Page("Settings locked") { Text("Saved settings could not be read safely. Ask a parent to recover this tablet's app data. Rules have not been reset.") }; return }
    if (state == null) { Page("FamilySafe") { CircularProgressIndicator() }; return }
    val activeCategories = state.categories.filter { it.enabled }.map { it.id }.toSet()
    val sites = state.sources.filter { it.enabled && it.categories.any(activeCategories::contains) }
    if (!app.parent && !app.gate && source != null) {
        val permitted = sites.find { it.id == source?.id }
        if (permitted != null) { ProtectedBrowser(permitted) { source=null }; return }
        source = null
    }
    BackHandler(enabled=app.parent || app.gate) { app.lock() }
    if (app.parent) {
        ParentScreen(app,state)
    } else if (app.gate || state.pinHash == null) {
        PinScreen(app,state.pinHash == null)
    } else {
        HomeScreen(app,state,sites) { source=it }
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
        if(app.busy) CircularProgressIndicator()
        Text(app.message)
    }
}

@Composable private fun HomeScreen(app: MainActivity,state:FamilySettings,sites:List<BrowserSource>,open:(BrowserSource)->Unit) {
    var query by remember { mutableStateOf("") }
    var decision by remember { mutableStateOf<QueryDecision?>(null) }
    var results by remember { mutableStateOf<List<VideoResult>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    Page("FamilySafe") {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            Text("A little curiosity goes a long way.",style=MaterialTheme.typography.titleLarge)
            TextButton(onClick={app.gate=true}) { Text("Parent settings") }
        }
        OutlinedTextField(query,{query=it.take(256);decision=null;results=emptyList();feedback=""},enabled=!searching,label={Text("What would you like to learn?")},singleLine=true,modifier=Modifier.fillMaxWidth())
        Button(onClick={
            decision=QueryPolicy.evaluate(query,state.categories,state.blocked)
            results=emptyList();feedback=""
            if(decision?.allowed==true) {
                if (!BuildConfig.YOUTUBE_API_ENABLED) feedback="Video discovery is not set up. Explore an approved website below."
                else {
                    val d = decision!!
                    val channels=state.channels.filter { it.categories.any(d.categoryIds::contains) }
                    val searchedQuery = query
                    searching=true
                    scope.launch {
                        try { val found=androidYouTubeClient(app,true,{QueryPolicy.evaluate(it,state.categories,state.blocked).categoryIds.any(d.categoryIds::contains)}).search(searchedQuery,channels,app.apiKey,state.blocked); if(query==searchedQuery) {results=found; feedback=if(results.isEmpty()) "No matching approved videos found." else "Videos found. Playback is unavailable while access protections are being tested."} }
                        catch(_: VideoApiException) { feedback="Videos are unavailable. Ask your parent to check setup." }
                        finally { searching=false }
                    }
                }
            }
        },enabled=!searching && !app.busy) { Text("Explore") }
        if(searching) CircularProgressIndicator()
        if(decision?.allowed==false) {
            Text("Let's ask your parent to help find something to learn.")
            Button(onClick={app.requestApproval(query)},enabled=!app.busy) { Text("Ask my parent") }
        }
        if(feedback.isNotEmpty()) Text(feedback)
        if(app.message.isNotEmpty()) Text(app.message)
        Text("Learning categories",style=MaterialTheme.typography.titleLarge)
        state.categories.filter{it.enabled}.chunked(3).forEach { row ->
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) { row.forEach { c -> OutlinedButton(onClick={query=c.terms.firstOrNull().orEmpty();decision=QueryPolicy.evaluate(query,state.categories,state.blocked);results=emptyList()}) { Text(c.name) } } }
        }
        if(state.categories.none{it.enabled}) Text("Your parent can choose learning categories in settings.")
        Text("Approved websites",style=MaterialTheme.typography.titleLarge)
        sites.filter { decision?.takeIf{it.allowed}?.let{d->it.categories.any(d.categoryIds::contains)} ?: true }.forEach { site ->
            OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(site.name,style=MaterialTheme.typography.titleMedium); Text(site.domain); Button(onClick={open(site)}) {Text("Visit website")} } }
        }
        if(sites.isEmpty()) Text("Your parent can approve websites for you to explore.")
        results.forEach { video -> OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(video.title);Text("Playback unavailable — parent review required") } } }
    }
}
