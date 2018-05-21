package state_machine

data class Nfa<STATE, SYMBOL, TAG>(
        val initialState: STATE,
        val states: Map<STATE, Map<SYMBOL?, Set<STATE>>> = mapOf(initialState to mapOf()),
        val groupsStart: Map<STATE, Set<TAG>> = mapOf(),
        val groupsEnd: Map<STATE, Set<TAG>> = mapOf()) {

  init {
    if (initialState !in states)
      throw UndefinedStateException("initial state: $initialState\n$this")
    for (state in groupsStart.keys) {
      if (state !in states) {
        throw UndefinedStateException("final state: $state\n$this")
      }
    }
    for ((start, transitions) in states) {
      for ((input, endStates) in transitions) {
        for (end in endStates) {
          if (end !in states) {
            throw UndefinedStateException("transition end state: $end ($start, $input, $end)\n$this")
          }
        }
      }
    }
    for (state in groupsStart.keys) {
      if (state !in states)
        throw UndefinedStateException("group start state: $state\n$this")
    }
    for (state in groupsEnd.keys) {
      if (state !in states)
        throw UndefinedStateException("group end state: $state\n$this")
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

  fun toDfa(): Dfa<Set<STATE>, SYMBOL, TAG> {
    val dfaInitialState = epsilonClosure(initialState)
    val dfaStates = mutableMapOf<Set<STATE>, Map<SYMBOL, Set<STATE>>>()
    val dfaGroupsStart = mutableMapOf<Set<STATE>, Set<TAG>>()
    val dfaGroupsEnd = mutableMapOf<Set<STATE>, Set<TAG>>()

    val queue = mutableListOf(dfaInitialState)
    while (queue.isNotEmpty()) {
      val startStates = queue.removeAt(0)
      if (startStates in dfaStates)
        continue

      val transitions: MutableMap<SYMBOL, Set<STATE>> = mutableMapOf()
      for (start in startStates) {
        for ((input, endStates) in this[start]) {
          if (input != null) {
            transitions[input] = transitions[input].orEmpty().union(epsilonClosure(endStates))
          }
        }
      }

      for (endStates in transitions.values)
        queue.add(endStates)
      dfaStates[startStates] = transitions

      val groupsStartIntersect = startStates.intersect(groupsStart.keys)
      if (groupsStartIntersect.isNotEmpty()) {
        var finalGroupsStart: Set<TAG> = mutableSetOf()
        for (state in groupsStartIntersect)
          finalGroupsStart = finalGroupsStart.union(groupsStart[state]!!)
        dfaGroupsStart[startStates] = dfaGroupsStart[startStates].orEmpty().union(finalGroupsStart)
      }

      val groupsEndIntersect = startStates.intersect(groupsEnd.keys)
      if (groupsEndIntersect.isNotEmpty()) {
        var finalGroupsEnd: Set<TAG> = mutableSetOf()
        for (state in groupsEndIntersect)
          finalGroupsEnd = finalGroupsEnd.union(groupsEnd[state]!!)
        dfaGroupsEnd[startStates] = dfaGroupsEnd[startStates].orEmpty().union(finalGroupsEnd)
      }
    }

    return Dfa(dfaInitialState, dfaStates, dfaGroupsStart, dfaGroupsEnd)
  }

}

data class NfaBuilder<STATE, SYMBOL, TAG>(var initialState: STATE) {
  var states: MutableMap<STATE, Map<SYMBOL?, Set<STATE>>> = mutableMapOf(initialState to mapOf())
  var tagsStart: MutableMap<STATE, Set<TAG>> = mutableMapOf()
  var tagsEnd: MutableMap<STATE, Set<TAG>> = mutableMapOf()

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

  fun group(start: STATE, end: STATE, tag: TAG) = apply {
    tagsStart[start] = tagsStart[start].orEmpty().union(setOf(tag))
    tagsEnd[end] = tagsEnd[end].orEmpty().union(setOf(tag))
  }

  fun build() = Nfa(initialState, states, tagsStart, tagsEnd)
}
