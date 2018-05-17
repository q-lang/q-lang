package regex

import kotlin.coroutines.experimental.buildSequence

data class Match(
        val start: Int,
        val end: Int,
        private val groups: Map<String, Group> = mapOf()) {

  operator fun get(name: String): Group? {
    return groups[name]
  }
}

fun match_all(text: String, pattern: String, count: Int = -1) = buildSequence {
  var numResults = 0
  val dfa = Parser(pattern).parse().toDfa()
  for (dfaGroupsList in dfa.evaluate_all(setOf(0), text.asSequence())) {
    var mainGroup: state_machine.Group<String>? = null
    val groups = mutableMapOf<String, Group>()
    for (dfaGroups in dfaGroupsList) {
      for (group in dfaGroups) {
        if (group.name == Parser.MAIN_GROUP) {
          mainGroup = group
        } else {
          groups[group.name] = Group(group.start, group.end)
        }
      }
    }
    if (mainGroup != null) {
      yield(Match(mainGroup.start, mainGroup.end, groups))
      numResults++
      if (count in 0..numResults)
        break
    }
  }
}

fun match(text: String, pattern: String): Match? {
  return match_all(text, pattern, 1).firstOrNull()
}
