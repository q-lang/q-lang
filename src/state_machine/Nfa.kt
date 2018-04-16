package state_machine

import kotlin.coroutines.experimental.buildSequence

data class Nfa<STATE, SYMBOL>(
        var states: MutableMap<STATE, MutableMap<SYMBOL, MutableSet<STATE>>>,
        var initialState: STATE,
        var finalStates: MutableSet<STATE>) {

  operator fun get(state: STATE): MutableMap<SYMBOL, MutableSet<STATE>> {
    return states[state] ?: throw IllegalArgumentException("undefined state: $state")
  }

  operator fun get(state: STATE, symbol: SYMBOL): MutableSet<STATE> {
    return get(state)[symbol] ?: throw IllegalArgumentException("undefined symbol $symbol in state $state")
  }

  operator fun set(state: STATE, symbol: MutableMap<SYMBOL, MutableSet<STATE>>) {
    states[state] = symbol
  }

  operator fun set(startState: STATE, symbol: SYMBOL, endState: MutableSet<STATE>) {
    get(startState)[symbol] = endState
  }

  fun toDfa(): Dfa<Set<STATE>, SYMBOL> {
    val dfaInitialState = setOf(initialState)
    val dfa = Dfa<Set<STATE>, SYMBOL>(mutableMapOf(), dfaInitialState, mutableSetOf())
    val queue = mutableListOf(dfaInitialState)
    while (queue.isNotEmpty()) {
      val dfaStartState = queue.removeAt(0)
      if (dfaStartState in dfa.states)
        continue
      val dfaTransitions = mutableMapOf<SYMBOL, Set<STATE>>()
      for (state in dfaStartState) {
        for ((symbol, nfaEndStates) in transitions(this, state)) {
          dfaTransitions[symbol] = dfaTransitions[symbol].orEmpty().union(nfaEndStates)
        }
      }
      dfa[dfaStartState] = mutableMapOf()
      for ((symbol, endStates) in dfaTransitions) {
        dfa[dfaStartState, symbol] = endStates
        queue.add(endStates)
      }
      if (dfaStartState.intersect(finalStates).isNotEmpty())
        dfa.finalStates.add(dfaStartState)
    }
    return dfa
  }
}

fun <STATE, SYMBOL> transitions(nfa: Nfa<STATE, SYMBOL>, state: STATE) = buildSequence {
  for (symbol in nfa[state].keys) {
    yield(Pair(symbol, nfa[state, symbol]))
  }
}

fun <STATE, SYMBOL> transitions(nfa: Nfa<STATE, SYMBOL>) = buildSequence {
  for (startState in nfa.states.keys) {
    for ((symbol, endState) in transitions(nfa, startState)) {
      yield(Triple(startState, symbol, endState))
    }
  }
}
