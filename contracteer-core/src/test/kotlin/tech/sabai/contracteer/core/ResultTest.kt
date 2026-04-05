package tech.sabai.contracteer.core

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success

class ResultTest {

  @Test
  fun `Error message contains property name and error description separated by colon`() {
    // given
    val validationResult = failure<Any>("prop", "Wrong Type")

    // when
    val errors = validationResult.assertFailure()

    // then
    assert(errors.size == 1)
    assert(errors.first() == "'prop': Wrong Type")
  }

  @Test
  fun `Error message contains only error description when property name is not set`() {
    // given
    val validationResult = failure<Any>("Wrong Type")

    // when
    val errors = validationResult.assertFailure()

    // then
    assert(errors.size == 1)
    assert(errors.first() == "Wrong Type")
  }

  @Test
  fun `failure with multiple error messages returns all errors`() {
    // when
    val errors = failure<Any>("error 1", "error 2", "error 3").assertFailure()

    // then
    assert(errors.size == 3)
    assert(errors == listOf("error 1", "error 2", "error 3"))
  }

  @Test
  fun `failure with property index formats path with brackets`() {
    // when
    val errors = failure<Any>(2, "Wrong Type").assertFailure()

    // then
    assert(errors.first() == "'[2]': Wrong Type")
  }

  @Test
  fun `errors returns empty list on success`() {
    // when
    val errors = success(1).errors()

    // then
    assert(errors.isEmpty())
  }

  @Test
  fun `map transforms value on success`() {
    // when
    val result = success(1).map { it * 2 }

    // then
    val value = result.assertSuccess()
    assert(value == 2)
  }

  @Test
  fun `map does not transform on failure`() {
    // when
    val result = failure<Int>("Error").map { it * 2 }

    // then
    result.assertFailure()
  }

  @Test
  fun `flatMap transforms value on success`() {
    // when
    val result = success(1).flatMap { success(it * 2) }

    // then
    val value = result.assertSuccess()
    assert(value == 2)
  }

  @Test
  fun `flatMap does not transform on failure`() {
    // when
    val result = failure<Int>("Error").flatMap { success(it * 2) }

    // then
    val errors = result.assertFailure()
    assert(errors.first() == "Error")
  }

  @Test
  fun `mapErrors transforms error messages on failure`() {
    // when
    val result = failure<Int>("error").forProperty("toto").mapErrors { "$it !!!" }

    // then
    val errors = result.assertFailure()
    assert(errors.size == 1)
    assert(errors.first() == "'toto': error !!!")
  }

  @Test
  fun `mapErrors has no effect on success`() {
    // when
    val result = success(1).mapErrors { "$it !!!" }

    // then
    val value = result.assertSuccess()
    assert(value == 1)
  }

  @Test
  fun `andThen returns next result when first succeeds`() {
    // when
    val result = success(1) andThen { success("John") }

    // then
    val value = result.assertSuccess()
    assert(value == "John")
  }

  @Test
  fun `retypeError preserves errors on failure`() {
    // when
    val result = failure<Int>("error 1", "error 2", "error 3").retypeError<String>()

    // then
    val errors = result.assertFailure()
    assert(errors == listOf("error 1", "error 2", "error 3"))
  }

  @Test
  fun `retypeError throws on success`() {
    assertThrows<IllegalStateException> { success(1).retypeError<String>() }
  }
}