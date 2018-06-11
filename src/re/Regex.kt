package re

import automaton.FiniteStateMachine
import kotlin.coroutines.experimental.buildSequence

internal const val MAIN_GROUP = "0"

data class Regex(val pattern: String, val eof: Char = 0.toChar()) {
  val fsm: FiniteStateMachine<Int, Char, String> = RegexParser(pattern).parse()

  fun match(text: String) = buildSequence {
    for (tags in fsm.evaluate((text + eof).asSequence())) {
      val mainTag = tags[MAIN_GROUP]!!
      val groups = mutableMapOf<String, Group>()
      for ((tagName, tag) in tags) {
        if (tagName != MAIN_GROUP)
          groups[tagName] = Group(tag.start, tag.end)
      }
      yield(Match(mainTag.start, mainTag.end, groups))
    }
  }
}
