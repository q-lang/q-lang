package state_machine

import org.testng.annotations.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class DfaTest {
  val pattern_negative = "--".toCharArray().asSequence()
  val pattern_positive = "--abcd-abcd-abcd--".toCharArray().asSequence()

  // (a)(b((c)d))
  val dfa = DfaBuilder<Int, Char, String>(0)
          .transition(0, 'a', 1)
          .transition(1, 'b', 2)
          .transition(2, 'c', 3)
          .transition(3, 'd', 4)
          .group(0, 4, "G0") // abcd
          .group(0, 1, "G1") // a
          .group(1, 4, "G2") // bcd
          .group(2, 4, "G3") // cd
          .group(2, 3, "G4") // c
          .build()

  fun dfa_group(start: Int): List<Set<Group<String>>> {
    return listOf(
            setOf(Group("G1", start + 0, start + 1)),
            setOf(Group("G4", start + 2, start + 3)),
            setOf(
                    Group("G0", start + 0, start + 4),
                    Group("G2", start + 1, start + 4),
                    Group("G3", start + 2, start + 4)
            )
    )
  }

  @Test
  fun DfaBuild() {
    DfaBuilder<Int, Char, String>(0)
            .transition(0, 'a', 0)
            .group(0, 0, "")
            .build()

    assertFailsWith(UndefinedStateException::class) {
      DfaBuilder<Int, Char, String>(0)
              .transition(0, 'a', 0)
              .group(1, 0, "")
              .build()
    }

    assertFailsWith(UndefinedStateException::class) {
      DfaBuilder<Int, Char, String>(0)
              .transition(0, 'a', 0)
              .group(0, 1, "")
              .build()
    }
  }

  @Test
  fun `evaluate_all negative`() {
    val actual = dfa.evaluate_all(0, pattern_negative).toList()
    val expected = listOf<List<Set<Group<String>>>>()
    assertEquals(expected, actual)
  }

  @Test
  fun `evaluate_all positive`() {
    val actual = dfa.evaluate_all(0, pattern_positive).toList()
    val expected = sequenceOf(
            dfa_group(2),
            dfa_group(7),
            dfa_group(12)
    ).toList()
    assertEquals(expected, actual)
  }

  @Test
  fun `evaluate_all positive count`() {
    val actual = dfa.evaluate_all(0, pattern_positive, 2).toList()
    val expected = sequenceOf(
            dfa_group(2),
            dfa_group(7)
    ).toList()
    assertEquals(expected, actual)
  }

  @Test
  fun `evaluate negative`() {
    val actual = dfa.evaluate(0, pattern_negative)
    val expected: List<Set<Group<String>>>? = null
    assertEquals(expected, actual)
  }

  @Test
  fun `evaluate positive`() {
    val actual = dfa.evaluate(0, pattern_positive)
    val expected: List<Set<Group<String>>>? = dfa_group(2)
    assertEquals(expected, actual)
  }
}