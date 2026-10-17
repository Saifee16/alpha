package family.safe

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import family.safe.browser.*
import family.safe.data.*
import family.safe.policy.*
import family.safe.settings.PinSecurity
import family.safe.video.*
import family.safe.video.nasa.*
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.*
import java.util.UUID
import java.net.URL
import javax.net.ssl.HttpsURLConnection

@Composable fun ParentScreen(app: MainActivity, state: FamilySettings) {
    val tab = app.parentTab
    Page("Parent settings") {
        Button(onClick={app.lock()}) { Text("Lock and return home") }
        Text("Rules and requests stay on this tablet. Website approval trusts the permitted pages; it cannot certify every page's content.")
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("Topics","Words","Websites","Videos","Requests","PIN").forEach { name ->
            FilterChip(selected=tab==name,onClick={app.parentTab=name},label={Text(name)})
        } }
        if(app.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(app.message)
        when(tab) {
            "Topics" -> TopicSettings(app,state)
            "Words" -> WordSettings(app,state)
            "Websites" -> WebsiteSettings(app,state)
            "Videos" -> VideoSettings(app,state)
            "Requests" -> RequestSettings(app,state)
            "PIN" -> PinSettings(app)
        }
    }
}

@Composable private fun TopicSettings(app:MainActivity,state:FamilySettings) {
    var name by remember{mutableStateOf("")}; var phrases by remember{mutableStateOf("")}
    Text("Approve whole search phrases. Add synonyms or other languages as separate phrases, separated by commas. A topic word does not authorize arbitrary extra words.")
    state.categories.forEach { c -> key(c.id) {
        var terms by remember(c.terms){mutableStateOf(c.terms.joinToString(", "))}
        OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Row { Checkbox(c.enabled,{value->app.update{it.copy(categories=it.categories.map{old->if(old.id==c.id)old.copy(enabled=value) else old})}},enabled=!app.busy); Text(c.name) }
            OutlinedTextField(terms,{terms=it.take(4000)},label={Text("Approved phrases and synonyms")},modifier=Modifier.fillMaxWidth())
            Row { TextButton(onClick={ val parsed=parseTerms(terms); if(parsed.isNotEmpty()) app.update{it.copy(categories=it.categories.map{old->if(old.id==c.id)old.copy(terms=parsed) else old})} },enabled=!app.busy){Text("Save phrases")}
                TextButton(onClick={app.update{it.copy(categories=it.categories.filterNot{old->old.id==c.id})}},enabled=!app.busy){Text("Remove topic")} }
        } }
    } }
    OutlinedTextField(name,{name=it.take(80)},label={Text("New topic name")})
    OutlinedTextField(phrases,{phrases=it.take(4000)},label={Text("Approved phrases, separated by commas")},modifier=Modifier.fillMaxWidth())
    Button(onClick={ val parsed=parseTerms(phrases); if(name.isNotBlank() && parsed.isNotEmpty()) app.update{it.copy(categories=it.categories+Category(UUID.randomUUID().toString(),name.trim(),parsed))} },enabled=!app.busy && state.categories.size<50){Text("Add topic (disabled initially)")}
}
private fun parseTerms(text:String) = text.split(',').map{QueryPolicy.normalize(it)}.filter{it.isNotBlank() && it.length<=256}.take(100).toSet()

@Composable private fun WordSettings(app:MainActivity,state:FamilySettings) {
    var word by remember{mutableStateOf("")}
    Text("Blocked phrases override all topic approvals. Separator variations are checked too.")
    state.blocked.forEach { term -> Row { Text(term,Modifier.weight(1f));TextButton(onClick={app.update{it.copy(blocked=it.blocked-term)}},enabled=!app.busy){Text("Remove")} } }
    OutlinedTextField(word,{word=it.take(100)},label={Text("New blocked word or phrase")})
    Button(onClick={ val clean=QueryPolicy.normalize(word); if(clean.isNotBlank())app.update{it.copy(blocked=it.blocked+clean)};word="" },enabled=!app.busy && state.blocked.size<100){Text("Block phrase")}
}

@Composable private fun CategoryChoices(categories:List<Category>, selected:Set<String>,change:(Set<String>)->Unit) {
    categories.forEach { c -> Row { Checkbox(c.id in selected,{ if(it) change(selected+c.id) else change(selected-c.id) });Text(c.name) } }
}

@Composable private fun WebsiteSettings(app:MainActivity,state:FamilySettings) {
    var name by remember{mutableStateOf("")};var domain by remember{mutableStateOf("")};var path by remember{mutableStateOf("/")}
    var selected by remember{mutableStateOf(emptySet<String>())};var confirmed by remember{mutableStateOf(false)}
    var checking by remember{mutableStateOf(false)};var note by remember{mutableStateOf("")}
    val scope=rememberCoroutineScope()
    Text("Review the exact domain and categories before enabling a source. The browser is read-only: scripts, forms, sign-in, popups, media and external resources are blocked. Check rendering together before handing over the tablet.")
    state.sources.forEach { s -> key(s.id) {
        var categories by remember(s.categories){mutableStateOf(s.categories)}
        OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
            Text(s.name,style=MaterialTheme.typography.titleMedium);Text("https://${s.domain}${s.pathPrefixes.firstOrNull().orEmpty()}")
            Text("External links: ${if(s.externalLinks) "present or possible; blocked" else "none declared"}. User contributions: ${if(s.userGenerated) "yes" else "none declared; review required"}.")
            Row { Checkbox(s.enabled,{enabled-> app.update{it.copy(sources=it.sources.map{old->if(old.id==s.id)old.copy(enabled=enabled) else old})} },enabled=!app.busy);Text("I approve this source for the selected topics") }
            CategoryChoices(state.categories,categories){categories=it}
            Row { TextButton(onClick={app.update{it.copy(sources=it.sources.map{old->if(old.id==s.id)old.copy(categories=categories) else old})}},enabled=!app.busy){Text("Save topics")}
                TextButton(onClick={app.update{it.copy(sources=it.sources.filterNot{old->old.id==s.id})}},enabled=!app.busy){Text("Remove source")} }
        } }
    } }
    Text("Add a custom website",style=MaterialTheme.typography.titleLarge)
    OutlinedTextField(name,{name=it.take(80)},label={Text("Website name")})
    OutlinedTextField(domain,{domain=it.take(253);confirmed=false},label={Text("Exact domain, for example spaceplace.nasa.gov")},modifier=Modifier.fillMaxWidth(),singleLine=true)
    OutlinedTextField(path,{path=it.take(200);confirmed=false},label={Text("Allowed path, for example /learning")},singleLine=true)
    CategoryChoices(state.categories,selected){selected=it}
    Row { Checkbox(confirmed,{confirmed=it});Text("I checked this domain belongs to the intended source and reviewed its content.") }
    Button(onClick={
        val candidate=BrowserSource(UUID.randomUUID().toString(),name.trim(),domain.trim().lowercase(),selected,listOf(path),enabled=true)
        val url=UrlPolicy.startUrl(candidate)
        if(url==null || !confirmed || name.isBlank()) note="Check the domain, path and topic selections."
        else { checking=true;scope.launch {
            val reachable=withContext(Dispatchers.IO) { runCatching { val conn=URL(url).openConnection() as HttpsURLConnection;try { conn.instanceFollowRedirects=false;conn.requestMethod="HEAD";conn.connectTimeout=8000;conn.readTimeout=8000;conn.responseCode in 200..299 } finally {conn.disconnect()} }.getOrDefault(false) }
            checking=false
            if(reachable && app.parent) {app.update{it.copy(sources=it.sources+candidate.copy(enabled=false))};note="Domain reached over HTTPS. Enable the source after reviewing its settings."}
            else note="Could not verify a direct HTTPS response. The source was not added."
        } }
    },enabled=!app.busy && !checking && confirmed && selected.isNotEmpty() && state.sources.size<30){Text(if(checking) "Checking website…" else "Check domain and add")}
    Text(note)
}

@Composable private fun VideoSettings(app:MainActivity,state:FamilySettings) {
    NasaReviewSettings(app,state)
    var name by remember{mutableStateOf("")};var selected by remember{mutableStateOf(emptySet<String>())}
    var candidates by remember{mutableStateOf<List<ChannelCandidate>>(emptyList())};var checking by remember{mutableStateOf(false)};var note by remember{mutableStateOf("")}
    val scope=rememberCoroutineScope()
    Text("YouTube playback is blocked: the official player can show related videos. Discovery also remains disabled in this build until child-directed API setup is completed. No YouTube or Google fallback is available.")
    OutlinedTextField(app.apiKey,{app.apiKey=it.take(256)},label={Text("API key (memory only)")},visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password,autoCorrectEnabled=false),singleLine=true)
    OutlinedTextField(name,{name=it.take(100)},label={Text("Find a channel by name")})
    CategoryChoices(state.categories,selected){selected=it}
    Button(onClick={checking=true;scope.launch {try{candidates=app.resolveChannels(name);note=if(candidates.isEmpty())"No matching channels." else "Compare the actual channel IDs and descriptions before approving."}catch(e:VideoApiException){note=when(e.reason){VideoError.DISABLED->"Discovery is disabled pending child-directed API setup.";VideoError.MISSING_KEY->"Add an API key first.";VideoError.QUOTA->"API quota exhausted; no fallback will be used.";else->"Channel lookup unavailable. Check credentials and connection."}}finally{checking=false}}},enabled=!checking && !app.busy){Text("Find verified channel identities")}
    Text(note)
    candidates.forEach { c -> OutlinedCard(Modifier.fillMaxWidth()) {Column(Modifier.padding(16.dp)) {
        Text(c.name);Text(c.url);Text(c.description.take(500))
        Button(onClick={app.update{it.copy(channels=(it.channels.filterNot{old->old.id==c.id}+TrustedChannel(c.id,c.name,selected)).take(30))};candidates=emptyList()},enabled=selected.isNotEmpty() && !app.busy){Text("Approve this exact channel")}
    }} }
    state.channels.forEach{ c->Row {Text("${c.name} · ${c.id}",Modifier.weight(1f));TextButton(onClick={app.update{it.copy(channels=it.channels.filterNot{old->old.id==c.id})}},enabled=!app.busy){Text("Remove")}}}
}

@Composable private fun NasaReviewSettings(app:MainActivity,state:FamilySettings) {
    val enabled = state.categories.any { it.id == "space" && it.enabled }
    Text("NASA Space collection",style=MaterialTheme.typography.titleLarge)
    Text("These candidates are not approved for your child. Preview the full video and review the credits and usage rights before approving. NASA source or category alone does not establish suitability. No thumbnails or unreviewed entries appear in child mode.")
    if(!enabled) Text("Enable the Space topic before reviewing this collection.")
    if(state.categories.none { it.id == "space" }) {
        OutlinedButton(onClick={app.update { it.copy(categories=it.categories+starterCategories().single { topic -> topic.id == "space" }) }},enabled=!app.busy && state.categories.size<50) { Text("Restore Space topic (disabled)") }
    }
    NasaCatalog.videos.forEach { video -> key(video.id) {
        val review = state.nasaReviews.find { it.id == video.id }
        var confirmed by remember(review) { mutableStateOf(false) }
        OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(video.title,style=MaterialTheme.typography.titleMedium)
            Text(video.description)
            Text("${video.source} · ${video.id}")
            Text(video.credit)
            Text(video.rightsNote)
            Text(when(review?.approved) { true -> "Approved"; false -> "Rejected"; null -> "Not reviewed" })
            OutlinedButton(onClick={app.openNasa(video.id,preview=true)},enabled=enabled && !app.busy,modifier=Modifier.testTag("nasa-preview-${video.id}")) { Text("Preview in parent mode") }
            Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(confirmed,{confirmed=it},enabled=enabled && !app.busy,modifier=Modifier.testTag("nasa-confirm-${video.id}"))
                Text("I previewed this video and reviewed its content, credits and usage rights.",Modifier.weight(1f))
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Button(onClick={app.reviewNasa(video.id,true,confirmed)},enabled=enabled && confirmed && app.nasaPreviewHashes.containsKey(video.id) && !app.busy,modifier=Modifier.testTag("nasa-approve-${video.id}")) { Text("Approve video") }
                OutlinedButton(onClick={app.reviewNasa(video.id,false)},enabled=!app.busy,modifier=Modifier.testTag("nasa-reject-${video.id}")) { Text("Reject video") }
            }
            if(!app.nasaPreviewHashes.containsKey(video.id)) Text("Preview successfully in this parent session before approving. Changed video files require a new review.")
        } }
    } }
}

@Composable private fun RequestSettings(app:MainActivity,state:FamilySettings) {
    var category by remember{mutableStateOf(state.categories.firstOrNull()?.id.orEmpty())}
    Text("Requests never approve themselves. Approving a subject adds its whole phrase to the selected topic. For a source request, verify and add the website or channel in its own settings first.")
    state.categories.forEach{c->Row{RadioButton(category==c.id,{category=c.id});Text(c.name)}}
    state.requests.forEach{r->OutlinedCard(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){
        Text(r.text);Text(r.status)
        if(r.status=="pending") Row {
            TextButton(onClick={app.update{it.copy(categories=it.categories.map{c->if(c.id==category)c.copy(terms=c.terms+QueryPolicy.normalize(r.text)) else c},requests=it.requests.map{old->if(old.id==r.id)old.copy(status="approved subject") else old})}},enabled=!app.busy && state.categories.any{it.id==category} && !QueryPolicy.containsBlocked(r.text,state.blocked)){Text("Approve subject")}
            TextButton(onClick={app.update{it.copy(requests=it.requests.map{old->if(old.id==r.id)old.copy(status="rejected") else old})}},enabled=!app.busy){Text("Reject")}
        }
        TextButton(onClick={app.update{it.copy(requests=it.requests.filterNot{old->old.id==r.id})}},enabled=!app.busy){Text("Delete request")}
    }}}
    if(state.requests.isEmpty()) Text("No requests yet.")
}

@Composable private fun PinSettings(app:MainActivity) {
    var pin by remember{mutableStateOf("")};var confirm by remember{mutableStateOf("")};var note by remember{mutableStateOf("")};var hashing by remember{mutableStateOf(false)}
    val scope=rememberCoroutineScope()
    OutlinedTextField(pin,{pin=it.take(32)},label={Text("New PIN")},visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword,autoCorrectEnabled=false),singleLine=true)
    OutlinedTextField(confirm,{confirm=it.take(32)},label={Text("Confirm new PIN")},visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword,autoCorrectEnabled=false),singleLine=true)
    Button(onClick={if(pin==confirm && pin.matches(Regex("[0-9]{6,32}"))){val chosen=pin;pin="";confirm="";hashing=true;scope.launch{val hash=withContext(Dispatchers.Default){PinSecurity.create(chosen)};hashing=false;if(app.parent)app.update{it.copy(pinHash=hash)}}}else note="Enter the same PIN twice using 6 to 32 digits."},enabled=!hashing && !app.busy){Text("Change PIN")}
    Text(note)
}
