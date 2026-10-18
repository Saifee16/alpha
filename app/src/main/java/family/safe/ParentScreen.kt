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
import family.safe.settings.*
import family.safe.video.*
import family.safe.video.nasa.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalFocusManager
import kotlinx.coroutines.*
import java.util.UUID
import java.net.URL
import javax.net.ssl.HttpsURLConnection

@Composable fun ParentScreen(app: MainActivity, state: FamilySettings) {
    val tab = app.parentTab
    Page("Parent settings") {
        Button(onClick={app.lock()}) { Text("Lock and return home") }
        Text("Rules and requests stay on this tablet. Website approval trusts the permitted pages; it cannot certify every page's content.")
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("Topics","Words","Websites","Videos","Requests","Test rules","PIN").forEach { name ->
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
            "Test rules" -> RuleTestSettings(state)
            "PIN" -> SecuritySettings(app,state)
        }
    }
}

@Composable private fun TopicSettings(app:MainActivity,state:FamilySettings) {
    val focus = LocalFocusManager.current
    var name by remember{mutableStateOf("")}; var phrases by remember{mutableStateOf("")}
    Text("Approve whole topic or niche phrases. A saved phrase also permits a supported educational introduction and ending, such as 'how to … for kids'. Extra subjects and arbitrary words remain denied. Add synonyms or other languages as separate comma-separated phrases, then save.")
    state.categories.forEach { c -> key(c.id) {
        var terms by remember(c.terms){mutableStateOf(c.terms.joinToString(", "))}
        var phraseNote by remember(c.terms){mutableStateOf("")}
        var topicName by remember(c.name){mutableStateOf(c.name)}
        OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Row { Checkbox(c.enabled,{value->app.update{it.copy(categories=it.categories.map{old->if(old.id==c.id)old.copy(enabled=value) else old})}},enabled=!app.busy); Text(c.name) }
            Text(if(starterCategories().any { it.id == c.id }) "Built-in topic" else "Custom topic")
            OutlinedTextField(topicName,{topicName=it.take(80)},label={Text("Topic name")},modifier=Modifier.fillMaxWidth().testTag("topic-name-${c.id}"),singleLine=true)
            TextButton(onClick={focus.clearFocus();app.update{it.copy(categories=it.categories.map{old->if(old.id==c.id)old.copy(name=topicName.trim()) else old})}},enabled=!app.busy && topicName.isNotBlank() && topicName.none { it.isISOControl() },modifier=Modifier.testTag("save-topic-name-${c.id}")){Text("Save topic name")}
            OutlinedTextField(terms,{terms=it.take(4000);phraseNote=""},label={Text("Approved phrases and synonyms")},modifier=Modifier.fillMaxWidth().testTag("topic-phrases-${c.id}"))
            TextButton(onClick={
                try {
                    terms=suggestSearchPhrases(terms,state.blocked)
                    phraseNote="Review the suggested phrases, then choose Save phrases to approve them."
                } catch(e:IllegalArgumentException) { phraseNote=e.message.orEmpty() }
            },enabled=!app.busy,modifier=Modifier.testTag("suggest-phrases-${c.id}")){Text("Suggest search phrases")}
            if(phraseNote.isNotEmpty()) Text(phraseNote)
            Row { TextButton(onClick={ focus.clearFocus();val parsed=parseTerms(terms); if(parsed.isNotEmpty()) app.update{it.copy(categories=it.categories.map{old->if(old.id==c.id)old.copy(terms=parsed) else old})} },enabled=!app.busy,modifier=Modifier.testTag("save-phrases-${c.id}")){Text("Save phrases")}
                ConfirmRuleButton(app,"Remove topic","Remove ${c.name}? Its source associations and video approvals will be revoked. Candidate videos and unrelated rules stay on this tablet."){app.update{removeTopicRules(it,c.id)}} }
        } }
    } }
    OutlinedTextField(name,{name=it.take(80)},label={Text("New topic name")})
    OutlinedTextField(phrases,{phrases=it.take(4000)},label={Text("Approved phrases, separated by commas")},modifier=Modifier.fillMaxWidth())
    Button(onClick={ focus.clearFocus();val parsed=parseTerms(phrases); if(name.isNotBlank() && parsed.isNotEmpty()) app.update{it.copy(categories=it.categories+Category(UUID.randomUUID().toString(),name.trim(),parsed))} },enabled=!app.busy && state.categories.size<50){Text("Add topic (disabled initially)")}
    ConfirmRuleButton(app,"Reset topic rules","Restore the built-in topics, all disabled? This removes custom topics and phrases, disables websites, and revokes channel and video approvals. Your PIN, attempt delay and requests are kept."){app.update{resetRules(it,RuleGroup.TOPICS)}}
}
private fun parseTerms(text:String) = text.split(',').map{QueryPolicy.normalize(it)}.filter{it.isNotBlank() && it.length<=256}.take(100).toSet()

@Composable private fun WordSettings(app:MainActivity,state:FamilySettings) {
    var word by remember{mutableStateOf("")}
    Text("Blocked content",style=MaterialTheme.typography.titleLarge)
    Text("Blocked phrases override all topic approvals. Separator variations are checked too.")
    state.blocked.forEach { term -> Row { Text(term,Modifier.weight(1f));ConfirmRuleButton(app,"Remove","Remove the block on '$term'? Searches previously denied by this phrase may become allowed."){app.update{it.copy(blocked=it.blocked-term)}} } }
    OutlinedTextField(word,{word=it.take(100)},label={Text("New blocked word or phrase")})
    Button(onClick={ val clean=QueryPolicy.normalize(word); if(clean.isNotBlank())app.update{it.copy(blocked=it.blocked+clean)};word="" },enabled=!app.busy && state.blocked.size<100){Text("Block phrase")}
    ConfirmRuleButton(app,"Reset blocked content","Replace your blocked phrases with the built-in list? Custom blocks will be removed. Review the default list before returning the tablet to your child."){app.update{resetRules(it,RuleGroup.BLOCKED)}}
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
    val missingSearchSources = starterSources().filter { source -> source.id in setOf("vikidia","wikijunior") && state.sources.none { it.id==source.id } }
    if(missingSearchSources.isNotEmpty()) {
        Text("Supported search sources can be added disabled, then reviewed and enabled individually.")
        OutlinedButton(onClick={app.update{addSupportedSearchSources(it)}},enabled=!app.busy && state.sources.size+missingSearchSources.size<=30){Text("Add supported search sources")}
        if(state.sources.size+missingSearchSources.size>30) Text("Remove a source first; the website limit is 30.")
    }
    state.sources.forEach { s -> key(s.id) {
        var categories by remember(s.categories){mutableStateOf(s.categories)}
        var confirmCommunitySource by remember{mutableStateOf(false)}
        val communitySource = s.id in setOf("vikidia","wikijunior")
        OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
            Text(s.name,style=MaterialTheme.typography.titleMedium);Text("https://${s.domain}${s.pathPrefixes.firstOrNull().orEmpty()}")
            Text("External links: ${if(s.externalLinks) "present or possible; blocked" else "none declared"}. User contributions: ${if(s.userGenerated) "yes" else "none declared; review required"}.")
            if(communitySource) Text("Community edited; not every page is reviewed for children. Searches are sent to this provider. Review and approve the article scope before enabling.")
            if(s.id=="natgeo") Text("Unavailable: safe rendering has not been verified. Keep this source disabled.")
            Row { Checkbox(s.enabled,{enabled->
                if(enabled && communitySource) confirmCommunitySource=true
                else app.update{it.copy(sources=it.sources.map{old->if(old.id==s.id)old.copy(enabled=enabled) else old})}
            },enabled=!app.busy && (s.id!="natgeo" || s.enabled),modifier=Modifier.testTag("source-enable-${s.id}"));Text("I approve this source for the selected topics") }
            if(confirmCommunitySource) AlertDialog(onDismissRequest={confirmCommunitySource=false},title={Text("Enable ${s.name}?")},text={Text("This community-edited source can contain unsuitable or inaccurate pages. Your child's searches will be sent to it. Only the approved article scope is permitted; review it together before enabling.")},
                confirmButton={TextButton(onClick={confirmCommunitySource=false;app.update{it.copy(sources=it.sources.map{old->if(old.id==s.id)old.copy(enabled=true) else old})}},enabled=!app.busy){Text("Approve and enable")}},
                dismissButton={TextButton(onClick={confirmCommunitySource=false}){Text("Cancel")}})
            CategoryChoices(state.categories,categories){categories=it}
            Row { TextButton(onClick={app.update{it.copy(sources=it.sources.map{old->if(old.id==s.id)old.copy(categories=categories) else old})}},enabled=!app.busy){Text("Save topics")}
                ConfirmRuleButton(app,"Remove source","Remove ${s.name}? Its website approval will be deleted. Other topics and sources are kept."){app.update{removeSourceRules(it,s.id)}} }
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
    ConfirmRuleButton(app,"Reset website rules","Replace website rules with the built-in sources, all disabled? Custom sources and their approvals will be removed. Topic rules, PIN and requests are kept.",enabled=!checking){app.update{resetRules(it,RuleGroup.WEBSITES)}}
}

@Composable private fun VideoSettings(app:MainActivity,state:FamilySettings) {
    Text("Optional experimental NASA playback; not general video search. Individual review remains required; collection mode is unavailable because no verified collection meets the policy.")
    NasaReviewSettings(app,state)
    Text("YouTube playback is unavailable because the official player can show related videos. No YouTube or Google fallback is available.")
    if(!BuildConfig.YOUTUBE_API_ENABLED) {
        Text("Channel discovery is unavailable in this build. API key entry and channel approval controls are hidden until child-directed API setup is complete.")
        return
    }
    var name by remember{mutableStateOf("")};var selected by remember{mutableStateOf(emptySet<String>())}
    var candidates by remember{mutableStateOf<List<ChannelCandidate>>(emptyList())};var checking by remember{mutableStateOf(false)};var note by remember{mutableStateOf("")}
    val scope=rememberCoroutineScope()
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
    val categories = state.categories.filter { it.enabled }
    var category by remember { mutableStateOf(categories.firstOrNull()?.id.orEmpty()) }
    var query by remember(category) { mutableStateOf(state.categories.find { it.id == category }?.terms?.sorted()?.firstOrNull().orEmpty()) }
    Text("NASA parent discovery",style=MaterialTheme.typography.titleLarge)
    Text("Choose an enabled topic and one of its approved whole phrases. Search checks up to five NASA/JPL videos and their mobile assets. No URL entry is needed. Results are saved only for parent review; nothing is approved automatically.")
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        categories.forEach { topic -> FilterChip(selected=category==topic.id,onClick={category=topic.id},enabled=!app.busy,label={Text(topic.name)},modifier=Modifier.testTag("nasa-category-${topic.id}")) }
    }
    if(categories.isEmpty()) Text("Enable a topic in Topics before searching NASA.")
    val chosen = categories.find { it.id == category }
    chosen?.let { Text("Approved phrases: ${it.terms.sorted().joinToString(", ")}") }
    OutlinedTextField(query,{query=it.take(200)},enabled=!app.busy,label={Text("NASA search phrase")},singleLine=true,modifier=Modifier.testTag("nasa-search-query"))
    Button(onClick={app.discoverNasa(category,query)},enabled=!app.busy && state.nasaVideos.size<MAX_NASA_VIDEOS && canDiscoverNasa(state,app.parent,category,query),modifier=Modifier.testTag("nasa-discover")) { Text("Find NASA candidates") }
    Text("Parent review collection (${state.nasaVideos.size}/$MAX_NASA_VIDEOS)",style=MaterialTheme.typography.titleLarge)
    Text("Preview the full video and review its credits and usage rights before approving. NASA source or category alone does not establish suitability. Only approved, eligible entries appear in child mode.")
    if(state.categories.none { it.id == "space" }) {
        OutlinedButton(onClick={app.update { it.copy(categories=it.categories+starterCategories().single { topic -> topic.id == "space" }) }},enabled=!app.busy && state.categories.size<50) { Text("Restore Space topic (disabled)") }
    }
    state.nasaVideos.forEach { video -> key(video.id) {
        val enabled = state.categories.any { it.id == video.category && it.enabled } && !nasaMetadataBlocked(video,state)
        val review = state.nasaReviews.find { it.id == video.id }
        var confirmed by remember(review) { mutableStateOf(false) }
        OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(video.title,style=MaterialTheme.typography.titleMedium)
            Text(video.description)
            Text("${video.source} · ${video.id}")
            Text(video.credit)
            Text(video.rightsNote)
            Text("Topic: ${state.categories.find { it.id == video.category }?.name ?: "Removed topic"}")
            if(!enabled) Text("Enable this topic and remove any metadata block before preview or approval.")
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
            TextButton(onClick={app.removeNasa(video.id)},enabled=!app.busy,modifier=Modifier.testTag("nasa-remove-${video.id}")) { Text("Remove candidate and decision") }
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
        ConfirmRuleButton(app,"Delete request","Delete this request from the tablet? Topic and source approvals will be kept."){app.update{it.copy(requests=it.requests.filterNot{old->old.id==r.id})}}
    }}}
    if(state.requests.isEmpty()) Text("No requests yet.")
    if(state.requests.any { it.status!="pending" }) ConfirmRuleButton(app,"Clear completed requests","Delete approved and rejected requests? Pending requests and existing approvals will be kept."){app.update{it.copy(requests=it.requests.filter { request->request.status=="pending" })}}
}

@Composable private fun ConfirmRuleButton(app:MainActivity,label:String,explanation:String,enabled:Boolean=true,onConfirm:()->Unit) {
    var confirming by remember{mutableStateOf(false)}
    TextButton(onClick={confirming=true},enabled=!app.busy && enabled){Text(label)}
    if(confirming) AlertDialog(onDismissRequest={confirming=false},title={Text(label)},text={Text(explanation)},
        confirmButton={TextButton(onClick={confirming=false;onConfirm()},enabled=!app.busy && enabled){Text("Confirm")}},
        dismissButton={TextButton(onClick={confirming=false}){Text("Cancel")}})
}

@Composable private fun RuleTestSettings(state:FamilySettings) {
    val focus = LocalFocusManager.current
    var query by remember{mutableStateOf("")}
    var decision by remember(state){mutableStateOf<QueryDecision?>(null)}
    Text("Test search restrictions",style=MaterialTheme.typography.titleLarge)
    Text("Try a complete search using your saved rules. This check stays on this tablet, sends no search and changes no approvals.")
    OutlinedTextField(query,{query=it.take(256);decision=null},label={Text("Sample search")},singleLine=true,modifier=Modifier.fillMaxWidth().testTag("rule-test-query"))
    Button(onClick={focus.clearFocus();decision=QueryPolicy.evaluate(query,state.categories,state.blocked)},modifier=Modifier.testTag("rule-test-run")){Text("Test search restrictions")}
    decision?.let { result ->
        Text("Outcome: ${when(result.outcome) {
            QueryOutcome.ALLOW -> "Allowed by saved topic rules"
            QueryOutcome.DENY_BLOCKED -> "Blocked content"
            QueryOutcome.DENY_UNAPPROVED -> "Unapproved search"
            QueryOutcome.REQUEST_PARENT -> "Parent approval needed"
            QueryOutcome.INVALID -> "Invalid search"
        }}")
        Text("Applied rule: ${result.reason}")
        result.searchPhrase?.let { Text("Approved topic phrase: $it") }
        val categories = state.categories.filter { it.id in result.categoryIds }
        Text("Categories: ${categories.joinToString { it.name }.ifEmpty { "None" }}")
        val sources = if(result.allowed) state.sources.filter { source ->
            source.enabled && source.categories.any(result.categoryIds::contains) && UrlPolicy.startUrl(source)!=null
        } else emptyList()
        Text("Approved sources: ${sources.joinToString { it.name }.ifEmpty { "None" }}")
        if(result.allowed && sources.isEmpty()) Text("The phrase is allowed, but no enabled approved website serves these topics yet.")
        Text("An allowed search does not certify a provider's results or approve video playback.")
    }
}

@Composable private fun SecuritySettings(app:MainActivity,state:FamilySettings) {
    Text("Security and protection status",style=MaterialTheme.typography.titleLarge)
    Text("Parent rules are PIN protected. Leaving the app locks parent access. Unknown searches and destinations are denied inside FamilySafe.")
    Text("${state.categories.count { it.enabled }} enabled topics · ${state.sources.count { it.enabled }} enabled websites")
    Text("Scripts, forms, sign-in, popups, external apps and unapproved destinations are blocked in the browser. YouTube playback is unavailable.")
    Text("FamilySafe protects its own browsing. Configure Android and your family device controls separately to restrict other apps and device settings.")
    PinSettings(app)
    ConfirmRuleButton(app,"Restore recommended safe defaults","Replace topic, blocked-content and website rules with the built-in defaults? All topics and websites will be disabled. Custom topics and sources will be removed, and channel and video approvals revoked. Your PIN, attempt delay, requests and candidate videos are kept."){app.update{resetRules(it,RuleGroup.ALL)}}
}

@Composable private fun PinSettings(app:MainActivity) {
    var pin by remember{mutableStateOf("")};var confirm by remember{mutableStateOf("")};var note by remember{mutableStateOf("")};var hashing by remember{mutableStateOf(false)}
    val scope=rememberCoroutineScope()
    OutlinedTextField(pin,{pin=it.take(32)},label={Text("New PIN")},visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword,autoCorrectEnabled=false),singleLine=true)
    OutlinedTextField(confirm,{confirm=it.take(32)},label={Text("Confirm new PIN")},visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword,autoCorrectEnabled=false),singleLine=true)
    Button(onClick={if(pin==confirm && pin.matches(Regex("[0-9]{6,32}"))){val chosen=pin;pin="";confirm="";hashing=true;scope.launch{val hash=withContext(Dispatchers.Default){PinSecurity.create(chosen)};hashing=false;if(app.parent)app.update{it.copy(pinHash=hash)}}}else note="Enter the same PIN twice using 6 to 32 digits."},enabled=!hashing && !app.busy){Text("Change PIN")}
    Text(note)
}
