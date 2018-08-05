package re

import automaton.FiniteStateMachine
import kotlin.coroutines.experimental.buildSequence

internal const val MAIN_GROUP = "0"

data class Regex(val pattern: String, val eof: Char = 0.toChar()) {
  val fsm: FiniteStateMachine<Int, Char> = RegexParser(pattern).parse()

  private fun makeMatch(result: Map<String, automaton.Match>): Match {
    val mainGroup = result[MAIN_GROUP]!!
    val groups = mutableMapOf<String, Group>()
    for ((name, match) in result) {
      if (name != MAIN_GROUP)
        groups[name] = Group(match.start, match.end)
    }
    return Match(mainGroup.start, mainGroup.end, groups)
  }

  fun match(text: String): Match? {
    val result = fsm.evaluate((text + eof).asSequence()) ?: return null
    return makeMatch(result)
  }

  fun matches(text: String) = buildSequence {
    for (result in fsm.evaluateAll((text + eof).asSequence()))
      yield(makeMatch(result))
  }
}
