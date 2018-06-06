package re

import org.testng.annotations.Test
import state_machine.NfaBuilder
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class ParserTest {
  @Test
  fun atom() {
    val actual = Parser("""a""").parse()
    val expected = NfaBuilder<Int, Char, String>(0)
            .transition(0, 'a', 1)
            .tag(Parser.MAIN_GROUP, 0, 1)
            .build()
    assertEquals(expected, actual)
  }

  @Test
  fun sequence() {
    val actual = Parser("""abc""").parse()
    val expected = NfaBuilder<Int, Char, String>(0)
            .transition(0, 'a', 1)
            .transition(1, 'b', 2)
            .transition(2, 'c', 3)
            .tag(Parser.MAIN_GROUP, 0, 3)
            .build()
    assertEquals(expected, actual)
  }

  @Test
  fun quantifier_zero_or_one() {
    val actual = Parser("""ab?c""").parse()
    val expected = NfaBuilder<Int, Char, String>(0)
            .transition(0, 'a', 1)
            .transition(1, 'b', 2)
            .transition(1, null, 2)
            .transition(2, 'c', 3)
            .tag(Parser.MAIN_GROUP, 0, 3)
            .build()
    assertEquals(expected, actual)
  }

  @Test
  fun quantifier_one_or_more() {
    val actual = Parser("""ab+c""").parse()
    val expected = NfaBuilder<Int, Char, String>(0)
            .transition(0, 'a', 1)
            .transition(1, 'b', 2)
            .transition(2, null, 1)
            .transition(2, null, 3)
            .transition(3, 'c', 4)
            .tag(Parser.MAIN_GROUP, 0, 4)
            .build()
    assertEquals(expected, actual)
  }

  @Test
  fun quantifier_zero_or_more() {
    val actual = Parser("""ab*c""").parse()
    val expected = NfaBuilder<Int, Char, String>(0)
            .transition(0, 'a', 1)
            .transition(1, 'b', 2)
            .transition(2, null, 3)
            .transition(2, null, 1)
            .transition(1, null, 3)
            .transition(3, 'c', 4)
            .tag(Parser.MAIN_GROUP, 0, 4)
            .build()
    assertEquals(expected, actual)
  }

  @Test
  fun expression() {
    val actual = Parser("""ab|cd|ef""").parse()
    val expected = NfaBuilder<Int, Char, String>(0)
            .transition(0, 'a', 1)
            .transition(1, 'b', 2)
            .transition(0, 'c', 3)
            .transition(3, 'd', 4)
            .transition(0, 'e', 5)
            .transition(5, 'f', 6)
            .transition(2, null, 7)
            .transition(4, null, 7)
            .transition(6, null, 7)
            .tag(Parser.MAIN_GROUP, 0, 7)
            .build()
    assertEquals(expected, actual)
  }

  @Test
  fun parenthesis() {
    val actual = Parser("""(a|b)(c|d)(e|f)""").parse()
    val expected = NfaBuilder<Int, Char, String>(0)
            .transition(0, 'a', 1)
            .transition(0, 'b', 2)
            .transition(1, null, 3)
            .transition(2, null, 3)
            .transition(3, 'c', 4)
            .transition(3, 'd', 5)
            .transition(4, null, 6)
            .transition(5, null, 6)
            .transition(6, 'e', 7)
            .transition(6, 'f', 8)
            .transition(7, null, 9)
            .transition(8, null, 9)
            .tag(Parser.MAIN_GROUP, 0, 9)
            .build()
    assertEquals(expected, actual)
  }

  @Test
  fun character_class() {
    val actual = Parser("""[abc]""").parse()
    val expected = NfaBuilder<Int, Char, String>(0)
            .transition(0, 'a', 1)
            .transition(0, 'b', 1)
            .transition(0, 'c', 1)
            .tag(Parser.MAIN_GROUP, 0, 1)
            .build()
    assertEquals(expected, actual)
  }

  @Test
  fun character_class_range() {
    val actual = Parser("""[a-c]""").parse()
    val expected = NfaBuilder<Int, Char, String>(0)
            .transition(0, 'a', 1)
            .transition(0, 'b', 1)
            .transition(0, 'c', 1)
            .tag(Parser.MAIN_GROUP, 0, 1)
            .build()
    assertEquals(expected, actual)
  }

  @Test
  fun character_class_first_dash() {
    val actual = Parser("""[-c]""").parse()
    val expected = NfaBuilder<Int, Char, String>(0)
            .transition(0, '-', 1)
            .transition(0, 'c', 1)
            .tag(Parser.MAIN_GROUP, 0, 1)
            .build()
    assertEquals(expected, actual)
  }

  @Test
  fun escape_characters() {
    val actual = Parser("""\0\a\e\f\n\r\t[\0\a\e\f\n\r\t]""").parse()
    val expected = NfaBuilder<Int, Char, String>(0)
            .transition(0, 0.toChar(), 1)
            .transition(1, 0x07.toChar(), 2)
            .transition(2, 0x1a.toChar(), 3)
            .transition(3, 0x0c.toChar(), 4)
            .transition(4, '\n', 5)
            .transition(5, '\r', 6)
            .transition(6, '\t', 7)
            .transition(7, 0.toChar(), 8)
            .transition(7, 0x07.toChar(), 8)
            .transition(7, 0x1a.toChar(), 8)
            .transition(7, 0x0c.toChar(), 8)
            .transition(7, '\n', 8)
            .transition(7, '\r', 8)
            .transition(7, '\t', 8)
            .tag(Parser.MAIN_GROUP, 0, 8)
            .build()
    assertEquals(expected, actual)
  }

  @Test
  fun escape_metacharacter() {
    val actual = Parser("""\[\\\^\$\.\|\?\*\+\(\)\{\}""").parse()
    val expected = NfaBuilder<Int, Char, String>(0)
            .transition(0, '[', 1)
            .transition(1, '\\', 2)
            .transition(2, '^', 3)
            .transition(3, '$', 4)
            .transition(4, '.', 5)
            .transition(5, '|', 6)
            .transition(6, '?', 7)
            .transition(7, '*', 8)
            .transition(8, '+', 9)
            .transition(9, '(', 10)
            .transition(10, ')', 11)
            .transition(11, '{', 12)
            .transition(12, '}', 13)
            .tag(Parser.MAIN_GROUP, 0, 13)
            .build()
    assertEquals(expected, actual)
  }

  @Test
  fun escape_line_break() {
    val actual = Parser("""\R[\R]""").parse()
    val expected = NfaBuilder<Int, Char, String>(0)
            .transition(0, '\r', 1)
            .transition(0, '\n', 1)
            .transition(0, '\r', 2)
            .transition(2, '\n', 1)
            .transition(1, '\r', 3)
            .transition(1, '\n', 3)
            .transition(1, '\r', 4)
            .transition(4, '\n', 3)
            .tag(Parser.MAIN_GROUP, 0, 3)
            .build()
    assertEquals(expected, actual)
  }

  @Test
  fun escape_octal() {
    val actual = Parser("""\052\100\o{52}\o{100}\o{00000052}""").parse()
    val expected = NfaBuilder<Int, Char, String>(0)
            .transition(0, '*', 1)
            .transition(1, '@', 2)
            .transition(2, '*', 3)
            .transition(3, '@', 4)
            .transition(4, '*', 5)
            .tag(Parser.MAIN_GROUP, 0, 5)
            .build()
    println(expected)
    println(actual)
    assertEquals(expected, actual)
  }

  @Test
  fun escape_hex() {
    val actual = Parser("""\x2a\x2A\x{2a}\x{0000002A}""").parse()
    val expected = NfaBuilder<Int, Char, String>(0)
            .transition(0, '*', 1)
            .transition(1, '*', 2)
            .transition(2, '*', 3)
            .transition(3, '*', 4)
            .tag(Parser.MAIN_GROUP, 0, 4)
            .build()
    println(expected)
    println(actual)
    assertEquals(expected, actual)
  }

  @Test
  fun escape_unicode() {
    val actual = Parser("""\u20ac\u20AC""").parse()
    val expected = NfaBuilder<Int, Char, String>(0)
            .transition(0, 0x20.toChar(), 1)
            .transition(1, 0xAC.toChar(), 2)
            .transition(2, 0x20.toChar(), 3)
            .transition(3, 0xAC.toChar(), 4)
            .tag(Parser.MAIN_GROUP, 0, 4)
            .build()
    println(expected)
    println(actual)
    assertEquals(expected, actual)
  }

  @Test
  fun escape_sequence() {
    val actual = Parser("""\Q[\^$.|?*+(){}\E""").parse()
    val expected = NfaBuilder<Int, Char, String>(0)
            .transition(0, '[', 1)
            .transition(1, '\\', 2)
            .transition(2, '^', 3)
            .transition(3, '$', 4)
            .transition(4, '.', 5)
            .transition(5, '|', 6)
            .transition(6, '?', 7)
            .transition(7, '*', 8)
            .transition(8, '+', 9)
            .transition(9, '(', 10)
            .transition(10, ')', 11)
            .transition(11, '{', 12)
            .transition(12, '}', 13)
            .tag(Parser.MAIN_GROUP, 0, 13)
            .build()
    println(expected)
    println(actual)
    assertEquals(expected, actual)
  }

  @Test
  fun control_characters() {
    val pattern = ('A'..'Z').joinToString("") { "\\c$it" } +
            ('a'..'z').joinToString("") { "\\c$it" } +
            "[" + ('A'..'Z').joinToString("") { "\\c$it" } +
            ('a'..'z').joinToString("") { "\\c$it" } + "]"
    val actual = Parser(pattern).parse()
    val expected = NfaBuilder<Int, Char, String>(0)
            .build()
    println(expected)
    println(actual)
    assertEquals(expected, actual)
  }

  @Test
  fun shorthand_digit() {
    val actual = Parser("""\d[\d]""").parse()
    val builder = NfaBuilder<Int, Char, String>(0)
    for (c in '0'..'9') {
      builder.transition(0, c, 1)
      builder.transition(1, c, 2)
    }
    val expected = builder
            .tag(Parser.MAIN_GROUP, 0, 2)
            .build()
    assertEquals(expected, actual)
  }

  @Test
  fun shorthand_word() {
    val actual = Parser("""\w[\w]""").parse()
    val builder = NfaBuilder<Int, Char, String>(0)
    builder.transition(0, '_', 1)
    builder.transition(1, '_', 2)
    for (c in 'a'..'z') {
      builder.transition(0, c, 1)
      builder.transition(0, c.toUpperCase(), 1)
      builder.transition(1, c, 2)
      builder.transition(1, c.toUpperCase(), 2)
    }
    val expected = builder
            .tag(Parser.MAIN_GROUP, 0, 2)
            .build()
    assertEquals(expected, actual)
  }

  @Test
  fun shorthand_space() {
    val actual = Parser("""\s[\s]""").parse()
    val expected = NfaBuilder<Int, Char, String>(0)
            .transition(0, ' ', 1)
            .transition(0, '\t', 1)
            .transition(0, '\r', 1)
            .transition(0, '\n', 1)
            .transition(0, 0x0b.toChar(), 1)
            .transition(0, 0x0c.toChar(), 1)
            .transition(1, ' ', 2)
            .transition(1, '\t', 2)
            .transition(1, '\r', 2)
            .transition(1, '\n', 2)
            .transition(1, 0x0b.toChar(), 2)
            .transition(1, 0x0c.toChar(), 2)
            .tag(Parser.MAIN_GROUP, 0, 2)
            .build()
    assertEquals(expected, actual)
  }

  @Test
  fun curly_braces() {
    val actual = Parser("""a{}b{0,2""").parse()
    val expected = NfaBuilder<Int, Char, String>(0)
            .transition(0, 'a', 1)
            .transition(1, '{', 2)
            .transition(2, '}', 3)
            .transition(3, 'b', 4)
            .transition(4, '{', 5)
            .transition(5, '0', 6)
            .transition(6, ',', 7)
            .transition(7, '2', 8)
            .tag(Parser.MAIN_GROUP, 0, 8)
            .build()
    println(expected)
    println(actual)
    assertEquals(expected, actual)
  }

  @Test
  fun quantifier_fixed() {
    val actual = Parser("""ab{3}c""").parse()
    val expected = NfaBuilder<Int, Char, String>(0)
            .transition(0, 'a', 1)
            .transition(1, 'b', 2)
            .transition(2, 'b', 3)
            .transition(3, 'b', 4)
            .transition(4, 'c', 5)
            .tag(Parser.MAIN_GROUP, 0, 5)
            .build()
    println(expected)
    println(actual)
    assertEquals(expected, actual)
  }

  @Test
  fun quantifier_range() {
    val actual = Parser("""ab{1,3}c""").parse()
    val expected = NfaBuilder<Int, Char, String>(0)
            .transition(0, 'a', 1)
            .transition(1, 'b', 2)
            .transition(2, null, 4)
            .transition(2, 'b', 3)
            .transition(3, null, 4)
            .transition(3, 'b', 4)
            .transition(4, 'c', 5)
            .tag(Parser.MAIN_GROUP, 0, 5)
            .build()
    println(expected)
    println(actual)
    assertEquals(expected, actual)
  }

  @Test
  fun groups() {
    val actual = Parser("""(?<G1>a)(?'G2'b)""").parse()
    val expected = NfaBuilder<Int, Char, String>(0)
            .transition(0, 'a', 1)
            .transition(1, 'b', 2)
            .tag("G1", 0, 1)
            .tag("G2", 1, 2)
            .tag(Parser.MAIN_GROUP, 0, 2)
            .build()
    assertEquals(expected, actual)
  }

  @Test
  fun `empty pattern`() {
    assertFailsWith(IllegalArgumentException::class) {
      Parser("").parse()
    }
  }

  @Test
  fun `dangling backslash`() {
    assertFailsWith(DanglingBackslashError::class) {
      Parser("""\""").parse()
    }
  }

  @Test
  fun `unmatched opening parenthesis`() {
    assertFailsWith(UnmatchedOpeningParenthesisError::class) {
      Parser("""(""").parse()
    }
  }

  @Test
  fun `unmatched opening square bracket`() {
    assertFailsWith(UnmatchedOpeningSquareBracketError::class) {
      Parser("""[""").parse()
    }
  }

  @Test
  fun `unmatched opening capture group`() {
    assertFailsWith(UnmatchedOpeningCaptureGroup::class) {
      Parser("""(?<""").parse()
    }
    assertFailsWith(UnmatchedOpeningCaptureGroup::class) {
      Parser("""(?'""").parse()
    }
    assertFailsWith(UnmatchedOpeningCaptureGroup::class) {
      Parser("""(?<a""").parse()
    }
    assertFailsWith(UnmatchedOpeningCaptureGroup::class) {
      Parser("""(?'a""").parse()
    }
    assertFailsWith(UnmatchedOpeningCaptureGroup::class) {
      Parser("""(?<a)""").parse()
    }
    assertFailsWith(UnmatchedOpeningCaptureGroup::class) {
      Parser("""(?'a)""").parse()
    }
    assertFailsWith(UnmatchedOpeningCaptureGroup::class) {
      Parser("""(?<a0""").parse()
    }
    assertFailsWith(UnmatchedOpeningCaptureGroup::class) {
      Parser("""(?'a0""").parse()
    }
    assertFailsWith(UnmatchedOpeningCaptureGroup::class) {
      Parser("""(?<a0)""").parse()
    }
    assertFailsWith(UnmatchedOpeningCaptureGroup::class) {
      Parser("""(?'a0)""").parse()
    }
  }

  @Test
  fun `invalid character class range`() {
    assertFailsWith(InvalidCharacterClassRange::class) {
      Parser("""[c-a]""").parse()
    }
  }

  @Test
  fun `invalid capture group syntax`() {
    assertFailsWith(InvalidCaptureGroupSyntax::class) {
      Parser("""(?""").parse()
    }
    assertFailsWith(InvalidCaptureGroupSyntax::class) {
      Parser("""(?a""").parse()
    }
  }

  @Test
  fun `invalid capture group name`() {
    assertFailsWith(InvalidCaptureGroupName::class) {
      Parser("""(?<*""").parse()
    }
    assertFailsWith(InvalidCaptureGroupName::class) {
      Parser("""(?<a*""").parse()
    }
  }
}