package lexer

import org.testng.annotations.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class LexerTest {
  val lex = Lexer(
          mapOf(
                  "word" to """[a-zA-Z]+""",
                  "number" to """\d+"""
          ),
          setOf(
                  "hello"
          )
  )

  @Test
  fun `duplicate symbols`() {
    assertFailsWith(DuplicateSymbols::class) {
      Lexer(mapOf("A" to "a"), setOf("A"))
    }
  }

  @Test
  fun `empty symbol name`() {
    assertFailsWith(EmptySymbolName::class) {
      Lexer(mapOf("" to "a"))
    }
    assertFailsWith(EmptySymbolName::class) {
      Lexer(mapOf("A" to "a"), setOf(""))
    }
  }

  @Test
  fun `empty vocabulary pattern`() {
    assertFailsWith(EmptyVocabularyPattern::class) {
      Lexer(mapOf("A" to ""))
    }
  }

  @Test
  fun `ambiguous vocabulary`() {
    Lexer(mapOf("A" to "a"), setOf("a"))
    assertFailsWith(AmbiguousVocabulary::class) {
      Lexer(mapOf("A" to "a", "B" to "a"))
    }
    assertFailsWith(AmbiguousVocabulary::class) {
      Lexer(mapOf("A" to "a", "B" to """\w"""))
    }
  }

  @Test
  fun tokenize() {
    val text = """
      |hello
      |helloworld
      |hello42  42world""".trimMargin()
    val actual = lex.tokenize(text).toList()
    val seq = lex.tokenize(text)
    val expected = listOf(
            Token("hello", "hello", 0, 0),
            Token("word", "helloworld", 1, 0),
            Token("hello", "hello", 2, 0),
            Token("number", "42", 2, 5),
            Token("number", "42", 2, 9),
            Token("word", "world", 2, 11),
            Token(lex.EOF, "", 2, 16))
    assertEquals(expected, actual)
  }
}