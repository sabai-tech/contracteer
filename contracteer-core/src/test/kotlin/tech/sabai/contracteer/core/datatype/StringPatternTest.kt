package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.assertFailure
import tech.sabai.contracteer.core.assertSuccess
import kotlin.test.Test

class StringPatternTest {

  // --- Happy path ---

  @Test
  fun `create succeeds for a simple pattern`() {
    // when
    val result = StringPattern.create("[a-zA-Z]+")

    // then
    result.assertSuccess()
  }

  @Test
  fun `validate accepts a matching value`() {
    // given
    val pattern = StringPattern.create("[a-zA-Z]+").assertSuccess()

    // when
    val result = pattern.validate("hello")

    // then
    assert(result.assertSuccess() == "hello")
  }

  @Test
  fun `validate rejects a non matching value with a clear error`() {
    // given
    val pattern = StringPattern.create("[0-9]+").assertSuccess()

    // when
    val errors = pattern.validate("abc").assertFailure()

    // then
    assert(errors.any { it.contains("abc") && it.contains("[0-9]+") })
  }

  @Test
  fun `randomValue produces values that match the pattern`() {
    // given
    val pattern = StringPattern.create("[a-z]{5,10}").assertSuccess()
    val compiled = Regex("[a-z]{5,10}")

    // when / then
    repeat(20) {
      val value = pattern.randomValue()
      assert(compiled.containsMatchIn(value)) { "Generated value '$value' does not match pattern" }
    }
  }

  // --- Failure paths ---

  @Test
  fun `create fails when pattern is not a valid Java regex`() {
    // when
    val errors = StringPattern.create("[unclosed").assertFailure()

    // then
    assert(errors.any { it.contains("not a valid regular expression") })
  }

  @Test
  fun `create fails when RgxGen cannot parse the pattern`() {
    // given — anchors inside alternation; RgxGen parse exception, no safe rewrite
    val errors = StringPattern.create("(?:^foo\$)|(?:^bar\$)").assertFailure()

    // then
    assert(errors.any { it.contains("value generator") || it.contains("random value generator") })
  }

  @Test
  fun `create fails when the generator produces values that do not match`() {
    // given — known silent-corruption pattern from a real-world batch
    val errors = StringPattern.create("^[0-9a-z\\.\\-]*(?<!\\.)\$").assertFailure()

    // then
    assert(errors.any { it.contains("do not match") })
  }

  @Test
  fun `create fails gracefully when pattern causes StackOverflowError during validation`() {
    // given — deeply nested quantifiers can overflow Java's recursive regex matcher
    val result = StringPattern.create(
      "abc:[a-z0-9-\\.]{1,63}:[a-z0-9-\\.]{0,63}:[a-z0-9-\\.]{0,63}:[a-z0-9-\\.]{0,63}:[^/].{0,1023}")

    // then — returns a Result (success or failure), never crashes with StackOverflowError
    assert(result.isSuccess() || result.isFailure())
  }

  // --- Rewrite integration (end-to-end) ---

  @Test
  fun `create succeeds for Java Is alias via rewrite`() {
    // given — \p{IsLetter} currently crashes RgxGen but is equivalent to \p{L}
    val pattern = StringPattern.create("\\p{IsLetter}+").assertSuccess()
    val compiled = Regex("\\p{IsLetter}+")

    // when / then
    repeat(20) {
      val value = pattern.randomValue()
      assert(compiled.containsMatchIn(value)) { "Generated value '$value' does not match pattern" }
    }
  }

  @Test
  fun `create succeeds for POSIX class via rewrite`() {
    // given — \p{Print} currently crashes RgxGen; ASCII subset is a safe lossy rewrite
    val pattern = StringPattern.create("\\p{Print}+").assertSuccess()
    val compiled = Regex("\\p{Print}+")

    // when / then
    repeat(20) {
      val value = pattern.randomValue()
      assert(compiled.containsMatchIn(value)) { "Generated value '$value' does not match pattern" }
    }
  }

  @Test
  fun `create succeeds for Unicode Other negation via rewrite`() {
    // given — \P{C}* currently crashes RgxGen; rewrite to complement union
    val pattern = StringPattern.create("\\P{C}+").assertSuccess()
    val compiled = Regex("\\P{C}+")

    // when / then
    repeat(20) {
      val value = pattern.randomValue()
      assert(compiled.containsMatchIn(value)) { "Generated value '$value' does not match pattern" }
    }
  }

  @Test
  fun `create succeeds for dash position in char class via rewrite`() {
    // given — RgxGen parses [0-9-\s] as a malformed range; reorder fixes it
    val pattern = StringPattern.create("[0-9-\\s]+").assertSuccess()
    val compiled = Regex("[0-9-\\s]+")

    // when / then
    repeat(20) {
      val value = pattern.randomValue()
      assert(compiled.containsMatchIn(value)) { "Generated value '$value' does not match pattern" }
    }
  }
}