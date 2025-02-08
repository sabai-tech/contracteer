package tech.sabai.contracteer.core.validation

import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success

class ResultTest {

  @Test
  fun `Error message contains property name and error description separated by colon`() {
    // given
    val validationResult = failure<Any>("prop", "Wrong Type")

    // when
    val errors = validationResult.errors()

    // then
    assert(errors.size == 1)
    assert(errors.first() == "'prop': Wrong Type")
  }

  @Test
  fun `Error message contains only error description when property name is not set`() {
    // given
    val validationResult = failure<Any>("Wrong Type")

    // when
    val errors = validationResult.errors()

    // then
    assert(errors.size == 1)
    assert(errors.first() == "Wrong Type")
  }

  @Test
  fun `combining 2 success of different type is a success with te value of the second one`() {
    // when
    val result = success(1) andThen { success("John") }

    // expect
    assert(result.isSuccess())
    assert(result.value == "John")
  }

  @Test
  fun `combining a success with an error is an error`() {
    // when
    val result = success(1) andThen { failure<Any>("Wrong Type") }

    // then
    assert(result.isFailure())
    assert(result.errors().size == 1)
    assert(result.errors().first() == "Wrong Type")
    assert(result.value == null)
  }

  @Test
  fun `error message is prepended with property index when adding it`() {
    // when
    val result = failure<Any>("prop1", "Wrong Type").forIndex(1)

    // then
    assert(result.errors().first() == "'[1].prop1': Wrong Type")
  }

  @Test
  fun `map error message`() {
    // when
    val result = failure<Int>("error").forProperty("toto").mapErrors { "$it !!!" }

    // then
    assert(result.isFailure())
    assert(result.errors().size == 1)
    assert(result.errors().first() == "'toto': error !!!")
  }

  @Test
  fun `map error message has no effect for success`() {
    // when
    val result = success(1).mapErrors { "$it !!!" }

    // then
    assert(result.isSuccess())
    assert(result.value == 1)
    assert(result.errors().isEmpty())
  }

  @Test
  fun `map a success`() {
    // when
    val result = success(1).map { it!! * 2 }

    // then
    assert(result.isSuccess())
    assert(result.value == 2)
    assert(result.errors().isEmpty())
  }

  @Test
  fun `does not map a failure`() {
    // when
    val result = failure<Int>("Error").map { it!! * 2 }

    // then
    assert(result.isFailure())
    assert(result.value == null)
    assert(result.errors().size == 1)
  }

  @Test
  fun `flatMap a success`() {
    // when
    val result = success(1).flatMap { success(it!! * 2) }

    // then
    assert(result.isSuccess())
    assert(result.value == 2)
    assert(result.errors().isEmpty())
  }
}