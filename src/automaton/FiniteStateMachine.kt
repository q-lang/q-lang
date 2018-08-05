package automaton

import graph.NonDeterministicGraph
import graph.SimpleGraph

data class FiniteStateMachine<STATE, SYMBOL>(
        val nfaStart: STATE,
        val nfa: NonDeterministicGraph<STATE, SYMBOL?>,
        val groups: Map<String, Group<STATE>>
) : Automaton<STATE, SYMBOL> {

  // Deterministic Finite Automaton
  val dfaStart: Set<STATE>
  val dfa: SimpleGraph<Set<STATE>, SYMBOL>

  init {
    val newDfa = SimpleGraph.New<Set<STATE>, SYMBOL>()

    dfaStart = nfa.epsilonClosure(setOf(nfaStart))
    val queue = mutableListOf(dfaStart)
    while (queue.isNotEmpty()) {
      val starts = queue.removeAt(0)
      if (starts in newDfa.vertices)
        continue

      val reachable: MutableMap<SYMBOL, Set<STATE>> = mutableMapOf()
      for (path in nfa.paths(nfa.epsilonClosure(starts), { p -> p.edge != null }))
        reachable[path.edge!!] = reachable[path.edge].orEmpty() union nfa.epsilonClosure(setOf(path.end))

      for (ends in reachable.values)
        queue.add(ends)

      newDfa[starts] = reachable
    }
    dfa = newDfa.build()
  }

  override fun transitionFunction(state: Set<STATE>, inputs: InputTape<SYMBOL>): Set<STATE>? {
    val input = inputs.next() ?: return null
    return dfa.vertices[state]?.get(input)
  }

  fun evaluate(inputs: Sequence<SYMBOL>): Map<String, Match>? {
    return evaluate(dfaStart, InputTape(inputs), groups)
  }

  fun evaluateAll(inputs: Sequence<SYMBOL>): Sequence<Map<String, Match>> {
    return evaluateAll(dfaStart, InputTape(inputs), groups)
  }

  data class New<STATE, SYMBOL>(val initialState: STATE) {
    val nfa = NonDeterministicGraph.New<STATE, SYMBOL?>()
    val groups: MutableMap<String, Group<STATE>> = mutableMapOf()

    init {
      nfa.graph.vertices[initialState] = mapOf()
    }

    fun transition(start: STATE, input: SYMBOL?, end: STATE) = apply {
      nfa.path(start, input, end)
    }

    fun group(name: String, start: STATE, end: STATE) = apply {
      groups[name] = Group(start, end)
    }

    fun build() = FiniteStateMachine(initialState, nfa.build(), groups)
  }
}

