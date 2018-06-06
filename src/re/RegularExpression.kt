package re

import state_machine.Dfa
import kotlin.coroutines.experimental.buildSequence

data class RegularExpression(val pattern: String, val eof: Char = 0.toChar()) {
  val dfa: Dfa<Int, Char, String> = Parser(pattern).parse().toDfa()

  fun match(text: String) = buildSequence {
    for (tags in dfa.evaluate((text + eof).asSequence())) {
      val mainTag = tags[Parser.MAIN_GROUP]!!
      var groups = mutableMapOf<String, Group>()
      for ((tagName, tag) in tags) {
        if (tagName != Parser.MAIN_GROUP)
          groups[tagName] = Group(tag.start, tag.end)
      }
      yield(Match(mainTag.start, mainTag.end, groups))
    }
  }
}
