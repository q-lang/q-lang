package lexer

import io.File
import re.Regex
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

  val regex: Regex
  val tags: MutableMap<String, String> = mutableMapOf()
  val symbols: MutableMap<String, String> = mutableMapOf()
  val ignoreGroup: String = "<IGNORE>"
  val ignoreTag = "I${tags.size}"

  class Builder(val ignore_pattern: String = """\s+""", val EOF: String = "<EOF>") {
    val vocabulary: MutableMap<String, String> = mutableMapOf()
    val keywords: MutableSet<String> = mutableSetOf()

    fun rule(name: String, patterns: List<String>) = apply {
      rule(name, patterns.joinToString("|"))
    }

    fun rule(name: String, pattern: String) = apply {
      vocabulary[name] = pattern
    }

    fun keyword(name: String) = apply {
      keywords.add(name)
    }

    fun build() = Lexer(vocabulary, keywords, ignore_pattern, EOF)
  }

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

    tags[ignoreGroup] = ignoreTag
    symbols[ignoreTag] = ignoreGroup
    val patternGroups = mutableListOf("(?<$ignoreTag>$ignore_pattern)")

    for ((symbol, pattern) in vocabulary) {
      val tag = "R${tags.size}"
      tags[symbol] = tag
      symbols[tag] = symbol
      patternGroups += "(?<$tag>$pattern)"
    }
    for (symbol in keywords) {
      val tag = "K${tags.size}"
      tags[symbol] = tag
      symbols[tag] = symbol
      var pattern = ""
      for (c in symbol)
        pattern += if (c.isLetterOrDigit()) "$c" else "\\$c"
      patternGroups += "(?<$tag>$pattern)"
    }
    regex = Regex(patternGroups.joinToString("|"))

    val endStateToTags = mutableMapOf<Int, Set<String>>()
    for ((tagName, group) in regex.fsm.groups) {
      if (tagName[0].isDigit())
        continue
      endStateToTags[group.end] = endStateToTags[group.end].orEmpty().union(setOf(tagName))
    }

    val mainEndState = setOf(regex.fsm.groups[re.MAIN_GROUP]!!.end)
    val keywordsEndStates = keywords.map { regex.fsm.groups[tags[it]]!!.end }.toSet()
    for (state in regex.fsm.dfa.vertices.keys) {
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
      for (match in regex.matches(line)) {
        var matched: String? = null
        for ((tag, group) in match.groups) {
          val symbol = symbols[tag]!!
          if (group.end != match.end)
            continue
          if (symbol in keywords) {
            matched = symbol
            break
          }
          assert(matched == null)
          matched = symbol
        }

        assert(matched != null)
        // If it's not the group for ignore_pattern, yield that result
        if (matched!! != ignoreGroup)
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

