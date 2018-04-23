package state_machine

import org.testng.annotations.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class NfaTest {
  @Test
  fun `NFA validation`() {
    Nfa<Int, Char>(mapOf(0 to mapOf()), 0, setOf(0))
    assertFailsWith(UndefinedStateException::class) {
      Nfa<Int, Char>(mapOf(0 to mapOf()), 1, setOf(0))
    }
    assertFailsWith(UndefinedStateException::class) {
      Nfa<Int, Char>(mapOf(0 to mapOf()), 0, setOf(1))
    }
    Nfa(mapOf(0 to mapOf('a' as Char? to setOf(0))), 0, setOf(0))
    assertFailsWith(UndefinedStateException::class) {
      Nfa(mapOf(0 to mapOf('a' as Char? to setOf(1))), 0, setOf())
    }
  }

  @Test
  fun `epsilon NFA to DFA`() {
    // Epsilon NFA for regular expression: (a|b)*a
    val nfa = Nfa(mapOf(
            0 to mapOf(
                    null as String? to setOf(1, 7)
            ),
            1 to mapOf(
                    null as String? to setOf(2, 4)
            ),
            2 to mapOf(
                    "a" as String? to setOf(3)
            ),
            3 to mapOf(
                    null as String? to setOf(6)
            ),
            4 to mapOf(
                    "b" as String? to setOf(5)
            ),
            5 to mapOf(
                    null as String? to setOf(6)
            ),
            6 to mapOf(
                    null as String? to setOf(1, 7)
            ),
            7 to mapOf(
                    "a" as String? to setOf(8)
            ),
            8 to mapOf()
    ), 0, setOf(8))

    val expectedDfa = Dfa(mapOf(
            setOf(0, 1, 2, 4, 7) to mapOf(
                    "a" to setOf(1, 2, 3, 4, 6, 7, 8),
                    "b" to setOf(1, 2, 4, 5, 6, 7)
            ),
            setOf(1, 2, 3, 4, 6, 7, 8) to mapOf(
                    "a" to setOf(1, 2, 3, 4, 6, 7, 8),
                    "b" to setOf(1, 2, 4, 5, 6, 7)
            ),
            setOf(1, 2, 4, 5, 6, 7) to mapOf(
                    "a" to setOf(1, 2, 3, 4, 6, 7, 8),
                    "b" to setOf(1, 2, 4, 5, 6, 7)
            )
    ), setOf(0, 1, 2, 4, 7), setOf(setOf(1, 2, 3, 4, 6, 7, 8)))

    assertEquals(expectedDfa, nfa.toDfa())
  }

  @Test
  fun `non-epsilon NFA to DFA`() {
    // Example from https://www.tutorialspoint.com/automata_theory/ndfa_to_dfa_conversion.htm
    val nfa = Nfa(mapOf(
            0 to mapOf(
                    "a" as String? to setOf(0, 1, 2, 3, 4),
                    "b" as String? to setOf(3, 4)
            ),
            1 to mapOf(
                    "a" as String? to setOf(2),
                    "b" as String? to setOf(4)
            ),
            2 to mapOf(
                    "b" as String? to setOf(1)
            ),
            3 to mapOf(
                    "a" as String? to setOf(4)
            ),
            4 to mapOf()
    ), 0, setOf(4))

    val expected = Dfa(mapOf(
            setOf(0) to mapOf(
                    "a" to setOf(0, 1, 2, 3, 4),
                    "b" to setOf(3, 4)
            ),
            setOf(0, 1, 2, 3, 4) to mapOf(
                    "a" to setOf(0, 1, 2, 3, 4),
                    "b" to setOf(1, 3, 4)
            ),
            setOf(3, 4) to mapOf(
                    "a" to setOf(4)
            ),
            setOf(1, 3, 4) to mapOf(
                    "a" to setOf(2, 4),
                    "b" to setOf(4)
            ),
            setOf(4) to mapOf(),
            setOf(2, 4) to mapOf(
                    "b" to setOf(1)
            ),
            setOf(1) to mapOf(
                    "a" to setOf(2),
                    "b" to setOf(4)
            ),
            setOf(2) to mapOf(
                    "b" to setOf(1)
            )),
            setOf(0),
            setOf(
                    setOf(0, 1, 2, 3, 4),
                    setOf(1, 3, 4),
                    setOf(2, 4),
                    setOf(3, 4),
                    setOf(4)))

    assertEquals(expected, nfa.toDfa())
  }
}