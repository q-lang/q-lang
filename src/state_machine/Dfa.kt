package state_machine

data class Dfa<STATE, SYMBOL>(
        var initialState: STATE,
        var states: MutableMap<STATE, MutableMap<SYMBOL, STATE>> =
                mutableMapOf(),
        var finalStates: MutableSet<STATE> = mutableSetOf())
