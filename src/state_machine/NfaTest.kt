package state_machine

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class NfaTest {
  @Test
  fun `epsilon NFA to DFA`() {
    // Epsilon NFA for regular expression: (a|b)*a
    val enfa = Nfa(0, mutableMapOf(
            0 to mutableMapOf(
                    null as String? to mutableSetOf(1, 7)
            ),
            1 to mutableMapOf(
                    null as String? to mutableSetOf(2, 4)
            ),
            2 to mutableMapOf(
                    "a" as String? to mutableSetOf(3)
            ),
            3 to mutableMapOf(
                    null as String? to mutableSetOf(6)
            ),
            4 to mutableMapOf(
                    "b" as String? to mutableSetOf(5)
            ),
            5 to mutableMapOf(
                    null as String? to mutableSetOf(6)
            ),
            6 to mutableMapOf(
                    null as String? to mutableSetOf(1, 7)
            ),
            7 to mutableMapOf(
                    "a" as String? to mutableSetOf(8)
            ),
            8 to mutableMapOf()
    ), mutableSetOf(8))

    val expectedDfa = Dfa(setOf(0, 1, 2, 4, 7), mutableMapOf(
            setOf(0, 1, 2, 4, 7) to mutableMapOf(
                    "a" to setOf(1, 2, 3, 4, 6, 7, 8),
                    "b" to setOf(1, 2, 4, 5, 6, 7)
            ),
            setOf(1, 2, 3, 4, 6, 7, 8) to mutableMapOf(
                    "a" to setOf(1, 2, 3, 4, 6, 7, 8),
                    "b" to setOf(1, 2, 4, 5, 6, 7)
            ),
            setOf(1, 2, 4, 5, 6, 7) to mutableMapOf(
                    "a" to setOf(1, 2, 3, 4, 6, 7, 8),
                    "b" to setOf(1, 2, 4, 5, 6, 7)
            )
    ), mutableSetOf(setOf(1, 2, 3, 4, 6, 7, 8)))

    assertEquals(expectedDfa, enfa.toDfa())
  }

  @Test
  fun `non-epsilon NFA to DFA`() {
    // Example from https://www.tutorialspoint.com/automata_theory/ndfa_to_dfa_conversion.htm
    val nfa = Nfa(0, mutableMapOf(
            0 to mutableMapOf(
                    "a" as String? to mutableSetOf(0, 1, 2, 3, 4),
                    "b" as String? to mutableSetOf(3, 4)
            ),
            1 to mutableMapOf(
                    "a" as String? to mutableSetOf(2),
                    "b" as String? to mutableSetOf(4)
            ),
            2 to mutableMapOf(
                    "b" as String? to mutableSetOf(1)
            ),
            3 to mutableMapOf(
                    "a" as String? to mutableSetOf(4)
            ),
            4 to mutableMapOf()
    ), mutableSetOf(4))

    val expected = Dfa(setOf(0), mutableMapOf(
            setOf(0) to mutableMapOf(
                    "a" to setOf(0, 1, 2, 3, 4),
                    "b" to setOf(3, 4)
            ),
            setOf(0, 1, 2, 3, 4) to mutableMapOf(
                    "a" to setOf(0, 1, 2, 3, 4),
                    "b" to setOf(1, 3, 4)
            ),
            setOf(3, 4) to mutableMapOf(
                    "a" to setOf(4)
            ),
            setOf(1, 3, 4) to mutableMapOf(
                    "a" to setOf(2, 4),
                    "b" to setOf(4)
            ),
            setOf(4) to mutableMapOf(),
            setOf(2, 4) to mutableMapOf(
                    "b" to setOf(1)
            ),
            setOf(1) to mutableMapOf(
                    "a" to setOf(2),
                    "b" to setOf(4)
            ),
            setOf(2) to mutableMapOf(
                    "b" to setOf(1)
            )

    ), mutableSetOf(
            setOf(0, 1, 2, 3, 4),
            setOf(1, 3, 4),
            setOf(2, 4),
            setOf(3, 4),
            setOf(4)))

    assertEquals(expected, nfa.toDfa())
  }
}