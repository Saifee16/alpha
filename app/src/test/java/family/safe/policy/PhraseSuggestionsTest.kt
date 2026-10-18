package family.safe.policy

import org.junit.Assert.*
import org.junit.Test

class PhraseSuggestionsTest {
    @Test fun suggestionsBecomeExactQueriesOnlyAfterParentSavesThem() {
        val original = Category("sports", "Sports", setOf("football"), true)
        val suggested = suggestSearchPhrases("football passing drills", emptySet())
        assertEquals(
            "football passing drills, football passing drills for kids, football passing drills for children, learn about football passing drills, tell me about football passing drills",
            suggested
        )
        assertFalse(QueryPolicy.evaluate("football passing drills for kids", listOf(original), emptySet()).allowed)
        val saved = original.copy(terms = suggested.split(", ").toSet())
        listOf("football passing drills", "football passing drills for kids", "football passing drills for children",
            "learn about football passing drills", "tell me about football passing drills").forEach {
            assertTrue(it, QueryPolicy.evaluate(it, listOf(saved), emptySet()).allowed)
        }
        listOf("football gaming", "football passing drills and gaming", "football passing drills and fractions",
            "https://football.com/passing-drills", "football passing drills for teens", "football passing drills for kids gaming").forEach {
            assertFalse(it, QueryPolicy.evaluate(it, listOf(saved), emptySet()).allowed)
        }
    }

    @Test fun categoryAssociationAndDisabledStateStillControlSuggestedPhrases() {
        val terms = suggestSearchPhrases("football passing drills", emptySet()).split(", ").toSet()
        val sports = Category("sports", "Sports", terms, true)
        val math = Category("math", "Math", setOf("fractions"), true)
        assertEquals(setOf("sports"), QueryPolicy.evaluate("football passing drills for kids", listOf(sports, math), emptySet()).categoryIds)
        assertFalse(QueryPolicy.evaluate("football passing drills for kids", listOf(sports.copy(enabled = false), math), emptySet()).allowed)
    }

    @Test fun suggestionsNormalizePreserveOriginalPhrasesAndRemainIdempotent() {
        val first = suggestSearchPhrases("  ＦＯＯＴＢＡＬＬ , fractions, football ", emptySet())
        assertTrue(first.startsWith("football, fractions, "))
        assertEquals(10, first.split(", ").size)
        assertEquals(first, suggestSearchPhrases(first, emptySet()))
        assertEquals("football for kids, learn about fractions, tell me about plants, plants for children",
            suggestSearchPhrases("football for kids, learn about fractions, tell me about plants, plants for children", emptySet()))
    }

    @Test fun blockedTermsRejectInputsAndGeneratedVariantsAndStillWinAfterSaving() {
        listOf("violence", "v.i.o.l.e.n.c.e", "football violence").forEach {
            assertThrows(IllegalArgumentException::class.java) { suggestSearchPhrases(it, setOf("violence")) }
        }
        assertThrows(IllegalArgumentException::class.java) { suggestSearchPhrases("football", setOf("kids")) }
        val saved = Category("sports", "Sports", suggestSearchPhrases("football", emptySet()).split(", ").toSet(), true)
        assertFalse(QueryPolicy.evaluate("football for kids", listOf(saved), setOf("football")).allowed)
    }

    @Test fun malformedAndEmptyInputIsRejectedRatherThanSilentlyNormalizedAway() {
        listOf("", " , ", "!!!", "football\n", "foot\u200bball", "football\u0000").forEach {
            assertThrows(it, IllegalArgumentException::class.java) { suggestSearchPhrases(it, emptySet()) }
        }
    }

    @Test fun oneHundredTermsFitButAdditionalSuggestionsCannotTruncateExistingTerms() {
        val twenty = (1..20).joinToString(", ") { "topic $it" }
        val result = suggestSearchPhrases(twenty, emptySet())
        assertEquals(100, result.split(", ").size)
        assertEquals(result, suggestSearchPhrases(result, emptySet()))
        assertThrows(IllegalArgumentException::class.java) {
            suggestSearchPhrases((1..21).joinToString(", ") { "topic $it" }, emptySet())
        }
        assertThrows(IllegalArgumentException::class.java) {
            suggestSearchPhrases((1..101).joinToString(", ") { "topic $it for kids" }, emptySet())
        }
    }

    @Test fun queryAndEditorBoundsRejectTheWholeSuggestionWithoutTruncation() {
        val longest = suggestSearchPhrases("x".repeat(242), emptySet())
        assertEquals(256, longest.split(", ").maxOf { it.length })
        assertThrows(IllegalArgumentException::class.java) { suggestSearchPhrases("x".repeat(243), emptySet()) }
        assertThrows(IllegalArgumentException::class.java) { suggestSearchPhrases("x".repeat(257), emptySet()) }
        assertThrows(IllegalArgumentException::class.java) { suggestSearchPhrases("x, ".repeat(1334), emptySet()) }
        assertThrows(IllegalArgumentException::class.java) {
            suggestSearchPhrases((1..12).joinToString(", ") { "topic $it ${"x".repeat(70)}" }, emptySet())
        }
    }
}
