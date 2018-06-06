package state_machine

import kotlin.coroutines.experimental.buildSequence

data class Dfa<STATE, SYMBOL, TAG>(
        val initialState: Set<STATE>,
        val states: Map<Set<STATE>, Map<SYMBOL, Set<STATE>>> = mapOf(initialState to mapOf()),
        val tags: Map<TAG, Tag<STATE>> = mapOf()) {

  init {
    if (initialState !in states)
      throw UndefinedStateException("initial state: $initialState\n$this")
    for ((start, transitions) in states) {
      for ((input, end) in transitions) {
        if (end !in states) {
          throw UndefinedStateException("transition end state: $end ($start, $input, $end)\n$this")
        }
      }
    }

    val allStates = states.keys.flatten().toSet()
    for ((start, end) in tags.values) {
      if (start !in allStates)
        throw UndefinedStateException("tag state state: $start\n$this")
      if (end !in allStates)
        throw UndefinedStateException("tag end state: $end\n$this")
    }
  }

  operator fun get(state: Set<STATE>): Map<SYMBOL, Set<STATE>> {
    return states[state] ?: throw UndefinedStateException("$state")
  }

  fun evaluate(inputs: Sequence<SYMBOL>) = buildSequence {
    val result = mutableMapOf<TAG, Match>()
    var current = initialState
    val tagStarts = mutableMapOf<STATE, Set<TAG>>()
    for ((tag, range) in tags)
      tagStarts[range.start] = tagStarts[range.start].orEmpty().union(setOf(tag))
    val startedMatches = mutableMapOf<TAG, Int>()

    for ((i, input) in inputs.withIndex()) {
      var next = states[current]?.get(input)

      if (current != next) {
        // Try to close any started tag matches, only if they can't be closed on the next state
        for ((tag, matchStart) in startedMatches) {
          val (_, end) = tags[tag]!!
          if (end in current)
            result[tag] = Match(matchStart, i)
        }

        // If no valid transition, yield a result and reset the state machine
        if (next == null) {
          if (result.isNotEmpty())
            yield(result.toMap())
          startedMatches.clear()
          result.clear()
          current = initialState
          next = states[current]?.get(input)
          if (next == null)
            continue
        }

        // Try to state new tag matches
        for (state in current.intersect(tagStarts.keys)) {
          for (tag in tagStarts[state]!!)
            startedMatches[tag] = i
        }
      }

      current = next
    }
  }

  override fun toString(): String {
    var s = "DFA(\n"
    s += "  initialState=$initialState\n"
    s += "  states={\n"
    for ((start, transition) in states) {
      if (transition.isNotEmpty()) {
        for ((input, end) in transition)
          s += "    $start -$input-> $end\n"
      } else
        s += "    $start\n"
    }
    s += "  }\n"
    s += "  tags={\n"
    for ((name, tag) in tags)
      s += "    $name: $tag\n"
    s += "  }\n"
    s += ")"
    return s
  }
}

data class DfaBuilder<STATE, SYMBOL, TAG>(var initialState: Set<STATE>) {
  var states = mutableMapOf<Set<STATE>, Map<SYMBOL, Set<STATE>>>(initialState to mapOf())
  var tags = mutableMapOf<TAG, Tag<STATE>>()

  fun transition(start: Set<STATE>, input: SYMBOL, end: Set<STATE>) = apply {
    val transition = states[start].orEmpty().toMutableMap()
    transition[input] = end
    states[start] = transition
    states[end] = states[end].orEmpty()
  }

  fun transition(start: Set<STATE>, inputs: Sequence<SYMBOL>, end: Set<STATE>) = apply {
    for (input in inputs)
      transition(start, input, end)
  }

  fun tag(tag: TAG, start: STATE, end: STATE) = apply {
    tags[tag] = Tag(start, end)
  }

  fun build() = Dfa(initialState, states, tags)
}

