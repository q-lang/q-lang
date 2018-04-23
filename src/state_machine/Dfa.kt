package state_machine

data class Dfa<STATE, SYMBOL>(
        var states: Map<STATE, Map<SYMBOL, STATE>>,
        var initialState: STATE,
        var finalStates: Set<STATE> = setOf()) {

  operator fun get(state: STATE): Map<SYMBOL, STATE> {
    return states[state] ?: throw UndefinedStateException("$state")
  }
}
