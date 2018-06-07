package lexer

import io.File
import re.Parser
import re.RegularExpression
import java.io.StringReader
import kotlin.coroutines.experimental.buildSequence

private fun stringLines(string: String) = buildSequence {
  StringReader(string).useLines { yieldAll(it) }
}

data class Lexer(
        val vocabulary: Map<String, String>,
        val keywords: Set<String> = setOf(),
        val ignore_pattern: String = """\s+""",
        val EOF: String = "<EOF>") {

  private val regex: RegularExpression

  init {
    val duplicates = vocabulary.keys.intersect(keywords)
    if (duplicates.isNotEmpty())
      throw DuplicateSymbols(duplicates)
    for ((symbol, pattern) in vocabulary) {
      if (symbol.isEmpty())
        throw EmptySymbolName()
      if (pattern.isEmpty())
        throw EmptyVocabularyPattern(symbol)
    }
    for (symbol in keywords) {
      if (symbol.isEmpty())
        throw EmptySymbolName()
    }

    val groups = mutableListOf("($ignore_pattern)")
    for ((symbol, symbol_pattern) in vocabulary)
      groups += "(?<$symbol>$symbol_pattern)"
    for (symbol in keywords)
      groups += "(?<$symbol>$symbol)"
    val pattern = groups.joinToString("|")
    regex = RegularExpression(pattern)

    val endStateToTags = mutableMapOf<Int, Set<String>>()
    for ((tagName, tag) in regex.dfa.tags)
      endStateToTags[tag.end] = endStateToTags[tag.end].orEmpty().union(setOf(tagName))

    val mainEndState = setOf(regex.dfa.tags[Parser.MAIN_GROUP]!!.end)
    val keywordsEndStates = keywords.map { regex.dfa.tags[it]!!.end }.toSet()
    for (state in regex.dfa.states.keys) {
      val possibleEndStates = endStateToTags.keys.intersect(state) - mainEndState - keywordsEndStates
      if (possibleEndStates.size > 1) {
        val ambiguousTags = possibleEndStates.map { endStateToTags[it]!! }.flatten().toSet()
        throw AmbiguousVocabulary(ambiguousTags)
      }
    }
  }

  fun tokenize(lines: Sequence<String>) = buildSequence {
    var row = 0
    var lastEnd = 0
    for (line in lines) {
      for (match in regex.match(line)) {
        var matched: String? = null
        for ((symbol, group) in match.groups) {
          if (group.end != match.end)
            continue
          if (symbol in keywords) {
            matched = symbol
            break
          }
          assert(matched == null)
          matched = symbol
        }

        // If it's not the group for ignore_pattern (1), yield that result
        assert(matched != null)
        if (matched!! != "1")
          yield(Token(matched, line.slice(match.start until match.end), row, match.start))
        lastEnd = match.end
      }
      row++
    }
    yield(Token(EOF, "", row - 1, lastEnd))
  }

  fun tokenize(string: String): Sequence<Token> {
    return tokenize(stringLines(string))
  }

  fun tokenize(file: File): Sequence<Token> {
    return tokenize(file.lines())
  }
}

