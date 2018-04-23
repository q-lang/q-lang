package state_machine

data class Dfa<STATE, SYMBOL>(
        var states: Map<STATE, Map<SYMBOL, STATE>>,
        var initialState: STATE,
        var finalStates: Set<STATE> = setOf()) {

  init {
    if (initialState !in states)
      throw UndefinedStateException("initial state: $initialState")
    for (state in finalStates) {
      if (state !in states) {
        throw UndefinedStateException("final state: $state")
      }
    }
    for ((startState, transitions) in states) {
      for ((symbol, endState) in transitions) {
        if (endState !in states) {
          throw UndefinedStateException("transition end state: $endState ($startState, $symbol, $endState)")
        }
      }
    }
  }

  operator fun get(state: STATE): Map<SYMBOL, STATE> {
    return states[state] ?: throw UndefinedStateException("$state")
  }
}
