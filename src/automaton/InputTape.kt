package automaton

data class InputTape<SYMBOL>(val inputs: Sequence<SYMBOL>) {
  val buffer = inputs.toList()
  var index = 0

  fun reset(newIndex: Int = 0) {
    index = newIndex
  }

  fun peek(n: Int = 0): SYMBOL? {
    return buffer.elementAtOrNull(index + n)
  }

  fun next(): SYMBOL? {
    val result = buffer.elementAtOrNull(index)
    index++
    return result
  }

  fun isDone(): Boolean {
    return index >= buffer.size
  }
}