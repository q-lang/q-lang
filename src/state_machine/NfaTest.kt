package state_machine

import org.testng.annotations.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class NfaTest {
  @Test
  fun `NFA validation`() {
    // check initial state
    Nfa<Int, Char, String>(0, mapOf(0 to mapOf()), mapOf(0 to setOf("")))
    assertFailsWith(UndefinedStateException::class) {
      Nfa<Int, Char, String>(1, mapOf(0 to mapOf()), mapOf(0 to setOf("")))
    }

    // check final states
    assertFailsWith(UndefinedStateException::class) {
      Nfa<Int, Char, String>(0, mapOf(0 to mapOf()), mapOf(1 to setOf("")))
    }

    // check transition end states
    Nfa(0, mapOf(0 to mapOf('a' as Char? to setOf(0))), mapOf(0 to setOf("")))
    assertFailsWith(UndefinedStateException::class) {
      Nfa(0, mapOf(0 to mapOf('a' as Char? to setOf(1))), mapOf(0 to setOf("")))
    }
  }

  @Test
  fun NfaBuild() {
    // check initial state
    NfaBuilder<Int, Char, String>(0)
            .transition(0, 'a', 0)
            .build()
    assertFailsWith(UndefinedStateException::class) {
      NfaBuilder<Int, Char, String>(1)
              .transition(0, 'a', 0)
              .build()
    }

    // check final state
    NfaBuilder<Int, Char, String>(0)
            .transition(0, 'a', 0)
            .group(0, 0, "")
            .build()
    assertFailsWith(UndefinedStateException::class) {
      NfaBuilder<Int, Char, String>(0)
              .transition(0, 'a', 0)
              .group(0, 1, "")
              .build()
    }
  }

  @Test
  fun `epsilon NFA to DFA`() {
    // Epsilon NFA for regular expression: (a|b)*a
    val nfa = NfaBuilder<Int, Char, String>(0)
            .transition(0, null, setOf(1, 7))
            .transition(1, null, setOf(2, 4))
            .transition(2, 'a', 3)
            .transition(3, null, 6)
            .transition(4, 'b', 5)
            .transition(5, null, 6)
            .transition(6, null, setOf(1, 7))
            .transition(7, 'a', 8)
            .group(0, 8, "A")
            .build()

    val expected = DfaBuilder<Set<Int>, Char, String>(setOf(0, 1, 2, 4, 7))
            .transition(setOf(0, 1, 2, 4, 7), 'a', setOf(1, 2, 3, 4, 6, 7, 8))
            .transition(setOf(0, 1, 2, 4, 7), 'b', setOf(1, 2, 4, 5, 6, 7))
            .transition(setOf(1, 2, 3, 4, 6, 7, 8), 'a', setOf(1, 2, 3, 4, 6, 7, 8))
            .transition(setOf(1, 2, 3, 4, 6, 7, 8), 'b', setOf(1, 2, 4, 5, 6, 7))
            .transition(setOf(1, 2, 4, 5, 6, 7), 'a', setOf(1, 2, 3, 4, 6, 7, 8))
            .transition(setOf(1, 2, 4, 5, 6, 7), 'b', setOf(1, 2, 4, 5, 6, 7))
            .group(setOf(0), setOf(1, 2, 3, 4, 6, 7, 8), "A")
            .build()

    assertEquals(expected, nfa.toDfa())
  }

  @Test
  fun `non-epsilon NFA to DFA`() {
    // Example from https://www.tutorialspoint.com/automata_theory/ndfa_to_dfa_conversion.htm
    val nfa = NfaBuilder<Int, Char, String>(0)
            .transition(0, 'a', setOf(0, 1, 2, 3, 4))
            .transition(0, 'b', setOf(3, 4))
            .transition(1, 'a', 2)
            .transition(1, 'b', 4)
            .transition(2, 'b', 1)
            .transition(3, 'a', 4)
            .group(0, 4, "A")
            .build()
    val actual = nfa.toDfa()

    val expected = DfaBuilder<Set<Int>, Char, String>(setOf(0))
            .transition(setOf(0), 'a', setOf(0, 1, 2, 3, 4))
            .transition(setOf(0), 'b', setOf(3, 4))
            .transition(setOf(0, 1, 2, 3, 4), 'a', setOf(0, 1, 2, 3, 4))
            .transition(setOf(0, 1, 2, 3, 4), 'b', setOf(1, 3, 4))
            .transition(setOf(3, 4), 'a', setOf(4))
            .transition(setOf(1, 3, 4), 'a', setOf(2, 4))
            .transition(setOf(1, 3, 4), 'b', setOf(4))
            .transition(setOf(2, 4), 'b', setOf(1))
            .transition(setOf(1), 'a', setOf(2))
            .transition(setOf(1), 'b', setOf(4))
            .transition(setOf(2), 'b', setOf(1))
            .group(setOf(0), setOf(0, 1, 2, 3, 4), "A")
            .group(setOf(0, 1, 2, 3, 4), setOf(0, 1, 2, 3, 4), "A")
            .group(setOf(0), setOf(1, 3, 4), "A")
            .group(setOf(0), setOf(2, 4), "A")
            .group(setOf(0), setOf(3, 4), "A")
            .group(setOf(0), setOf(4), "A")
            .build()

    assertEquals(expected, actual)
  }
}