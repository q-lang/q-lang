package state_machine

data class Dfa<STATE, SYMBOL>(
        var initialState: STATE,
        var states: MutableMap<STATE, MutableMap<SYMBOL, STATE>> =
                mutableMapOf(),
        var finalStates: MutableSet<STATE> = mutableSetOf()) {

  operator fun get(state: STATE): MutableMap<SYMBOL, STATE> {
    return states[state]!!
  }

  operator fun get(state: STATE, symbol: SYMBOL): STATE {
    return states[state]!![symbol]!!
  }

  operator fun set(state: STATE, symbol: MutableMap<SYMBOL, STATE>) {
    states[state] = symbol
  }

  operator fun set(startState: STATE, symbol: SYMBOL, endState: STATE) {
    states[startState]!![symbol] = endState
  }
}
