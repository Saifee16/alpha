package family.safe

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import family.safe.policy.QueryDecision
import family.safe.search.EducationalResult
import family.safe.search.EducationalSearchClient
import kotlinx.coroutines.Job

/** Foreground-only results survive opening an article, but never persist as browsing history. */
internal class ChildSearchState {
    var query by mutableStateOf("")
    var decision by mutableStateOf<QueryDecision?>(null)
    var results by mutableStateOf<List<EducationalResult>>(emptyList())
    var feedback by mutableStateOf("")
    var searching by mutableStateOf(false)
    var generation = 0; private set
    var client: EducationalSearchClient? = null
    var job: Job? = null
    fun cancel(clear: Boolean = false) {
        generation++
        client?.cancel(); client = null
        job?.cancel(); job = null
        searching = false
        if (clear) { query = ""; decision = null; results = emptyList(); feedback = "" }
    }
}
