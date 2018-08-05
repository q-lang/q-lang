package automaton

import org.testng.annotations.Test
import kotlin.test.assertEquals

internal class FiniteStateMachineTest {
  // (abc)|([abc]+)
  val fsm = FiniteStateMachine.New<Int, Char>(0)
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
          .group("G0", 0, 10)
          .group("G1", 0, 4)
          .group("G2", 0, 8)
          .build()

  fun evaluate(fsm: FiniteStateMachine<Int, Char>, input: String): List<Map<String, Match>> {
    return fsm.evaluateAll((input + 0.toChar()).toCharArray().asSequence()).toList()
  }

  fun group(start: Int): Map<String, Match> {
    return mapOf(
            "G0" to Match(0 + start, 6 + start),
            "G1" to Match(0 + start, 3 + start),
            "G2" to Match(0 + start, 6 + start)
    )
  }

  /*
  @Test
  fun `transition NFA to DFA`() {
    // Epsilon NFA for regular expression: (a|b)*a
    val actual = FiniteStateMachine.New<Int, Char, Void>(0)
            .transition(0, 1, 'a')
            .transition(0, 2, 'b')
            .transition(1, 3, null)
            .transition(2, 3, null)
            .transition(3, 4, null)
            .transition(3, 0, null)
            .transition(0, 4, null)
            .transition(4, 5, 'a')
            .build()

    val dfaInitialState = setOf(0, 4)
    val dfaTransitions = mapOf(
            setOf(0, 1, 3, 4, 5) to mapOf('a' to setOf(0, 1, 3, 4, 5), 'b' to setOf(0, 2, 3, 4)),
            setOf(0, 2, 3, 4) to mapOf('a' to setOf(0, 1, 3, 4, 5), 'b' to setOf(0, 2, 3, 4)),
            setOf(0, 4) to mapOf('a' to setOf(0, 1, 3, 4, 5), 'b' to setOf(0, 2, 3, 4))
    )
    assertEquals(dfaInitialState, actual.dfaStart)
    assertEquals(dfaTransitions, actual.dfa)
  }

  @Test
  fun `non-transition NFA to DFA`() {
    // Example from https://www.tutorialspoint.com/automata_theory/ndfa_to_dfa_conversion.htm
    val actual = FiniteStateMachine.New<Int, Char>(0)
            .transition(0, 0, 'a')
            .transition(0, 1, 'a')
            .transition(0, 2, 'a')
            .transition(0, 3, 'a')
            .transition(0, 4, 'a')
            .transition(0, 3, 'b')
            .transition(0, 4, 'b')
            .transition(1, 2, 'a')
            .transition(1, 4, 'b')
            .transition(2, 1, 'b')
            .transition(3, 4, 'a')
            .build()

    val dfaInitialState = setOf(0)
    val dfaTransitions = mapOf(
            setOf(0) to mapOf('a' to setOf(0, 1, 2, 3, 4), 'b' to setOf(3, 4)),
            setOf(0, 1, 2, 3, 4) to mapOf('a' to setOf(0, 1, 2, 3, 4), 'b' to setOf(1, 3, 4)),
            setOf(1) to mapOf('a' to setOf(2), 'b' to setOf(4)),
            setOf(1, 3, 4) to mapOf('a' to setOf(2, 4), 'b' to setOf(4)),
            setOf(2) to mapOf('b' to setOf(1)),
            setOf(3, 4) to mapOf('a' to setOf(4)),
            setOf(4) to mapOf(),
            setOf(4, 2) to mapOf('b' to setOf(1))
    )
    assertEquals(dfaInitialState, actual.dfaStart)
    assertEquals(dfaTransitions, actual.dfa)
  }
  */

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
            group(0)
    )
    assertEquals(expected, actual)
  }

  @Test
  fun `evaluate positive multi`() {
    val actual = evaluate(fsm, "--abcaba-abcaba-abcaba--")
    val expected = listOf(
            group(2),
            group(9),
            group(16)
    )
    assertEquals(expected, actual)
  }
}