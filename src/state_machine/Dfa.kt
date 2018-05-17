package state_machine

import kotlin.coroutines.experimental.buildSequence

data class Dfa<STATE, SYMBOL, TAG>(
        val initialState: STATE,
        val states: Map<STATE, Map<SYMBOL, STATE>> = mapOf(initialState to mapOf()),
        val groupsStart: Map<STATE, Set<TAG>> = mapOf(),
        val groupsEnd: Map<STATE, Set<TAG>> = mapOf()) {

  init {
    if (initialState !in states)
      throw UndefinedStateException("initial state: $initialState\n$this")
    for (state in groupsStart.keys) {
      if (state !in states) {
        throw UndefinedStateException("final state: $state\n$this")
      }
    }
    for ((start, transitions) in states) {
      for ((input, end) in transitions) {
        if (end !in states) {
          throw UndefinedStateException("transition end state: $end ($start, $input, $end)\n$this")
        }
      }
    }
  }

  operator fun get(state: STATE): Map<SYMBOL, STATE> {
    return states[state] ?: throw UndefinedStateException("$state")
  }

  fun evaluate_all(startState: STATE, inputs: Sequence<SYMBOL>, count: Int = -1) = buildSequence {
    val result = mutableListOf<Set<Group<TAG>>>()
    val possibleGroups = mutableMapOf<TAG, Group<TAG>>()
    var currentState = startState
    var numResults = 0
    for ((i, input) in inputs.withIndex()) {
      var tags = groupsEnd[currentState].orEmpty()
      val groups = mutableSetOf<Group<TAG>>()
      for (groupName in tags) {
        val group = possibleGroups[groupName] ?: continue
        group.end = i
        possibleGroups.remove(groupName)
        groups.add(group)
      }
      if (groups.isNotEmpty())
        result.add(groups)

      val transitions = states[currentState]
      if (transitions == null || transitions.isEmpty() || transitions[input] == null) {
        if (result.isNotEmpty()) {
          yield(result.toList())
          numResults++
          if (count in 0..numResults)
            break
        }
        result.clear()
        possibleGroups.clear()
        currentState = startState
        continue
      }

      tags = groupsStart[currentState].orEmpty()
      for (tag in tags)
        possibleGroups[tag] = Group(tag, i, -1)
      currentState = transitions[input]!!
    }
  }

  fun evaluate(startState: STATE, inputs: Sequence<SYMBOL>): List<Set<Group<TAG>>>? {
    return evaluate_all(startState, inputs, 1).firstOrNull()
  }
}

data class DfaBuilder<STATE, SYMBOL, TAG>(var initialState: STATE) {
  var states = mutableMapOf<STATE, Map<SYMBOL, STATE>>(initialState to mapOf())
  var groupsStart = mutableMapOf<STATE, Set<TAG>>()
  var groupsEnd = mutableMapOf<STATE, Set<TAG>>()

  fun transition(start: STATE, input: SYMBOL, end: STATE) = apply {
    val trans = states[start].orEmpty().toMutableMap()
    trans[input] = end
    states[start] = trans
    states[end] = states[end].orEmpty()
  }

  fun group(start: STATE, end: STATE, tag: TAG) = apply {
    groupsStart[start] = groupsStart[start].orEmpty().union(setOf(tag))
    groupsEnd[end] = groupsEnd[end].orEmpty().union(setOf(tag))
  }

  fun build() = Dfa(initialState, states, groupsStart, groupsEnd)
}

