package tech.sabai.contracteer.core

import org.junit.jupiter.api.Test
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
  fun `combining 2 success of different type is a success with te value of the second one`() {
    // when
    val result = success(1) andThen { success("John") }

    // expect
    val value = result.assertSuccess()
    assert(value == "John")
  }

  @Test
  fun `combining a success with an error is an error`() {
    // when
    val result = success(1) andThen { failure<Any>("Wrong Type") }

    // then
    val errors = result.assertFailure()
    assert(errors.size == 1)
    assert(errors.first() == "Wrong Type")
  }

  @Test
  fun `error message is prepended with property index when adding it`() {
    // when
    val result = failure<Any>("prop1", "Wrong Type").forIndex(1)

    // then
    val errors = result.assertFailure()
    assert(errors.first() == "'[1].prop1': Wrong Type")
  }

  @Test
  fun `map error message`() {
    // when
    val result = failure<Int>("error").forProperty("toto").mapErrors { "$it !!!" }

    // then
    val errors = result.assertFailure()
    assert(errors.size == 1)
    assert(errors.first() == "'toto': error !!!")
  }

  @Test
  fun `map error message has no effect for success`() {
    // when
    val result = success(1).mapErrors { "$it !!!" }

    // then
    val value = result.assertSuccess()
    assert(value == 1)
  }

  @Test
  fun `map a success`() {
    // when
    val result = success(1).map { it!! * 2 }

    // then
    val value = result.assertSuccess()
    assert(value == 2)
  }

  @Test
  fun `does not map a failure`() {
    // when
    val result = failure<Int>("Error").map { it!! * 2 }

    // then
    result.assertFailure()
  }

  @Test
  fun `flatMap a success`() {
    // when
    val result = success(1).flatMap { success(it!! * 2) }

    // then
    val value = result.assertSuccess()
    assert(value == 2)
  }
}