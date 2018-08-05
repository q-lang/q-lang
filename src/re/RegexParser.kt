package re

import automaton.FiniteStateMachine

internal class RegexParser(val pattern: String) {

  var idx = 0
  var groupIndex = 1
  var fsm = FiniteStateMachine.New<Int, Char>(0)

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

  // TODO: make this function also create the transitionFunction so the New state is always created
  fun newState(): Int {
    return fsm.nfa.graph.vertices.size
  }

  fun parse(): FiniteStateMachine<Int, Char> {
    if (pattern.isEmpty())
      throw IllegalArgumentException("pattern cannot be empty")
    idx = 0
    fsm = FiniteStateMachine.New(0)
    fsm.group(MAIN_GROUP, 0, expression(0))
    return fsm.build()
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
      fsm.transition(choice, null, end)
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
           *  start -index-> end
           *    +--------^
           */
          fsm.transition(start, null, end)
          next()
        }

        '+' -> {
          /*  .+
           *  start -index-> end ---> New
           *    ^---------+
           */
          val new = newState()
          fsm.transition(end, null, new)
          fsm.transition(end, null, start)
          end = new
          next()
        }

        '*' -> {
          /*  .*
           *  start -index-> end ---> New
           *    \ ^-------+      /^
           *     +--------------+
           */
          val new = newState()
          fsm.transition(end, null, new)
          fsm.transition(end, null, start)
          fsm.transition(start, null, new)
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
          fsm.transition(start, i.toChar(), end)
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
          '0' -> fsm.transition(start, 0.toChar(), end)
          'a' -> fsm.transition(start, 0x07.toChar(), end)
          'e' -> fsm.transition(start, 0x1a.toChar(), end)
          'f' -> fsm.transition(start, 0x0c.toChar(), end)
          'n' -> fsm.transition(start, '\n', end)
          'r' -> fsm.transition(start, '\r', end)
          't' -> fsm.transition(start, '\t', end)

        // line break
          'R' -> {
            fsm.transition(start, '\r', end)
            fsm.transition(start, '\n', end)
            val midState = newState()
            fsm.transition(start, '\r', midState)
            fsm.transition(midState, '\n', end)
          }

        // shorthands
          'd' -> {
            // [0-9]
            for (c in '0'..'9')
              fsm.transition(start, c, end)
          }
          'w' -> {
            // [_a-zA-Z0-9]
            fsm.transition(start, '_', end)
            for (c in 'a'..'z') {
              fsm.transition(start, c, end)
              fsm.transition(start, c.toUpperCase(), end)
            }
          }
          's' -> {
            // [ \t\r\n\f]
            fsm.transition(start, ' ', end)
            fsm.transition(start, '\t', end)
            fsm.transition(start, '\r', end)
            fsm.transition(start, '\n', end)
            fsm.transition(start, 0x0b.toChar(), end)
            fsm.transition(start, 0x0c.toChar(), end)
          }

          else -> fsm.transition(start, peek(), end)
        }
      }

      else -> fsm.transition(start, peek(), end)
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
    fsm.transition(start, null, groupStart)
    val groupEnd = expression(groupStart)
    val end = newState()
    fsm.transition(groupEnd, null, end)
    fsm.group(groupName, start, groupEnd)
    return end
  }
}
