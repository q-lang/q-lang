package re

import org.testng.annotations.Test
import kotlin.test.assertEquals

internal class RegexTest {
  val text_negative = "--"

  val regex = Regex("""(abc)|([abc]+)""")
  val text = "--abcaba-abcaba-abcaba--"

  val regex_groups = Regex("""(a+)(b+((c+)d+))""")
  val regex_named_groups = Regex("""(?<A>a+)(?<BCD>b+(?<CD>(?<C>c+)d+))""")
  val text_groups = "--abcdaabbccdd-aaabbbcccddd--"

  val regex_final_tags = Regex("""((abc)+)""")
  val text_final_tags = "--abcabcabc--"

  // Tests  (* can be any of ?, +. *, {2}, {2,3})
  // abc
  // (ab)|(cd)
  // (ab)|((ab)*)
  // (ab)|([ab]*)
  // (ab)|(a*c*)
  // (a*)(b*((c*)d*))

  fun match(start: Int): Match {
    return Match(0 + start, 6 + start, mapOf(
            "1" to Group(0 + start, 3 + start),
            "2" to Group(0 + start, 6 + start)
    ))
  }

  fun match_groups(start: Int, stride: Int): Match {
    return Match(0 * stride + start, 4 * stride + start, mapOf(
            "1" to Group(0 * stride + start, 1 * stride + start),  // a
            "2" to Group(1 * stride + start, 4 * stride + start),  // bcd
            "3" to Group(2 * stride + start, 4 * stride + start),  // cd
            "4" to Group(2 * stride + start, 3 * stride + start))) // c
  }

  fun match_named_groups(start: Int, stride: Int): Match {
    return Match(0 * stride + start, 4 * stride + start, mapOf(
            "A" to Group(0 * stride + start, 1 * stride + start),   // a
            "BCD" to Group(1 * stride + start, 4 * stride + start), // bcd
            "CD" to Group(2 * stride + start, 4 * stride + start),  // cd
            "C" to Group(2 * stride + start, 3 * stride + start)))  // c
  }

  @Test
  fun `match negative`() {
    val actual = regex.match(text_negative).toList()
    val expected = listOf<Match>()
    assertEquals(expected, actual)
  }

  @Test
  fun `match positive`() {
    val actual = regex.match(text).toList()
    val expected = listOf(
            match(2),
            match(9),
            match(16)
    )
    assertEquals(expected, actual)
  }

  @Test
  fun `match groups`() {
    val actual = regex_groups.match(text_groups).toList()
    val expected = listOf(
            match_groups(2, 1),
            match_groups(6, 2),
            match_groups(15, 3)
    )
    assertEquals(expected, actual)
  }

  @Test
  fun `match named groups`() {
    val actual = regex_named_groups.match(text_groups).toList()
    val expected = listOf(
            match_named_groups(2, 1),
            match_named_groups(6, 2),
            match_named_groups(15, 3)
    )
    assertEquals(expected, actual)
  }

  @Test
  fun `match final tags`() {
    val actual = regex_final_tags.match(text_final_tags).toList()
    val expected = listOf(
            Match(2, 11, mapOf(
                    "1" to Group(2, 11),
                    "2" to Group(8, 11)
            ))
    )
    assertEquals(expected, actual)
  }
}