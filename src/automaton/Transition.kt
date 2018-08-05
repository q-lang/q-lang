package automaton

data class Transition<STATE, SYMBOL, VALUE>(
        val start: STATE,
        val end: STATE,
        val input: SYMBOL?,
        val value: VALUE?
)