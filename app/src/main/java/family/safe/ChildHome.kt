package family.safe

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import family.safe.browser.*
import family.safe.data.FamilySettings
import family.safe.policy.*
import family.safe.search.*
import family.safe.video.nasa.*

@Composable internal fun HomeScreen(app: MainActivity, state: FamilySettings, sites: List<BrowserSource>, search: ChildSearchState, open: (BrowserSource,String?) -> Unit) {
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val inputStarted = remember { longArrayOf(0L) }
    var watching by remember { mutableStateOf(false) }
    var requested by remember(search.query) { mutableStateOf(false) }
    val library = approvedNasaVideos(state)
    val renderedDecision = search.decision
    DisposableEffect(search) { onDispose { search.cancel() } }
    BackHandler(enabled=search.decision != null || watching) { search.cancel(clear=true); watching=false }
    fun explore(query: String) {
        focus.clearFocus()
        search.cancel(clear=true)
        search.query = query
        val decision = QueryPolicy.evaluate(query,state.categories,state.blocked)
        search.decision = decision
        if (!decision.allowed || watching) return
        val providers = EducationalSource.entries.filter { provider -> sites.any {
            it.id == provider.id && it.domain == provider.domain && it.categories.any(decision.categoryIds::contains)
        } }
        if(providers.isEmpty()) { search.feedback="No results"; return }
        search.start(scope,providers,decision.searchPhrase ?: query,state.blocked,eligible = {
            app.settings == state && !app.parent && !app.gate && app.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        })
    }
    Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding().fillMaxSize().widthIn(max=1100.dp).padding(horizontal=24.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("FamilySafe",Modifier.weight(1f),style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.primary)
                TextButton(onClick={search.cancel(clear=true);app.gate=true},modifier=Modifier.heightIn(min=48.dp).testTag("parent-entry")) { Text("Parent settings") }
            }
            Text("Discover",style=MaterialTheme.typography.headlineLarge)
            Text("Big questions. New discoveries.",style=MaterialTheme.typography.bodyLarge)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
                OutlinedTextField(search.query,{
                    val started=System.nanoTime()
                    inputStarted[0]=started
                    search.cancel(clear=true); search.query=it.take(256)
                    app.performance["input_state_ms"]=(System.nanoTime()-started)/1_000_000
                },label={Text("What would you like to learn?")},singleLine=true,
                    keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),keyboardActions=KeyboardActions(onSearch={explore(search.query)}),
                    modifier=Modifier.weight(1f).testTag("discover-query").drawWithContent {
                        drawContent()
                        if(inputStarted[0]!=0L) {
                            app.performance["input_feedback_ms"]=(System.nanoTime()-inputStarted[0])/1_000_000
                            inputStarted[0]=0L
                        }
                    })
                Button(onClick={explore(search.query)},enabled=search.query.isNotBlank() && !app.busy,
                    modifier=Modifier.heightIn(min=56.dp).testTag("discover-search")) { Text("Search") }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                FilterChip(selected=!watching,onClick={watching=false;if(search.query.isNotBlank()) explore(search.query)},label={Text("Read")},modifier=Modifier.heightIn(min=48.dp).testTag("discover-read"))
                if(library.isNotEmpty()) FilterChip(selected=watching,onClick={search.cancel();watching=true},label={Text("Watch")},modifier=Modifier.heightIn(min=48.dp).testTag("discover-watch"))
            }
            if(search.searching) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text(if(search.results.isEmpty()) "Looking for something to read…" else "Finding more to read…") }
            LazyColumn(Modifier.fillMaxWidth().weight(1f).testTag("discover-content"),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=24.dp)) {
                if(renderedDecision?.allowed == false) item {
                    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text("Let’s try a different question.",style=MaterialTheme.typography.titleLarge)
                        if(renderedDecision?.outcome in setOf(QueryOutcome.DENY_UNAPPROVED,QueryOutcome.REQUEST_PARENT)) {
                            TextButton(onClick={app.requestApproval(search.query);requested=true},enabled=!requested && !app.busy) { Text(if(requested) "Request sent" else "Ask my parent") }
                        }
                    }
                }
                if(!watching && renderedDecision?.allowed == true && !search.searching && search.results.isEmpty()) item {
                    OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text("Nothing to read here yet",style=MaterialTheme.typography.titleLarge)
                        Text(if(search.feedback.contains("unavailable",true) || search.feedback.contains("timed out",true)) "We couldn’t finish the search. Check your connection and try again." else "Try another question, or try this search again.")
                        OutlinedButton(onClick={explore(search.query)},modifier=Modifier.heightIn(min=48.dp).testTag("discover-retry")) { Text("Try again") }
                    } }
                }
                if(!watching && renderedDecision == null) item {
                    OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text("Where will your curiosity take you?",style=MaterialTheme.typography.titleLarge)
                        Text("Type a question above to find something new to read.")
                    } }
                }
                if(!watching && renderedDecision?.allowed == true) items(search.results.filter { !QueryPolicy.containsBlocked(it.title,state.blocked) },key={it.url}) { result ->
                    val site=sites.find { it.id==result.source.id && it.domain==result.source.domain && it.categories.any(renderedDecision.categoryIds::contains) }
                    if(site != null && UrlPolicy.allows(result.url,site)) OutlinedCard(Modifier.fillMaxWidth().testTag("result-${result.source.id}-${result.title}")) {
                        Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            Text(result.title.removePrefix("Wikijunior:"),style=MaterialTheme.typography.titleLarge)
                            Text(result.source.displayName,style=MaterialTheme.typography.bodyMedium)
                            Button(onClick={
                                val current=app.settings ?: return@Button
                                val decision=QueryPolicy.evaluate(search.query,current.categories,current.blocked)
                                val allowed=current.sources.find { it.id==site.id && it.domain==site.domain && it.enabled && it.categories.any(decision.categoryIds::contains) }
                                if(decision.allowed && allowed!=null && !QueryPolicy.containsBlocked(result.title,current.blocked) && UrlPolicy.allows(result.url,allowed)) open(allowed,result.url)
                            },modifier=Modifier.heightIn(min=48.dp).testTag("read-${result.source.id}-${result.title}")) { Text("Read") }
                        }
                    }
                }
                if(!watching && renderedDecision?.allowed == true) items(sites.filter { site ->
                    EducationalSource.entries.none { it.id==site.id && it.domain==site.domain } &&
                        site.categories.any(renderedDecision.categoryIds::contains) && UrlPolicy.startUrl(site)!=null
                },key={"browse-${it.id}"}) { site ->
                    OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text(site.name,style=MaterialTheme.typography.titleLarge)
                        Text("Browse this website. Your question is not searched there.")
                        OutlinedButton(onClick={
                            val current=app.settings ?: return@OutlinedButton
                            val decision=QueryPolicy.evaluate(search.query,current.categories,current.blocked)
                            val allowed=current.sources.find { it.id==site.id && it.enabled && it.categories.any(decision.categoryIds::contains) }
                            if(decision.allowed && allowed!=null && UrlPolicy.startUrl(allowed)!=null) open(allowed,null)
                        },modifier=Modifier.heightIn(min=48.dp).testTag("browse-${site.id}")) { Text("Browse website") }
                    } }
                }
                if(watching) {
                    item { Text("Your video library",style=MaterialTheme.typography.titleLarge) }
                    val videos=approvedWatchVideos(state,search.query)
                    if(videos.isEmpty()) item { Text("No videos for this question yet. Try Read to look for an article.") }
                    items(videos,key={it.id}) { video -> OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            Text(video.title,style=MaterialTheme.typography.titleLarge)
                            Text(video.credit,style=MaterialTheme.typography.bodyMedium)
                            Button(onClick={app.openNasa(video.id)},modifier=Modifier.heightIn(min=48.dp).testTag("nasa-play-${video.id}")) { Text("Play video") }
                        }
                    } }
                }
            }
        }
    }
}
