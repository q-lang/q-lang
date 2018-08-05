package automaton

class UndefinedStateException(state: String) : Exception("undefined state: $state")
class UndefinedSymbolException(symbol: String) : Exception("undefined symbol: $symbol")
class EmptyGrammarException : Exception("empty grammar")
