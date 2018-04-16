package state_machine

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class NfaTest {

  @BeforeEach
  fun setUp() {
  }

  @AfterEach
  fun tearDown() {
  }

  @Test
  fun toDfa() {
    val nfa = Nfa(mutableMapOf(
            "a" to mutableMapOf(
                    0 to mutableSetOf("a", "b", "c", "d", "e"),
                    1 to mutableSetOf("d", "e")
            ),
            "b" to mutableMapOf(
                    0 to mutableSetOf("c"),
                    1 to mutableSetOf("e")
            ),
            "c" to mutableMapOf(
                    1 to mutableSetOf("b")
            ),
            "d" to mutableMapOf(
                    0 to mutableSetOf("e")
            ),
            "e" to mutableMapOf()
    ), "a", mutableSetOf("e"))

    val expected = Dfa(mutableMapOf(
            setOf("a") to mutableMapOf(
                    0 to setOf("a", "b", "c", "d", "e"),
                    1 to setOf("d", "e")
            ),
            setOf("a", "b", "c", "d", "e") to mutableMapOf(
                    0 to setOf("a", "b", "c", "d", "e"),
                    1 to setOf("b", "d", "e")
            ),
            setOf("d", "e") to mutableMapOf(
                    0 to setOf("e")
            ),
            setOf("b", "d", "e") to mutableMapOf(
                    0 to setOf("c", "e"),
                    1 to setOf("e")
            ),
            setOf("e") to mutableMapOf(),
            setOf("c", "e") to mutableMapOf(
                    1 to setOf("b")
            ),
            setOf("b") to mutableMapOf(
                    0 to setOf("c"),
                    1 to setOf("e")
            ),
            setOf("c") to mutableMapOf(
                    1 to setOf("b")
            )

    ), setOf("a"), mutableSetOf(
            setOf("a", "b", "c", "d", "e"),
            setOf("b", "d", "e"),
            setOf("c", "e"),
            setOf("d", "e"),
            setOf("e")))

    assertEquals(expected, nfa.toDfa())
  }
}