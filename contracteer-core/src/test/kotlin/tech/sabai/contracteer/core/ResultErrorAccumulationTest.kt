package tech.sabai.contracteer.core

import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success

class ResultErrorAccumulationTest {

  @Test
  fun `andThen returns failure when first succeeds and next fails`() {
    // when
    val result = success(1) andThen { failure<Any>("Wrong Type") }

    // then
    val errors = result.assertFailure()
    assert(errors.size == 1)
    assert(errors.first() == "Wrong Type")
  }

  @Test
  fun `andThen returns first errors when first fails and next succeeds`() {
    // when
    val result = failure<Int>("first error") andThen { success("John") }

    // then
    val errors = result.assertFailure()
    assert(errors == listOf("first error"))
  }

  @Test
  fun `andThen accumulates errors from both when first and next fail`() {
    // when
    val result = failure<Int>("first error") andThen { failure<String>("second error") }

    // then
    val errors = result.assertFailure()
    assert(errors == listOf("first error", "second error"))
  }

  @Test
  fun `combineWith succeeds when both results succeed`() {
    // when
    val result = success(1) combineWith success(2)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `combineWith returns errors from second when first succeeds`() {
    // when
    val result = success(1) combineWith failure<Any>("second error")

    // then
    val errors = result.assertFailure()
    assert(errors == listOf("second error"))
  }

  @Test
  fun `combineWith returns errors from first when second succeeds`() {
    // when
    val result = failure<Int>("first error") combineWith success(1)

    // then
    val errors = result.assertFailure()
    assert(errors == listOf("first error"))
  }

  @Test
  fun `combineWith accumulates errors from both results when both fail`() {
    // when
    val result = failure<Int>("first error") combineWith failure<Any>("second error")

    // then
    val errors = result.assertFailure()
    assert(errors == listOf("first error", "second error"))
  }

  // -- Error accumulation cap --

  @Test
  fun `accumulate caps errors at 25 with truncation message`() {
    // when
    val result = (1..30).toList().accumulate { failure<Int>("error $it") }

    // then
    val errors = result.assertFailure()
    assert(errors.size == 25)
    assert(errors.last().contains("additional errors were truncated"))
  }

  @Test
  fun `accumulate preserves all errors when under the cap`() {
    // when
    val result = (1..24).toList().accumulate { failure<Int>("error $it") }

    // then
    val errors = result.assertFailure()
    assert(errors.size == 24)
  }

  @Test
  fun `andThen caps errors at 25 with truncation message`() {
    // given
    val first = (1..15).toList().accumulate { failure<Int>("error $it") }

    // when
    val result = first andThen { (16..40).toList().accumulate { failure<Int>("error $it") } }

    // then
    val errors = result.assertFailure()
    assert(errors.size == 25)
    assert(errors.last().contains("additional errors were truncated"))
  }
}