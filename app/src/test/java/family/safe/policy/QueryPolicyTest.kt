package family.safe.policy

import org.junit.Assert.*
import org.junit.Test

class QueryPolicyTest {
    @Test fun boundedEducationalWordingRetainsWholeNicheAndTypedDecision() {
        val topics = listOf(Category("football", "Football", setOf("football passing drills"), true))
        listOf("football passing drills for beginners", "how to football passing drills", "football passing drills explained",
            "football passing drills tutorial", "football passing drills lessons", "learn about football passing drills for kids").forEach {
            val decision = QueryPolicy.evaluate(it, topics, emptySet())
            assertEquals(it, QueryOutcome.ALLOW, decision.outcome)
            assertEquals("football passing drills", decision.searchPhrase)
            assertEquals(setOf("football"), decision.categoryIds)
        }
        listOf("football passing drills and gaming", "football violent gaming", "football passing drills for adults",
            "football passing drills lessons explained", "how to football passing drills and fractions").forEach {
            assertEquals(it, QueryOutcome.DENY_UNAPPROVED, QueryPolicy.evaluate(it, topics, emptySet()).outcome)
        }
        assertEquals(QueryOutcome.DENY_BLOCKED, QueryPolicy.evaluate("football passing drills for kids", topics, setOf("kids")).outcome)
        assertEquals(QueryOutcome.INVALID, QueryPolicy.evaluate("football\u200b passing drills", topics, emptySet()).outcome)
        assertEquals(QueryOutcome.DENY_UNAPPROVED, QueryPolicy.evaluate("football passing drills tutorial", topics.map { it.copy(enabled=false) }, emptySet()).outcome)
    }

    private val science = Category("science", "Science", setOf("solar system", "planets", "سائنس"), true)
    private val categories = listOf(science, Category("math", "Math", setOf("addition")))
    private val blocked = setOf("adult content", "porn", "violence")

    @Test fun explicitWholePhrasesAndSynonymsAreAllowed() {
        listOf("solar system", "planets", "سائنس").forEach {
            val decision = QueryPolicy.evaluate(it, categories, blocked)
            assertTrue(it, decision.allowed)
            assertEquals(setOf("science"), decision.categoryIds)
            assertTrue(decision.reason.isNotBlank())
        }
    }

    @Test fun capitalizationPunctuationAndUnicodeSpacesNormalize() {
        assertEquals("solar system", QueryPolicy.normalize("  ＳＯＬＡＲ\u00a0— SYSTEM!!  "))
        assertTrue(QueryPolicy.evaluate("  ＳＯＬＡＲ\u00a0— SYSTEM!!  ", categories, blocked).allowed)
    }

    @Test fun unknownOrExtendedPhrasesNeverPassBecauseOfOneKeyword() {
        listOf("football", "solar", "solar system football", "solar system porn", "what is the solar system", "planet", "сolar system").forEach {
            assertFalse(it, QueryPolicy.evaluate(it, categories, blocked).allowed)
        }
    }

    @Test fun disabledCategoriesDoNotAuthorizeSearches() {
        assertFalse(QueryPolicy.evaluate("addition", categories, blocked).allowed)
        assertFalse(QueryPolicy.evaluate("solar system", emptyList(), blocked).allowed)
    }

    @Test fun blockedTermsWinEvenOverAnExplicitParentPhrase() {
        val unsafe = listOf(Category("custom", "Custom", setOf("solar system porn", "adultcontent", "p.o.r.n"), true))
        unsafe.single().terms.forEach { assertFalse(it, QueryPolicy.evaluate(it, unsafe, blocked).allowed) }
    }

    @Test fun blockedDetectionHandlesSeparatorsAndInvisibleMarks() {
        listOf("PORN", "p.o.r.n", "p o r n", "p\u200bo\u200crn", "adult-content", "adultcontent", "adult . con tent", "p\u202eorn").forEach {
            assertTrue(it, QueryPolicy.containsBlocked(it, blocked))
        }
    }

    @Test fun blockedDetectionRespectsWordBoundaries() {
        listOf("classic", "assignment", "compass").forEach { assertFalse(it, QueryPolicy.containsBlocked(it, setOf("ass"))) }
        assertTrue(QueryPolicy.containsBlocked("an ass!", setOf("ass")))
        assertFalse(QueryPolicy.containsBlocked("planets", setOf("", "  ")))
    }

    @Test fun malformedEmptyAndExcessiveQueriesFailClosed() {
        listOf("", "    ", "!!!", "solar\u0000system", "solar\nsystem", "solar\u200bsystem", "solar\u202esystem", "a".repeat(257)).forEach {
            val custom = listOf(Category("custom", "Custom", setOf(it), true))
            assertFalse(it, QueryPolicy.evaluate(it, custom, emptySet()).allowed)
        }
    }

    @Test fun allMatchingEnabledCategoryIdsAreReturned() {
        val duplicate = Category("astronomy", "Astronomy", setOf("solar system"), true)
        assertEquals(setOf("science", "astronomy"), QueryPolicy.evaluate("solar system", categories + duplicate, blocked).categoryIds)
    }

    @Test fun significantUnicodeMarksNeverCollapseDifferentWords() {
        assertEquals("कला", QueryPolicy.normalize("कला"))
        val art = listOf(Category("art", "Art", setOf("कला"), true))
        assertTrue(QueryPolicy.evaluate("कला", art, emptySet()).allowed)
        assertFalse(QueryPolicy.evaluate("कल", art, emptySet()).allowed)
    }

    @Test fun canonicalCombiningFormsRemainEquivalent() {
        val cafe = listOf(Category("cafe", "Cafe", setOf("café"), true))
        assertTrue(QueryPolicy.evaluate("cafe\u0301", cafe, emptySet()).allowed)
        assertEquals("café", QueryPolicy.normalize("cafe\u0301"))
        assertTrue(QueryPolicy.containsBlocked("cafe\u0301", setOf("café")))
    }

    @Test fun blockedPhraseMarksRemainSignificantAndInsideWordBoundaries() {
        assertTrue(QueryPolicy.containsBlocked("कला", setOf("कला")))
        assertFalse(QueryPolicy.containsBlocked("कल", setOf("कला")))
        assertFalse(QueryPolicy.containsBlocked("कला", setOf("कल")))
        assertTrue(QueryPolicy.containsBlocked("क.ला", setOf("कला")))
    }
}
