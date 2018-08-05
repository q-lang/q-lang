package automaton

import kotlin.coroutines.experimental.buildSequence
import kotlin.math.max

interface Automaton<STATE, SYMBOL> {
  fun transitionFunction(state: Set<STATE>, inputs: InputTape<SYMBOL>): Set<STATE>?

  fun peek(inputs: Iterator<SYMBOL>): SYMBOL {
    return inputs.asSequence().first()
  }

  fun evaluate(
          initialState: Set<STATE>,
          inputs: InputTape<SYMBOL>,
          groups: Map<String, Group<STATE>>
  ): Map<String, Match>? {
    val result = mutableMapOf<String, Match>()
    val startedMatches = mutableMapOf<String, Int>()
    val groupStarts = mutableMapOf<STATE, Set<String>>()
    for ((name, group) in groups)
      groupStarts[group.start] = groupStarts[group.start].orEmpty().union(setOf(name))

    var current = initialState
    while (!inputs.isDone()) {
      // Get the next state via the state transitionFunction function
      val next = transitionFunction(current, inputs)

      if (current != next) {
        // Try to close any started group matches
        for ((name, matchStart) in startedMatches) {
          val (_, end) = groups[name]!!
          if (end in current)
            result[name] = Match(matchStart, inputs.index - 1)
        }

        if (next == null)
          break

        // Try to start New group matches
        for (state in current.intersect(groupStarts.keys)) {
          for (name in groupStarts[state]!!)
            startedMatches[name] = inputs.index - 1
        }
      }

      current = next
    }

    return if (result.isNotEmpty()) result else null
  }

  fun evaluateAll(
          initialState: Set<STATE>,
          inputs: InputTape<SYMBOL>,
          groups: Map<String, Group<STATE>>
  ) = buildSequence<Map<String, Match>> {
    while (!inputs.isDone()) {
      val lastIndex = inputs.index
      val result = evaluate(initialState, inputs, groups)
      if (result != null)
        yield(result)
      inputs.reset(max(inputs.index - 1, lastIndex + 1))
    }
  }
}

