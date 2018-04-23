package state_machine

data class Nfa<STATE, SYMBOL>(
        val states: Map<STATE, Map<SYMBOL?, Set<STATE>>>,
        val initialState: STATE,
        val finalStates: Set<STATE> = setOf()) {

  operator fun get(state: STATE): Map<SYMBOL?, Set<STATE>> {
    return states[state] ?: throw UndefinedStateException("$state")
  }

  fun epsilonClosure(states: Set<STATE>): Set<STATE> {
    var result = states.toMutableSet()
    for (state in states) {
      val epsilonTransitions = this[state][null]
      if (epsilonTransitions != null) {
        result = result.union(epsilonClosure(epsilonTransitions)).toMutableSet()
      }
    }
    return result
  }

  fun epsilonClosure(states: Set<STATE>, cache: MutableMap<Set<STATE>, Set<STATE>>): Set<STATE> {
    var result = cache[states]
    if (result != null)
      return result
    result = epsilonClosure(states)
    cache[states] = result
    return result
  }

  fun epsilonClosure(state: STATE): Set<STATE> {
    return epsilonClosure(setOf(state))
  }

  fun epsilonClosure(state: STATE, cache: MutableMap<Set<STATE>, Set<STATE>>): Set<STATE> {
    return epsilonClosure(setOf(state), cache)
  }

  fun toDfa(): Dfa<Set<STATE>, SYMBOL> {
    val cache: MutableMap<Set<STATE>, Set<STATE>> = mutableMapOf()
    val dfaInitialState = epsilonClosure(initialState, cache)
    val dfaStates: MutableMap<Set<STATE>, Map<SYMBOL, Set<STATE>>> = mutableMapOf()
    val dfaFinalStates: MutableSet<Set<STATE>> = mutableSetOf()

    val queue = mutableListOf(dfaInitialState)
    while (queue.isNotEmpty()) {
      val startStates = queue.removeAt(0)
      if (startStates in dfaStates)
        continue
      val transitions: MutableMap<SYMBOL, Set<STATE>> = mutableMapOf()
      for (startState in startStates) {
        for ((symbol, endStates) in this[startState]) {
          if (symbol != null) {
            transitions[symbol] = transitions[symbol].orEmpty().union(epsilonClosure(endStates, cache))
          }
        }
      }
      for (endStates in transitions.values)
        queue.add(endStates)
      dfaStates[startStates] = transitions.toMap()
      if (startStates.intersect(finalStates).isNotEmpty())
        dfaFinalStates.add(startStates)
    }

    return Dfa(dfaStates.toMap(), dfaInitialState, dfaFinalStates.toSet())
  }
}