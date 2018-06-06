package state_machine

import org.testng.annotations.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class DfaTest {
  val pattern_negative = ("--" + 0.toChar()).toCharArray().asSequence()
  val pattern_positive = ("abcaba" + 0.toChar()).toCharArray().asSequence()
  val pattern_positive_multi = ("--abcaba-abcaba-abcaba--" + 0.toChar()).toCharArray().asSequence()

  // (abc)|([abc]+)
  val dfa = DfaBuilder<Int, Char, String>(setOf(0))
          .transition(setOf(0), 'a', setOf(0, 1, 4, 5, 6))
          .transition(setOf(0), 'b', setOf(0, 4, 5, 6))
          .transition(setOf(0), 'c', setOf(0, 4, 5, 6))
          .transition(setOf(0, 1, 4, 5, 6), 'a', setOf(0, 1, 4, 5, 6))
          .transition(setOf(0, 1, 4, 5, 6), 'b', setOf(0, 2, 4, 5, 6))
          .transition(setOf(0, 1, 4, 5, 6), 'c', setOf(0, 4, 5, 6))
          .transition(setOf(0, 2, 4, 5, 6), 'a', setOf(0, 1, 4, 5, 6))
          .transition(setOf(0, 2, 4, 5, 6), 'b', setOf(0, 4, 5, 6))
          .transition(setOf(0, 2, 4, 5, 6), 'c', setOf(0, 3, 4, 5, 6))
          .transition(setOf(0, 3, 4, 5, 6), 'a', setOf(0, 1, 4, 5, 6))
          .transition(setOf(0, 3, 4, 5, 6), 'b', setOf(0, 4, 5, 6))
          .transition(setOf(0, 3, 4, 5, 6), 'c', setOf(0, 4, 5, 6))
          .transition(setOf(0, 4, 5, 6), 'a', setOf(0, 1, 4, 5, 6))
          .transition(setOf(0, 4, 5, 6), 'b', setOf(0, 4, 5, 6))
          .transition(setOf(0, 4, 5, 6), 'c', setOf(0, 4, 5, 6))
          .tag("G0", 0, 6)
          .tag("G1", 0, 3)
          .tag("G2", 0, 5)
          .build()

  fun dfa_group(start: Int): Map<String, Match> {
    return mapOf(
            "G0" to Match(0 + start, 6 + start),
            "G1" to Match(0 + start, 3 + start),
            "G2" to Match(0 + start, 6 + start)
    )
  }

  @Test
  fun DfaBuild() {
    DfaBuilder<Int, Char, String>(setOf(0))
            .transition(setOf(0), 'a', setOf(0))
            .tag("", 0, 0)
            .build()

    assertFailsWith(UndefinedStateException::class) {
      DfaBuilder<Int, Char, String>(setOf(0))
              .transition(setOf(0), 'a', setOf(0))
              .tag("", 1, 0)
              .build()
    }

    assertFailsWith(UndefinedStateException::class) {
      DfaBuilder<Int, Char, String>(setOf(0))
              .transition(setOf(0), 'a', setOf(0))
              .tag("", 0, 1)
              .build()
    }
  }

  @Test
  fun `evaluate negative`() {
    val actual = dfa.evaluate(pattern_negative).toList()
    val expected = listOf<Map<String, Match>>()
    assertEquals(expected, actual)
  }

  @Test
  fun `evaluate positive`() {
    val actual = dfa.evaluate(pattern_positive).toList()
    val expected = listOf(
            dfa_group(0)
    )
    assertEquals(expected, actual)
  }

  @Test
  fun `evaluate positive multi`() {
    val actual = dfa.evaluate(pattern_positive_multi).toList()
    val expected = listOf(
            dfa_group(2),
            dfa_group(9),
            dfa_group(16)
    )
    assertEquals(expected, actual)
  }
}