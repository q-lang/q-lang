package state_machine

data class Nfa<STATE, SYMBOL, TAG>(
        val initialState: STATE,
        val states: Map<STATE, Map<SYMBOL?, Set<STATE>>> = mapOf(initialState to mapOf()),
        val tags: Map<TAG, Tag<STATE>> = mapOf()) {

  init {
    if (initialState !in states)
      throw UndefinedStateException("initial state: $initialState\n$this")
    for ((start, transitions) in states) {
      for ((input, endStates) in transitions) {
        for (end in endStates) {
          if (end !in states) {
            throw UndefinedStateException("transition end state: $end ($start, $input, $end)\n$this")
          }
        }
      }
    }

    for ((start, end) in tags.values) {
      if (start !in states.keys)
        throw UndefinedStateException("tag state state: $start\n$this")
      if (end !in states.keys)
        throw UndefinedStateException("tag end state: $end\n$this")
    }
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

  fun epsilonClosure(state: STATE): Set<STATE> {
    return epsilonClosure(setOf(state))
  }

  fun toDfa(): Dfa<STATE, SYMBOL, TAG> {
    val dfaInitialState = epsilonClosure(initialState)
    val dfaStates = mutableMapOf<Set<STATE>, Map<SYMBOL, Set<STATE>>>()

    val queue = mutableListOf(dfaInitialState)
    while (queue.isNotEmpty()) {
      val startStates = queue.removeAt(0)
      if (startStates in dfaStates)
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
      dfaStates[startStates] = transitions
    }

    return Dfa(dfaInitialState, dfaStates, tags)
  }

  override fun toString(): String {
    var s = "NFA(\n"
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

data class NfaBuilder<STATE, SYMBOL, TAG>(var initialState: STATE) {
  var states: MutableMap<STATE, Map<SYMBOL?, Set<STATE>>> = mutableMapOf(initialState to mapOf())
  var tags: MutableMap<TAG, Tag<STATE>> = mutableMapOf()

  fun transition(start: STATE, input: SYMBOL?, endStates: Set<STATE>) = apply {
    val trans = states[start].orEmpty().toMutableMap()
    trans[input] = trans[input].orEmpty().union(endStates)
    states[start] = trans
    for (end in endStates)
      states[end] = states[end].orEmpty()
  }

  fun transition(start: STATE, input: SYMBOL?, end: STATE) = apply {
    transition(start, input, setOf(end))
  }

  fun tag(tag: TAG, start: STATE, end: STATE) = apply {
    tags[tag] = Tag(start, end)
  }

  fun build() = Nfa(initialState, states, tags)
}
