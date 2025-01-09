package dev.blitzcraft.contracts.core.validation

import dev.blitzcraft.contracts.core.Result.Companion.failure
import dev.blitzcraft.contracts.core.Result.Companion.success
import org.junit.jupiter.api.Test

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
  fun `combining 2 success of different type is a success with null value`() {
    // when
    val result = success(1) combineWith success("John")

    // expect
    assert(result.isSuccess())
    assert(result.value == null)
  }

  @Test
  fun `combining a success with an error is an error`() {
    // when
    val result = success(1) combineWith failure<Any>("Wrong Type")

    // then
    assert(result.isFailure())
    assert(result.errors().size == 1)
    assert(result.errors().first() == "Wrong Type")
    assert(result.value == null)
  }

  @Test
  fun `combining errors is an error`() {
    // when
    val result = failure<Any>("Wrong Type1") combineWith failure<Any>("Wrong Type2") combineWith failure<Any>("Wrong Type3")

    // then
    assert(result.isFailure())
    assert(result.errors().size == 3)
    assert(result.errors().containsAll(listOf("Wrong Type1", "Wrong Type2", "Wrong Type3")))
  }

  @Test
  fun `combining errors do not modify property name`() {
    // when
    val result = failure<Any>("prop1", "Wrong Type1") combineWith failure<Any>("prop2", "Wrong Type2") combineWith failure<Any>("prop3", "Wrong Type3")

    // then
    assert(result.errors().containsAll(listOf("'prop1': Wrong Type1", "'prop2': Wrong Type2", "'prop3': Wrong Type3")))
  }

  @Test
  fun `all error message are prepended with property name when adding it`() {
    // when
    val result = (failure<Any>("prop1", "Wrong Type") combineWith failure<Any>("prop2", "Wrong Type")).forProperty("parent")

    // then
    assert(result.errors().containsAll(listOf("'parent.prop1': Wrong Type", "'parent.prop2': Wrong Type")))
  }

  @Test
  fun `error message is prepended with property index when adding it`() {
    // when
    val result = failure<Any>("prop1", "Wrong Type").forIndex(1)

    // then
    assert(result.errors().first() == "'[1].prop1': Wrong Type")
  }

  @Test
  fun `map value when it is a success`() {
    // when
    val result = success(1).map { it + 1 }

    // then
    assert(result.isSuccess())
    assert(result.value == 2)
  }

  @Test
  fun `does not map when it is a failure`() {
    // when
    val result = failure<Int>("error").map { it + 1 }

    // then
    assert(result.isFailure())
    assert(result.value == null)
    assert(result.errors().first() == "error")
  }
}