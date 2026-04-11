package tech.sabai.contracteer.core.datatype

import kotlin.test.Test

class PatternRewriterTest {

  @Test
  fun `rewrites Java Is aliases to standard Unicode short forms`() {
    assertRewrite("\\p{IsLetter}", "\\p{L}")
    assertRewrite("\\p{IsDigit}", "[0-9]")
    assertRewrite("\\p{IsAlphabetic}", "[a-zA-Z]")
    assertRewrite("\\p{IsLowercase}", "[a-z]")
    assertRewrite("\\p{IsUppercase}", "[A-Z]")
    assertRewrite("\\p{IsWhiteSpace}", "[ \\t\\n\\r\\f\\x0B]")
  }

  @Test
  fun `rewrites POSIX classes to ASCII character class subsets`() {
    assertRewrite("\\p{Alpha}", "[a-zA-Z]")
    assertRewrite("\\p{Alnum}", "[a-zA-Z0-9]")
    assertRewrite("\\p{ASCII}", "[\\x00-\\x7F]")
    assertRewrite("\\p{Blank}", "[ \\t]")
    assertRewrite("\\p{Cntrl}", "[\\x00-\\x1F\\x7F]")
    assertRewrite("\\p{Digit}", "[0-9]")
    assertRewrite("\\p{Graph}", "[\\x21-\\x7E]")
    assertRewrite("\\p{Lower}", "[a-z]")
    assertRewrite("\\p{Print}", "[\\x20-\\x7E]")
    assertRewrite("\\p{Punct}", "[!-/:-@\\[-`{-~]")
    assertRewrite("\\p{Space}", "[ \\t\\n\\r\\f\\x0B]")
    assertRewrite("\\p{Upper}", "[A-Z]")
    assertRewrite("\\p{XDigit}", "[0-9A-Fa-f]")
  }

  @Test
  fun `rewrites Java java methods to ASCII equivalents`() {
    assertRewrite("\\p{javaLowerCase}", "[a-z]")
    assertRewrite("\\p{javaUpperCase}", "[A-Z]")
    assertRewrite("\\p{javaWhitespace}", "[ \\t\\n\\r\\f\\x0B]")
  }

  @Test
  fun `rewrites Unicode Other category to subcategory union or complement`() {
    assertRewrite("\\p{C}", "[\\x00-\\x1F\\x7F]")
    assertRewrite("\\P{C}", "[\\p{L}\\p{N}\\p{P}\\p{S}\\p{M}\\p{Z}]")
  }

  @Test
  fun `rewrites dash position in character class when followed by a shorthand escape`() {
    assertRewrite("[0-9-\\s]", "[0-9\\s-]")
    assertRewrite("[a-z-\\w]", "[a-z\\w-]")
    assertRewrite("[A-Z-\\d]", "[A-Z\\d-]")
  }

  @Test
  fun `strips every dash-shorthand pair inside a single character class`() {
    // Multiple dash-before-shorthand sequences collapse into one trailing literal dash
    assertRewrite("[a-z-\\s-\\w]", "[a-z\\s\\w-]")
    assertRewrite("[0-9-\\d-\\s-\\w]", "[0-9\\d\\s\\w-]")
  }

  @Test
  fun `does not duplicate trailing dash when one is already present`() {
    // Stripping produces content already ending in `-`; no extra dash appended
    assertRewrite("[a-z-\\s-]", "[a-z\\s-]")
  }

  @Test
  fun `leaves shorthand-before-dash character classes unchanged`() {
    // RgxGen handles `\s-0` (shorthand on the left of the dash) correctly
    assertRewrite("[\\s-0-9]", "[\\s-0-9]")
    assertRewrite("[\\w-a]", "[\\w-a]")
  }

  @Test
  fun `applies the rewrite independently to each character class in the pattern`() {
    assertRewrite("[a-z-\\s][0-9-\\w]", "[a-z\\s-][0-9\\w-]")
  }

  @Test
  fun `leaves a compliant pattern unchanged`() {
    assertRewrite("[a-zA-Z]+", "[a-zA-Z]+")
    assertRewrite("\\p{L}+", "\\p{L}+")
    assertRewrite("[0-9\\s-]+", "[0-9\\s-]+")
    assertRewrite("(?!foo)bar", "(?!foo)bar")
  }

  @Test
  fun `applies multiple rewrites in sequence when a pattern has several issues`() {
    val input = "\\p{IsLetter}+[0-9-\\s]*\\p{Print}"
    val expected = "\\p{L}+[0-9\\s-]*[\\x20-\\x7E]"
    assertRewrite(input, expected)
  }

  private fun assertRewrite(input: String, expected: String) {
    val actual = PatternRewriter.rewrite(input)
    assert(actual == expected) { "rewrite('$input') == '$actual', expected '$expected'" }
  }
}