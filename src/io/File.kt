package io

import kotlin.coroutines.experimental.buildSequence

private fun lines(filename: String) = buildSequence {
  java.io.File(filename).useLines { yieldAll(it) }
}

data class File(val filename: String) {
  fun lines(): Sequence<String> {
    return lines(filename)
  }
}

