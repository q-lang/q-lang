package automaton

import org.testng.annotations.Test
import kotlin.test.assertEquals

internal class FiniteStateMachineTest {
  // (abc)|([abc]+)
  val fsm = FiniteStateMachine.Builder<Int, Char, String>(0)
          .transition(0, null, 1)
          .transition(1, 'a', 2)
          .transition(2, 'b', 3)
          .transition(3, 'c', 4)
          .transition(4, null, 5)
          .transition(0, null, 6)
          .transition(6, 'a', 7)
          .transition(6, 'b', 7)
          .transition(6, 'c', 7)
          .transition(7, null, 8)
          .transition(7, null, 6)
          .transition(8, null, 9)
          .transition(9, null, 10)
          .transition(5, null, 10)
          .tag("G0", 0, 10)
          .tag("G1", 0, 4)
          .tag("G2", 0, 8)
          .build()

  fun evaluate(fsm: FiniteStateMachine<Int, Char, String>, string: String): List<Map<String, Match>> {
    return fsm.evaluate((string + 0.toChar()).toCharArray().asSequence()).toList()
  }

  fun dfa_group(start: Int): Map<String, Match> {
    return mapOf(
            "G0" to Match(0 + start, 6 + start),
            "G1" to Match(0 + start, 3 + start),
            "G2" to Match(0 + start, 6 + start)
    )
  }

  @Test
  fun `epsilon NFA to DFA`() {
    // Epsilon NFA for regular expression: (a|b)*a
    val actual = FiniteStateMachine.Builder<Int, Char, String>(0)
            .transition(0, 'a', 1)
            .transition(0, 'b', 2)
            .transition(1, null, 3)
            .transition(2, null, 3)
            .transition(3, null, 4)
            .transition(3, null, 0)
            .transition(0, null, 4)
            .transition(4, 'a', 5)
            .build()

    val dfaInitialState = setOf(0, 4)
    val dfaStates = mapOf(
            setOf(0, 1, 3, 4, 5) to mapOf('a' to setOf(0, 1, 3, 4, 5), 'b' to setOf(0, 2, 3, 4)),
            setOf(0, 2, 3, 4) to mapOf('a' to setOf(0, 1, 3, 4, 5), 'b' to setOf(0, 2, 3, 4)),
            setOf(0, 4) to mapOf('a' to setOf(0, 1, 3, 4, 5), 'b' to setOf(0, 2, 3, 4))
    )
    assertEquals(dfaInitialState, actual.dfaInitialState)
    assertEquals(dfaStates, actual.dfaStates)
  }

  @Test
  fun `non-epsilon NFA to DFA`() {
    // Example from https://www.tutorialspoint.com/automata_theory/ndfa_to_dfa_conversion.htm
    val actual = FiniteStateMachine.Builder<Int, Char, String>(0)
            .transition(0, 'a', 0)
            .transition(0, 'a', 1)
            .transition(0, 'a', 2)
            .transition(0, 'a', 3)
            .transition(0, 'a', 4)
            .transition(0, 'b', 3)
            .transition(0, 'b', 4)
            .transition(1, 'a', 2)
            .transition(1, 'b', 4)
            .transition(2, 'b', 1)
            .transition(3, 'a', 4)
            .build()

    val dfaInitialState = setOf(0)
    val dfaStates = mapOf(
            setOf(0) to mapOf('a' to setOf(0, 1, 2, 3, 4), 'b' to setOf(3, 4)),
            setOf(0, 1, 2, 3, 4) to mapOf('a' to setOf(0, 1, 2, 3, 4), 'b' to setOf(1, 3, 4)),
            setOf(1) to mapOf('a' to setOf(2), 'b' to setOf(4)),
            setOf(1, 3, 4) to mapOf('a' to setOf(2, 4), 'b' to setOf(4)),
            setOf(2) to mapOf('b' to setOf(1)),
            setOf(3, 4) to mapOf('a' to setOf(4)),
            setOf(4) to mapOf(),
            setOf(4, 2) to mapOf('b' to setOf(1))
    )
    assertEquals(dfaInitialState, actual.dfaInitialState)
    assertEquals(dfaStates, actual.dfaStates)
  }

  @Test
  fun `evaluate negative`() {
    val actual = evaluate(fsm, "-")
    val expected = listOf<Map<String, Match>>()
    assertEquals(expected, actual)
  }

  @Test
  fun `evaluate positive`() {
    val actual = evaluate(fsm, "abcaba")
    val expected = listOf(
            dfa_group(0)
    )
    assertEquals(expected, actual)
  }

  @Test
  fun `evaluate positive multi`() {
    val actual = evaluate(fsm, "--abcaba-abcaba-abcaba--")
    val expected = listOf(
            dfa_group(2),
            dfa_group(9),
            dfa_group(16)
    )
    assertEquals(expected, actual)
  }
}