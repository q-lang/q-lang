package automaton

import kotlin.coroutines.experimental.buildSequence

data class FiniteStateMachine<STATE, SYMBOL, TAG>(
        val initialState: STATE,
        val states: Map<STATE, Map<SYMBOL?, Set<STATE>>>,
        val tags: Map<TAG, Group<STATE>>) {

  val dfaInitialState: Set<STATE>
  val dfaStates: Map<Set<STATE>, Map<SYMBOL, Set<STATE>>>

  class Builder<STATE, SYMBOL, TAG>(val initialState: STATE) {
    val states: MutableMap<STATE, Map<SYMBOL?, Set<STATE>>> = mutableMapOf(initialState to mapOf())
    val tags: MutableMap<TAG, Group<STATE>> = mutableMapOf()

    fun transition(start: STATE, input: SYMBOL?, end: STATE) = apply {
      val trans = states[start].orEmpty().toMutableMap()
      trans[input] = trans[input].orEmpty().union(setOf(end))
      states[start] = trans
      states[end] = states[end].orEmpty()
    }

    fun tag(tag: TAG, start: STATE, end: STATE) = apply {
      tags[tag] = Group(start, end)
    }

    fun build() = FiniteStateMachine(initialState, states, tags)
  }

  init {
    dfaInitialState = epsilonClosure(setOf(initialState))
    val mutableDfaStates = mutableMapOf<Set<STATE>, Map<SYMBOL, Set<STATE>>>()

    val queue = mutableListOf(dfaInitialState)
    while (queue.isNotEmpty()) {
      val startStates = queue.removeAt(0)
      if (startStates in mutableDfaStates)
        continue

      val transitions: MutableMap<SYMBOL, Set<STATE>> = mutableMapOf()
      for (start in startStates) {
        for ((input, endStates) in this[start]) {
          if (input != null)
            transitions[input] = transitions[input].orEmpty().union(epsilonClosure(endStates))
        }
      }

      for (endStates in transitions.values)
        queue.add(endStates)
      mutableDfaStates[startStates] = transitions
    }
    dfaStates = mutableDfaStates
  }

  operator fun get(state: STATE): Map<SYMBOL?, Set<STATE>> {
    return states[state] ?: throw UndefinedStateException("$state")
  }

  fun epsilonClosure(states: Set<STATE>): Set<STATE> {
    val result = states.toMutableSet()
    val stack = states.toMutableList()
    while (stack.isNotEmpty()) {
      val startState = stack.removeAt(stack.lastIndex)  // pop
      val epsilonTransitions = this[startState][null] ?: continue
      for (endState in epsilonTransitions) {
        if (endState !in result) {
          stack.add(endState)
          result.add(endState)
        }
      }
    }
    return result
  }

  fun evaluate(inputs: Sequence<SYMBOL>) = buildSequence {
    val result = mutableMapOf<TAG, Match>()
    var current = dfaInitialState
    val tagStarts = mutableMapOf<STATE, Set<TAG>>()
    for ((tag, range) in tags)
      tagStarts[range.start] = tagStarts[range.start].orEmpty().union(setOf(tag))
    val startedMatches = mutableMapOf<TAG, Int>()

    for ((i, input) in inputs.withIndex()) {
      var next = dfaStates[current]?.get(input)

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
          current = dfaInitialState
          next = dfaStates[current]?.get(input)
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
    var s = "${this::class}(\n"
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
    s += "  dfaInitialState=$dfaInitialState\n"
    s += "  dfaStates={\n"
    for ((start, transition) in dfaStates) {
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
