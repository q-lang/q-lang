package regex

import state_machine.Nfa
import state_machine.NfaBuilder

data class Parser(val pattern: String) {
  companion object {
    const val MAIN_GROUP = ""
  }

  var idx = 0
  var groupIndex = 1
  var nfaBuilder = NfaBuilder<Int, Char, String>(0)

  fun more(): Boolean {
    return idx < pattern.length
  }

  fun peek(offset: Int = 0): Char? {
    val i = idx + offset
    if (i < pattern.length)
      return pattern[i]
    return null
  }

  fun next(): Char? {
    val c = peek()
    idx++
    return c
  }

  fun accept(c: Char): Boolean {
    val result = c == peek()
    if (result)
      next()
    return result
  }

  fun expect(c: Char, error: Exception? = null) {
    val actual = peek()
    if (actual == null || actual != c) {
      throw error ?: SyntaxError(pattern, idx + 1, "expected '$c', got '${actual}' (${actual?.toInt()})")
    }
    next()
  }

  fun newState(): Int {
    return nfaBuilder.states.size
  }

  fun parse(): Nfa<Int, Char, String> {
    if (pattern.isEmpty())
      throw IllegalArgumentException("pattern cannot be empty")
    idx = 0
    nfaBuilder = NfaBuilder(0)
    nfaBuilder.group(0, expression(0), MAIN_GROUP)
    return nfaBuilder.build()
  }

  fun expression(startState: Int): Int {
    val choices = mutableListOf<Int>()

    do {
      choices.add(sequence(startState))
    } while (more() && accept('|'))

    if (choices.size == 1)
      return choices[0]

    val endState = newState()
    for (choice in choices)
      nfaBuilder.transition(choice, null, endState)
    return endState
  }

  fun sequence(startState: Int): Int {
    var startState = startState
    var endState = startState
    while (peek() != null && peek()!! !in ")]|") {
      endState = quantifier(startState)
      startState = endState
    }
    return endState
  }

  fun quantifier(startState: Int): Int {
    val endState = atom(startState)
    if (more()) {
      when (peek()) {
        '?' -> {
          nfaBuilder.transition(startState, null, endState)
          next()
        }
        '+' -> {
          nfaBuilder.transition(endState, null, startState)
          next()
        }
        '*' -> {
          nfaBuilder.transition(startState, null, endState)
          nfaBuilder.transition(endState, null, startState)
          next()
        }
        '{' -> throw NotImplementedError("a{n}, a{n,}, a{n,m}")
      }
    }
    return endState
  }

  fun atom(startState: Int): Int {
    var endState = startState
    when (peek()) {
      '(' -> {
        val openParenthesisIdx = idx
        expect('(')
        endState = capture_group(startState)
        expect(')', UnmatchedOpeningParenthesisError(pattern, openParenthesisIdx))
      }

      '[' -> endState = character_class(startState)

      '^', '$', '.' -> throw NotImplementedError("atom ${peek()}")

      '?', '+', '*', ')', ']', '|' -> return endState

      else -> endState = character(startState)
    }
    return endState
  }

  fun character_class(startState: Int): Int {
    val endState = newState()
    expect('[')
    var c = peek()
    while (c != null && c != ']') {
      character(startState, endState)
      if (peek(0) == '-' && peek(1) != ']') {
        expect('-')
        val start = c.toInt() + 1
        val end = peek()!!.toInt()
        if (start > end)
          throw InvalidCharacterClassRange(pattern, idx)
        for (i in start..end)
          nfaBuilder.transition(startState, i.toChar(), endState)
      }
      c = peek()
    }
    expect(']', UnmatchedOpeningSquareBracketError(pattern, idx))
    return endState
  }


  fun character(startState: Int, finalState: Int? = null): Int {
    val endState = finalState ?: newState()
    when (peek()) {
      '\\' -> {
        expect('\\')

        when (peek()) {
          null -> throw DanglingBackslashError(pattern, idx - 1)

        // escape characters
          '0' -> nfaBuilder.transition(startState, 0.toChar(), endState)
          'a' -> nfaBuilder.transition(startState, 0x07.toChar(), endState)
          'e' -> nfaBuilder.transition(startState, 0x1a.toChar(), endState)
          'f' -> nfaBuilder.transition(startState, 0x0c.toChar(), endState)
          'n' -> nfaBuilder.transition(startState, '\n', endState)
          'r' -> nfaBuilder.transition(startState, '\r', endState)
          't' -> nfaBuilder.transition(startState, '\t', endState)

        // line break
          'R' -> {
            nfaBuilder.transition(startState, '\r', endState)
            nfaBuilder.transition(startState, '\n', endState)
            val midState = newState()
            nfaBuilder.transition(startState, '\r', midState)
            nfaBuilder.transition(midState, '\n', endState)
          }

        // shorthands
          'd' -> {
            // [0-9]
            for (c in '0'..'9')
              nfaBuilder.transition(startState, c, endState)
          }
          'w' -> {
            // [_a-zA-Z0-9]
            nfaBuilder.transition(startState, '_', endState)
            for (c in 'a'..'z') {
              nfaBuilder.transition(startState, c, endState)
              nfaBuilder.transition(startState, c.toUpperCase(), endState)
            }
          }
          's' -> {
            // [ \t\r\n\f]
            nfaBuilder.transition(startState, ' ', endState)
            nfaBuilder.transition(startState, '\t', endState)
            nfaBuilder.transition(startState, '\r', endState)
            nfaBuilder.transition(startState, '\n', endState)
            nfaBuilder.transition(startState, 0x0b.toChar(), endState)
            nfaBuilder.transition(startState, 0x0c.toChar(), endState)
          }

          else -> nfaBuilder.transition(startState, peek(), endState)
        }
      }

      else -> nfaBuilder.transition(startState, peek(), endState)
    }
    next()
    return endState
  }

  fun capture_group(startState: Int): Int {
    val groupName: String
    if (accept('?')) {
      val openCaptureGroupIdx = idx
      var c = next() ?: throw InvalidCaptureGroupSyntax(pattern, idx)
      val closeCaptureGroupChar = when (c) {
        '<' -> '>'
        '\'' -> '\''
        else -> throw InvalidCaptureGroupSyntax(pattern, idx)
      }
      val groupNameStart = idx
      c = next() ?: throw UnmatchedOpeningCaptureGroup(pattern, openCaptureGroupIdx)
      if (c !in 'a'..'z' && c !in 'A'..'Z' && c != '_')
        throw InvalidCaptureGroupName(pattern, idx)
      while (peek() != closeCaptureGroupChar) {
        c = next() ?: throw UnmatchedOpeningCaptureGroup(pattern, openCaptureGroupIdx)
        if (c == ')')
          throw UnmatchedOpeningCaptureGroup(pattern, openCaptureGroupIdx)
        if (c !in 'a'..'z' && c !in 'A'..'Z' && c !in '0'..'9' && c != '_')
          throw InvalidCaptureGroupName(pattern, idx)
      }
      groupName = pattern.slice(groupNameStart until idx)
      expect(closeCaptureGroupChar, InvalidCaptureGroupSyntax(pattern, idx))
    } else {
      groupName = "$groupIndex"
    }
    groupIndex++

    val endState = expression(startState)
    nfaBuilder.group(startState, endState, groupName)
    return endState
  }
}
