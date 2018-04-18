package state_machine

data class Nfa<STATE, SYMBOL>(
        val initialState: STATE,
        val states: MutableMap<STATE, MutableMap<SYMBOL?, MutableSet<STATE>>> =
                mutableMapOf(),
        val finalStates: MutableSet<STATE> = mutableSetOf()) {

  operator fun get(state: STATE): MutableMap<SYMBOL?, MutableSet<STATE>> {
    return states[state]!!
  }

  operator fun get(state: STATE, symbol: SYMBOL?): MutableSet<STATE> {
    return get(state)[symbol]!!
  }

  operator fun set(state: STATE, symbol: MutableMap<SYMBOL?, MutableSet<STATE>>) {
    states[state] = symbol
  }

  operator fun set(startState: STATE, symbol: SYMBOL?, endState: MutableSet<STATE>) {
    states[startState]!![symbol] = endState
  }

  fun epsilonClosure(state: STATE): Set<STATE> {
    return epsilonClosure(setOf(state))
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

  fun toDfa(): Dfa<Set<STATE>, SYMBOL> {
    val dfa = Dfa<Set<STATE>, SYMBOL>(epsilonClosure(initialState))
    val queue = mutableListOf(dfa.initialState)
    while (queue.isNotEmpty()) {
      val startStates = queue.removeAt(0)
      if (startStates in dfa.states)
        continue
      val transitions = mutableMapOf<SYMBOL, Set<STATE>>()
      for (startState in startStates) {
        for ((symbol, endStates) in this[startState]) {
          if (symbol != null) {
            transitions[symbol] = transitions[symbol].orEmpty().union(epsilonClosure(endStates))
          }
        }
      }
      for (endStates in transitions.values)
        queue.add(endStates)
      dfa[startStates] = transitions
      if (startStates.intersect(finalStates).isNotEmpty())
        dfa.finalStates.add(startStates)
    }
    return dfa
  }
}