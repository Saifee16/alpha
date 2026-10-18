package family.safe

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import family.safe.policy.QueryDecision
import family.safe.search.EducationalResult
import family.safe.search.EducationalSearchClient
import family.safe.search.EducationalSource
import kotlinx.coroutines.*

/** Foreground-only results survive opening an article, but never persist as browsing history. */
internal class ChildSearchState {
    var query by mutableStateOf("")
    var decision by mutableStateOf<QueryDecision?>(null)
    var results by mutableStateOf<List<EducationalResult>>(emptyList())
    var feedback by mutableStateOf("")
    var searching by mutableStateOf(false)
    var generation = 0; private set
    var providerMillis by mutableStateOf<Map<EducationalSource, Long>>(emptyMap()); private set
    var firstResultMillis by mutableStateOf<Long?>(null); private set
    var elapsedMillis by mutableStateOf<Long?>(null); private set
    private var clients = emptyList<EducationalSearchClient>()
    private var job: Job? = null
    private var deadline: Job? = null
    private var startedNanos = 0L

    fun cancel(clear: Boolean = false) {
        generation++
        clients.forEach { it.cancel() }; clients = emptyList()
        job?.cancel(); job = null
        deadline?.cancel(); deadline = null
        if (searching) elapsedMillis = elapsed()
        searching = false
        if (clear) {
            query = ""; decision = null; results = emptyList(); feedback = ""
            providerMillis = emptyMap(); firstResultMillis = null; elapsedMillis = null
        }
    }

    fun start(
        scope: CoroutineScope,
        sources: List<EducationalSource>,
        phrase: String,
        blocked: Set<String>,
        eligible: () -> Boolean,
        clientFactory: (EducationalSource) -> EducationalSearchClient = { EducationalSearchClient() }
    ) {
        cancel()
        results = emptyList(); feedback = ""
        providerMillis = emptyMap(); firstResultMillis = null; elapsedMillis = null
        if (!eligible()) return
        val providers = sources.distinct().take(2).map { it to clientFactory(it) }
        if (providers.isEmpty()) return
        val currentGeneration = generation
        startedNanos = System.nanoTime()
        searching = true
        clients = providers.map { it.second }
        fun current() = generation == currentGeneration && eligible()
        var failures = 0
        job = scope.launch {
            try {
                supervisorScope {
                    providers.forEach { (source, client) ->
                        launch {
                            val providerStarted = System.nanoTime()
                            try {
                                val found = withContext(Dispatchers.IO) { client.search(source, phrase, blocked) }
                                if (current()) {
                                    results = (results + found).distinctBy { it.url }
                                    if (results.isNotEmpty() && firstResultMillis == null) firstResultMillis = elapsed()
                                    if (results.isNotEmpty()) feedback = "Choose a reading result below."
                                }
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                if (current()) failures++
                            } finally {
                                client.cancel()
                                if (current()) providerMillis = providerMillis + (source to ((System.nanoTime() - providerStarted) / 1_000_000))
                            }
                        }
                    }
                }
                if (current()) feedback = when {
                    results.isEmpty() && failures > 0 -> "Search is unavailable. Check your connection and try again."
                    results.isEmpty() -> "No matching resources in your approved sources. Try an approved subject."
                    failures > 0 -> "Some approved sources are unavailable. Showing results from the others."
                    else -> "Choose a reading result below."
                }
            } finally {
                if (generation == currentGeneration) {
                    deadline?.cancel(); deadline = null
                    clients.forEach { it.cancel() }; clients = emptyList()
                    elapsedMillis = elapsed()
                    searching = false
                }
            }
        }
        deadline = scope.launch {
            delay(12_000)
            if (generation == currentGeneration && searching) {
                val mayPublish = eligible()
                this@ChildSearchState.cancel()
                if (mayPublish) feedback = if (results.isEmpty()) "Search timed out. Try again."
                    else "Some approved sources timed out. Showing results from the others."
            }
        }
    }

    private fun elapsed(): Long = ((System.nanoTime() - startedNanos) / 1_000_000).coerceAtLeast(0)
}
