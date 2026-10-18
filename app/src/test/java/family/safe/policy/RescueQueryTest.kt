package family.safe.policy

import org.junit.Assert.*
import org.junit.Test

class RescueQueryTest {
    private val topics = listOf(Category("space", "Space", setOf("planets", "solar system"), true))
    private val blocked = setOf("gaming", "adult")

    @Test fun educationalQuestionsKeepTheWholeApprovedNiche() {
        for (query in listOf("what are planets?", "what is the solar system", "explain planets for beginners")) {
            assertTrue(query, QueryPolicy.evaluate(query, topics, blocked).allowed)
        }
    }

    @Test fun questionWordingCannotAuthorizeAdditionalSubjectsOrHideBlocks() {
        for (query in listOf("what are planets and cooking", "explain planets gaming", "what is adult solar system", "what are unknown topics")) {
            assertFalse(query, QueryPolicy.evaluate(query, topics, blocked).allowed)
        }
        assertFalse(QueryPolicy.evaluate("what are planets", topics.map { it.copy(enabled=false) }, blocked).allowed)
        assertEquals(QueryOutcome.DENY_BLOCKED, QueryPolicy.evaluate("explain planets gaming", topics, blocked).outcome)
    }
}
