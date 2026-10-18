package family.safe

import family.safe.search.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ChildSearchStateTest {
    private fun response(source: EducationalSource) = EducationalResponse(200,
        """{"query":{"search":[{"ns":${source.namespace},"pageid":1,"title":"${if (source == EducationalSource.VIKIDIA) "Animal" else "Wikijunior:Animal Kingdom"}"}]}}""")

    @Test fun firstProviderPublishesWhileOtherProviderIsStillRunning() = runBlocking {
        val release = CountDownLatch(1)
        val entered = CountDownLatch(1)
        val state = ChildSearchState()
        try {
            state.start(this, EducationalSource.entries, "animals", emptySet(), { true }) { source ->
                EducationalSearchClient {
                    if (source == EducationalSource.WIKIJUNIOR) {
                        entered.countDown()
                        check(release.await(2, TimeUnit.SECONDS))
                    }
                    response(source)
                }
            }
            withTimeout(1500) {
                while (state.results.isEmpty()) delay(5)
            }
            assertEquals("Animal", state.results.single().title)
            assertTrue(entered.await(1, TimeUnit.SECONDS))
            assertTrue(state.searching)
            assertNotNull(state.firstResultMillis)
            release.countDown()
            withTimeout(1500) { while (state.searching) delay(5) }
            assertEquals(2, state.results.size)
            assertEquals(2, state.providerMillis.size)
            assertTrue(state.elapsedMillis!! >= state.firstResultMillis!!)
        } finally { release.countDown(); state.cancel() }
    }

    @Test fun cancelledOrIneligibleLateResponseCannotPublish() = runBlocking {
        for (cancel in listOf(false, true)) {
            val release = CountDownLatch(1)
            val entered = CountDownLatch(1)
            val state = ChildSearchState()
            var eligible = true
            try {
                state.start(this, listOf(EducationalSource.VIKIDIA), "animals", emptySet(), { eligible }) { source ->
                    EducationalSearchClient {
                        entered.countDown()
                        check(release.await(2, TimeUnit.SECONDS))
                        response(source)
                    }
                }
                withContext(Dispatchers.IO) { assertTrue(entered.await(1, TimeUnit.SECONDS)) }
                if (cancel) state.cancel(clear = true) else eligible = false
                release.countDown()
                delay(100)
                assertTrue(state.results.isEmpty())
                assertFalse(state.searching)
                assertEquals("", state.feedback)
            } finally { release.countDown(); state.cancel() }
        }
    }
    @Test fun newSearchCannotBeOverwrittenByPreviousGeneration() = runBlocking {
        val release = CountDownLatch(1)
        val entered = CountDownLatch(1)
        val state = ChildSearchState()
        try {
            state.start(this, listOf(EducationalSource.VIKIDIA), "animals", emptySet(), { true }) { source ->
                EducationalSearchClient {
                    entered.countDown()
                    check(release.await(2, TimeUnit.SECONDS))
                    response(source)
                }
            }
            withContext(Dispatchers.IO) { assertTrue(entered.await(1, TimeUnit.SECONDS)) }
            state.start(this, listOf(EducationalSource.VIKIDIA), "science", emptySet(), { true }) {
                EducationalSearchClient { EducationalResponse(200,
                    """{"query":{"search":[{"ns":0,"pageid":2,"title":"Science"}]}}""") }
            }
            withTimeout(1500) { while (state.searching) delay(5) }
            release.countDown()
            delay(100)
            assertEquals("Science", state.results.single().title)
            assertEquals(1, state.providerMillis.size)
        } finally { release.countDown(); state.cancel() }
    }

    @Test fun wholeSearchDeadlineStopsWaitingForAnUnresponsiveTransport() = runBlocking {
        val release = CountDownLatch(1)
        val entered = CountDownLatch(1)
        val state = ChildSearchState()
        try {
            state.start(this, listOf(EducationalSource.VIKIDIA), "animals", emptySet(), { true }) { source ->
                EducationalSearchClient {
                    entered.countDown()
                    check(release.await(16, TimeUnit.SECONDS))
                    response(source)
                }
            }
            withContext(Dispatchers.IO) { assertTrue(entered.await(1, TimeUnit.SECONDS)) }
            withTimeout(14_000) { while (state.searching) delay(10) }
            assertTrue(state.results.isEmpty())
            assertTrue(state.feedback.contains("timed out"))
            assertTrue(state.elapsedMillis!! >= 11_500)
            release.countDown()
            delay(100)
            assertTrue(state.results.isEmpty())
        } finally { release.countDown(); state.cancel() }
    }

}
