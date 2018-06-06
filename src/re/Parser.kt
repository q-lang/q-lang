package re

import state_machine.Nfa
import state_machine.NfaBuilder

data class Parser(val pattern: String) {
  companion object {
    const val MAIN_GROUP = "0"
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

  // TODO: make this function also create the transition so the new state is always created
  fun newState(): Int {
    return nfaBuilder.states.size
  }

  fun parse(): Nfa<Int, Char, String> {
    if (pattern.isEmpty())
      throw IllegalArgumentException("pattern cannot be empty")
    idx = 0
    nfaBuilder = NfaBuilder(0)
    nfaBuilder.tag(MAIN_GROUP, 0, expression(0))
    return nfaBuilder.build()
  }

  fun expression(start: Int): Int {
    val choices = mutableListOf<Int>()

    do {
      choices.add(sequence(start))
    } while (more() && accept('|'))

    if (choices.size == 1)
      return choices[0]

    val end = newState()
    for (choice in choices)
      nfaBuilder.transition(choice, null, end)
    return end
  }

  fun sequence(start: Int): Int {
    var start = start
    var end = start
    while (peek() != null && peek()!! !in ")]|") {
      end = quantifier(start)
      start = end
    }
    return end
  }

  fun quantifier(start: Int): Int {
    var end = atom(start)
    if (more()) {
      when (peek()) {
        '?' -> {
          /*  .?
           *  start -i-> end
           *    +--------^
           */
          nfaBuilder.transition(start, null, end)
          next()
        }

        '+' -> {
          /*  .+
           *  start -i-> end ---> new
           *    ^---------+
           */
          val new = newState()
          nfaBuilder.transition(end, null, new)
          nfaBuilder.transition(end, null, start)
          end = new
          next()
        }

        '*' -> {
          /*  .*
           *  start -i-> end ---> new
           *    \ ^-------+      /^
           *     +--------------+
           */
          val new = newState()
          nfaBuilder.transition(end, null, new)
          nfaBuilder.transition(end, null, start)
          nfaBuilder.transition(start, null, new)
          end = new
          next()
        }

        '{' -> throw NotImplementedError("a{n}, a{n,}, a{n,m}")
      }
    }
    return end
  }

  fun atom(start: Int): Int {
    var end = start
    when (peek()) {
      '(' -> {
        val openParenthesisIdx = idx
        expect('(')
        end = capture_group(start)
        expect(')', UnmatchedOpeningParenthesisError(pattern, openParenthesisIdx))
      }

      '[' -> end = character_class(start)

      '^', '$', '.' -> throw NotImplementedError("atom ${peek()}")

      '?', '+', '*', ')', ']', '|' -> return end

      else -> end = character(start)
    }
    return end
  }

  fun character_class(start: Int): Int {
    val end = newState()
    expect('[')
    var c = peek()
    while (c != null && c != ']') {
      character(start, end)
      if (peek(0) == '-' && peek(1) != ']') {
        expect('-')
        val startChar = c.toInt() + 1
        val endChar = peek()!!.toInt()
        if (start > end)
          throw InvalidCharacterClassRange(pattern, idx)
        for (i in startChar..endChar)
          nfaBuilder.transition(start, i.toChar(), end)
      }
      c = peek()
    }
    expect(']', UnmatchedOpeningSquareBracketError(pattern, idx))
    return end
  }

  fun character(start: Int, finalState: Int? = null): Int {
    val end = finalState ?: newState()
    when (peek()) {
      '\\' -> {
        expect('\\')

        when (peek()) {
          null -> throw DanglingBackslashError(pattern, idx - 1)

        // escape characters
          '0' -> nfaBuilder.transition(start, 0.toChar(), end)
          'a' -> nfaBuilder.transition(start, 0x07.toChar(), end)
          'e' -> nfaBuilder.transition(start, 0x1a.toChar(), end)
          'f' -> nfaBuilder.transition(start, 0x0c.toChar(), end)
          'n' -> nfaBuilder.transition(start, '\n', end)
          'r' -> nfaBuilder.transition(start, '\r', end)
          't' -> nfaBuilder.transition(start, '\t', end)

        // line break
          'R' -> {
            nfaBuilder.transition(start, '\r', end)
            nfaBuilder.transition(start, '\n', end)
            val midState = newState()
            nfaBuilder.transition(start, '\r', midState)
            nfaBuilder.transition(midState, '\n', end)
          }

        // shorthands
          'd' -> {
            // [0-9]
            for (c in '0'..'9')
              nfaBuilder.transition(start, c, end)
          }
          'w' -> {
            // [_a-zA-Z0-9]
            nfaBuilder.transition(start, '_', end)
            for (c in 'a'..'z') {
              nfaBuilder.transition(start, c, end)
              nfaBuilder.transition(start, c.toUpperCase(), end)
            }
          }
          's' -> {
            // [ \t\r\n\f]
            nfaBuilder.transition(start, ' ', end)
            nfaBuilder.transition(start, '\t', end)
            nfaBuilder.transition(start, '\r', end)
            nfaBuilder.transition(start, '\n', end)
            nfaBuilder.transition(start, 0x0b.toChar(), end)
            nfaBuilder.transition(start, 0x0c.toChar(), end)
          }

          else -> nfaBuilder.transition(start, peek(), end)
        }
      }

      else -> nfaBuilder.transition(start, peek(), end)
    }
    next()
    return end
  }

  fun capture_group(start: Int): Int {
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

    val groupStart = newState()
    nfaBuilder.transition(start, null, groupStart)
    val groupEnd = expression(groupStart)
    val end = newState()
    nfaBuilder.transition(groupEnd, null, end)
    nfaBuilder.tag(groupName, start, groupEnd)
    return end
  }
}
