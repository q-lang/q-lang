package state_machine

data class Nfa<STATE, SYMBOL>(
        val initialState: STATE,
        val states: MutableMap<STATE, MutableMap<SYMBOL?, MutableSet<STATE>>> =
                mutableMapOf(),
        val finalStates: MutableSet<STATE> = mutableSetOf()) {

  fun epsilonClosure(state: STATE): Set<STATE> {
    return epsilonClosure(setOf(state))
  }

  fun epsilonClosure(states: Set<STATE>): Set<STATE> {
    var result = states.toMutableSet()
    for (state in states) {
      val epsilonTransitions = this.states[state]!![null]
      if (epsilonTransitions != null) {
        result = result.union(epsilonClosure(epsilonTransitions)).toMutableSet()
      }
    }
    return result
  }

  fun toDfa(): Dfa<Set<STATE>, SYMBOL> {
    val dfa = Dfa<Set<STATE>, SYMBOL>(epsilonClosure(initialState))
    val queue = mutableListOf(dfa.initialState)
    while (queue.isNotEmpty()) {
      val startStates = queue.removeAt(0)
      if (startStates in dfa.states)
        continue
      val transitions = mutableMapOf<SYMBOL, Set<STATE>>()
      for (startState in startStates) {
        for ((symbol, endStates) in states[startState]!!) {
          if (symbol != null) {
            transitions[symbol] = transitions[symbol].orEmpty().union(epsilonClosure(endStates))
          }
        }
      }
      for (endStates in transitions.values)
        queue.add(endStates)
      dfa.states[startStates] = transitions
      if (startStates.intersect(finalStates).isNotEmpty())
        dfa.finalStates.add(startStates)
    }
    return dfa
  }
}