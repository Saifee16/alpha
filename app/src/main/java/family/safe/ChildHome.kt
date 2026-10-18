package family.safe

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import family.safe.browser.*
import family.safe.data.FamilySettings
import family.safe.policy.*
import family.safe.search.*
import family.safe.video.nasa.*
import kotlinx.coroutines.*

@Composable internal fun HomeScreen(app: MainActivity, state: FamilySettings, sites: List<BrowserSource>, search: ChildSearchState, open: (BrowserSource,String?) -> Unit) {
    val scope = rememberCoroutineScope()
    DisposableEffect(search) { onDispose { search.cancel() } }
    fun explore(query: String) {
        search.cancel()
        search.query = query
        search.results = emptyList()
        val decision = QueryPolicy.evaluate(query,state.categories,state.blocked)
        search.decision = decision
        search.feedback = ""
        if (!decision.allowed) return
        val providers = EducationalSource.entries.filter { provider -> sites.any {
            it.id == provider.id && it.domain == provider.domain && it.categories.any(decision.categoryIds::contains) && UrlPolicy.startUrl(it) != null
        } }
        if(providers.isEmpty()) {
            search.feedback = if(approvedNasaVideos(state).none { it.category in decision.categoryIds }) "Video discovery is not set up. Explore an approved website below."
                else "Choose an approved video below."
            return
        }
        val generation = search.generation
        val client = EducationalSearchClient()
        search.client = client
        search.searching = true
        search.job = scope.launch {
            val found = mutableListOf<EducationalResult>()
            var failures = 0
            try {
                providers.take(2).forEach { provider ->
                    try { found += withContext(Dispatchers.IO) { client.search(provider,decision.searchPhrase ?: query,state.blocked) } }
                    catch(cancelled: CancellationException) { throw cancelled }
                    catch(_: Exception) { failures++ }
                }
                if(generation == search.generation && app.settings == state && !app.parent && !app.gate && app.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    search.results = found.distinctBy { it.url }
                    search.feedback = when {
                        found.isEmpty() && failures > 0 -> "Search is unavailable. Check your connection and try again."
                        found.isEmpty() -> "No matching resources in your approved sources. Try an approved subject."
                        failures > 0 -> "Some approved sources are unavailable. Showing results from the others."
                        else -> "Choose a reading result below."
                    }
                }
            } finally {
                client.cancel()
                if(generation == search.generation) { search.searching = false; search.client = null }
            }
        }
    }
    Page("FamilySafe") {
        Text("What would you like to explore?",style=MaterialTheme.typography.titleLarge)
        TextButton(onClick={search.cancel(clear=true);app.gate=true}) { Text("Parent settings") }
        OutlinedTextField(search.query,{ search.cancel(); search.query=it.take(256); search.decision=null; search.results=emptyList(); search.feedback="" },
            label={Text("What would you like to learn?")},singleLine=true,modifier=Modifier.fillMaxWidth())
        Button(onClick={explore(search.query)},enabled=!search.searching && !app.busy) { Text("Explore") }
        if(search.searching) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Searching your approved sources…") }
        if(search.decision?.allowed == false) {
            Text("Try one of your approved learning subjects.")
            if(search.decision?.outcome == QueryOutcome.DENY_UNAPPROVED || search.decision?.outcome == QueryOutcome.REQUEST_PARENT) {
                Button(onClick={app.requestApproval(search.query); search.decision=search.decision?.copy(outcome=QueryOutcome.REQUEST_PARENT)},enabled=!app.busy) { Text("Ask my parent") }
            }
            state.categories.filter { it.enabled }.flatMap { it.terms }.filter { QueryPolicy.evaluate(it,state.categories,state.blocked).allowed }.distinct().take(3).forEach { phrase ->
                TextButton(onClick={explore(phrase)}) { Text(phrase) }
            }
        }
        if(search.feedback.isNotEmpty()) Text(search.feedback)
        if(app.message.isNotEmpty()) Text(app.message)
        Text("Learning categories",style=MaterialTheme.typography.titleLarge)
        state.categories.filter { it.enabled }.chunked(3).forEach { row ->
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                row.forEach { category -> OutlinedButton(onClick={explore(category.terms.firstOrNull().orEmpty())}) { Text(category.name) } }
            }
        }
        if(state.categories.none { it.enabled }) Text("Your parent can choose learning categories in settings.")
        if(search.results.isNotEmpty() && search.decision?.allowed == true) Text("Reading results",style=MaterialTheme.typography.titleLarge)
        search.results.filter { !QueryPolicy.containsBlocked(it.title,state.blocked) && search.decision?.allowed == true }.forEach { result ->
            val site = sites.find { it.id == result.source.id && it.domain == result.source.domain && it.categories.any(search.decision!!.categoryIds::contains) }
            if(site != null && UrlPolicy.allows(result.url,site)) OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(result.title,style=MaterialTheme.typography.titleMedium)
                    Text(result.source.displayName)
                    Text(state.categories.filter { it.id in search.decision!!.categoryIds }.joinToString { it.name })
                    Button(onClick={open(site,result.url)},modifier=Modifier.testTag("read-${result.source.id}-${result.title}")) { Text("Read") }
                }
            }
        }
        val videos = approvedNasaVideos(state).filter { search.decision == null || (search.decision?.allowed == true && it.category in search.decision!!.categoryIds) }
        if(videos.isNotEmpty()) Text(if(videos.all { it.category == "space" }) "Approved Space videos" else "Approved learning videos",style=MaterialTheme.typography.titleLarge)
        videos.forEach { video -> OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(video.title,style=MaterialTheme.typography.titleMedium); Text(video.credit)
                Button(onClick={app.openNasa(video.id)},modifier=Modifier.testTag("nasa-play-${video.id}")) { Text("Play video") }
            }
        } }
        Text("Approved websites",style=MaterialTheme.typography.titleLarge)
        sites.filter { site -> search.decision?.let { it.allowed && site.categories.any(it.categoryIds::contains) } ?: true }.forEach { site ->
            OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                Text(site.name,style=MaterialTheme.typography.titleMedium); Text(site.domain)
                if(EducationalSource.entries.any { it.id == site.id && it.domain == site.domain }) Text("Use search above to find articles from this source.")
                else Button(onClick={open(site,null)}) { Text("Visit website") }
            } }
        }
        if(sites.isEmpty()) Text("Your parent can approve websites for you to explore.")
    }
}
