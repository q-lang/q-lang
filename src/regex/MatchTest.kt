package regex

import org.testng.annotations.Test
import kotlin.test.assertEquals

internal class MatchTest {
  val text_negative = "--"
  val text_positive = "--abcd-abcd-abcd--"
  val pattern = "abcd"
  val groups_pattern = "(a)(b((c)d))"
  val named_groups_pattern = "(?<A>a)(?<BCD>b(?<CD>(?<C>c)d))"

  fun match(start: Int): Match {
    return Match(start + 0, start + 4)
  }

  fun match_groups(start: Int): Match {
    return Match(start + 0, start + 4, mapOf(
            "1" to Group(start + 0, start + 1),  // a
            "2" to Group(start + 1, start + 4),  // bcd
            "3" to Group(start + 2, start + 4),  // cd
            "4" to Group(start + 2, start + 3))) // c
  }

  fun match_named_groups(start: Int): Match {
    return Match(start + 0, start + 4, mapOf(
            "A" to Group(start + 0, start + 1),  // a
            "BCD" to Group(start + 1, start + 4),  // bcd
            "CD" to Group(start + 2, start + 4),  // cd
            "C" to Group(start + 2, start + 3))) // c
  }

  @Test
  fun `match_all negative`() {
    val actual = match_all(text_negative, pattern).toList()
    val expected = listOf<Match>()
    assertEquals(expected, actual)
  }

  @Test
  fun `match_all positive`() {
    val actual = match_all(text_positive, pattern).toList()
    val expected = listOf(
            match(2),
            match(7),
            match(12)
    )
    assertEquals(expected, actual)
  }

  @Test
  fun `match_all positive count`() {
    val actual = match_all(text_positive, pattern, 2).toList()
    val expected = listOf(
            match(2),
            match(7)
    )
    assertEquals(expected, actual)
  }

  @Test
  fun `match_all groups`() {
    val actual = match_all(text_positive, groups_pattern).toList()
    val expected = listOf(
            match_groups(2),
            match_groups(7),
            match_groups(12)
    )
    assertEquals(expected, actual)
  }

  @Test
  fun `match_all named groups`() {
    val actual = match_all(text_positive, named_groups_pattern).toList()
    val expected = listOf(
            match_named_groups(2),
            match_named_groups(7),
            match_named_groups(12)
    )
    assertEquals(expected, actual)
  }

  @Test
  fun `match negative`() {
    val actual = match(text_negative, pattern)
    val expected = null
    assertEquals(expected, actual)
  }

  @Test
  fun `match positive`() {
    val actual = match(text_positive, pattern)
    val expected = Match(2, 6)
    assertEquals(expected, actual)
  }

  @Test
  fun `match groups`() {
    val actual = match(text_positive, groups_pattern)
    val expected = match_groups(2)
    assertEquals(expected, actual)
  }

  @Test
  fun `match named groups`() {
    val actual = match(text_positive, named_groups_pattern)
    val expected = match_named_groups(2)
    assertEquals(expected, actual)
  }
}