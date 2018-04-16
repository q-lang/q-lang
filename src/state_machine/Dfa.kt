package state_machine

data class Dfa<STATE, SYMBOL>(
        var states: MutableMap<STATE, MutableMap<SYMBOL, STATE>>,
        var initialState: STATE,
        var finalStates: MutableSet<STATE>) {

  operator fun get(state: STATE): MutableMap<SYMBOL, STATE> {
    return states[state] ?: throw IllegalArgumentException("undefined state: $state")
  }

  operator fun get(state: STATE, symbol: SYMBOL): STATE {
    return get(state)[symbol] ?: throw IllegalArgumentException("undefined symbol $symbol in state $state")
  }

  operator fun set(state: STATE, symbol: MutableMap<SYMBOL, STATE>) {
    states[state] = symbol
  }

  operator fun set(startState: STATE, symbol: SYMBOL, endState: STATE) {
    get(startState)[symbol] = endState
  }
}
