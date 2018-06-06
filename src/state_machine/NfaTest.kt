package state_machine

import org.testng.annotations.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class NfaTest {
  @Test
  fun NfaBuild() {
    NfaBuilder<Int, Char, String>(0)
            .transition(0, 'a', 0)
            .tag("", 0, 0)
            .build()

    assertFailsWith(UndefinedStateException::class) {
      NfaBuilder<Int, Char, String>(0)
              .transition(0, 'a', 0)
              .tag("", 1, 0)
              .build()
    }

    assertFailsWith(UndefinedStateException::class) {
      NfaBuilder<Int, Char, String>(0)
              .transition(0, 'a', 0)
              .tag("", 0, 1)
              .build()
    }
  }

  @Test
  fun `epsilon NFA to DFA`() {
    // Epsilon NFA for regular expression: (a|b)*a
    val actual = NfaBuilder<Int, Char, String>(0)
            .transition(0, 'a', 1)
            .transition(0, 'b', 2)
            .transition(1, null, 3)
            .transition(2, null, 3)
            .transition(3, null, 4)
            .transition(3, null, 0)
            .transition(0, null, 4)
            .transition(4, 'a', 5)
            .tag("A", 0, 5)
            .build()
            .toDfa()

    val expected = DfaBuilder<Int, Char, String>(setOf(0, 4))
            .transition(setOf(0, 1, 3, 4, 5), 'a', setOf(0, 1, 3, 4, 5))
            .transition(setOf(0, 1, 3, 4, 5), 'b', setOf(0, 2, 3, 4))
            .transition(setOf(0, 2, 3, 4), 'a', setOf(0, 1, 3, 4, 5))
            .transition(setOf(0, 2, 3, 4), 'b', setOf(0, 2, 3, 4))
            .transition(setOf(0, 4), 'a', setOf(0, 1, 3, 4, 5))
            .transition(setOf(0, 4), 'b', setOf(0, 2, 3, 4))
            .tag("A", 0, 5)
            .build()

    assertEquals(expected, actual)
  }

  @Test
  fun `non-epsilon NFA to DFA`() {
    // Example from https://www.tutorialspoint.com/automata_theory/ndfa_to_dfa_conversion.htm
    val actual = NfaBuilder<Int, Char, String>(0)
            .transition(0, 'a', setOf(0, 1, 2, 3, 4))
            .transition(0, 'b', setOf(3, 4))
            .transition(1, 'a', 2)
            .transition(1, 'b', 4)
            .transition(2, 'b', 1)
            .transition(3, 'a', 4)
            .tag("A", 0, 4)
            .build()
            .toDfa()

    val expected = DfaBuilder<Int, Char, String>(setOf(0))
            .transition(setOf(0), 'a', setOf(0, 1, 2, 3, 4))
            .transition(setOf(0), 'b', setOf(3, 4))
            .transition(setOf(0, 1, 2, 3, 4), 'a', setOf(0, 1, 2, 3, 4))
            .transition(setOf(0, 1, 2, 3, 4), 'b', setOf(1, 3, 4))
            .transition(setOf(1), 'a', setOf(2))
            .transition(setOf(1), 'b', setOf(4))
            .transition(setOf(1, 3, 4), 'a', setOf(2, 4))
            .transition(setOf(1, 3, 4), 'b', setOf(4))
            .transition(setOf(2), 'b', setOf(1))
            .transition(setOf(3, 4), 'a', setOf(4))
            .transition(setOf(4, 2), 'b', setOf(1))
            .tag("A", 0, 4)
            .build()

    assertEquals(expected, actual)
  }
}