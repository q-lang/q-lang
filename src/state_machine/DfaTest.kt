package state_machine

import org.testng.annotations.Test
import kotlin.test.assertFailsWith

internal class DfaTest {
  @Test
  fun `DFA validation`() {
    Dfa<Int, Char>(mapOf(0 to mapOf()), 0, setOf(0))
    assertFailsWith(UndefinedStateException::class) {
      Dfa<Int, Char>(mapOf(0 to mapOf()), 1, setOf(0))
    }
    assertFailsWith(UndefinedStateException::class) {
      Dfa<Int, Char>(mapOf(0 to mapOf()), 0, setOf(1))
    }
    Dfa(mapOf(0 to mapOf('a' to 0)), 0, setOf(0))
    assertFailsWith(UndefinedStateException::class) {
      Dfa(mapOf(0 to mapOf('a' to 1)), 0, setOf(0))
    }
  }
}