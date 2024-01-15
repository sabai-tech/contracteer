package dev.blitzcraft.contracts.core.validation

import dev.blitzcraft.contracts.core.validation.ValidationResult.Companion.success
import dev.blitzcraft.contracts.core.validation.ValidationResult.Companion.error
import org.junit.jupiter.api.Test

class ValidationResultTest {

  @Test
  fun `Error message contains property name and error description separated by colon`() {
    // given
    val validationResult = error("prop", "Wrong Type")

    // when
    val errors = validationResult.errors()

    // then
    assert(errors.size == 1)
    assert(errors.first() == "prop: Wrong Type")
  }

  @Test
  fun `Error message contains only error description when property name is not set`() {
    // given
    val validationResult = error("Wrong Type")

    // when
    val errors = validationResult.errors()

    // then
    assert(errors.size == 1)
    assert(errors.first() == "Wrong Type")
  }

  @Test
  fun `aggregates 2 success is a success`() {
    // when
    val result = success() and success()

    // expect
    assert(result.isSuccess())
  }

  @Test
  fun `aggregates success with error is an error`() {
    // when
    val result = success() and error("Wrong Type")

    // then
    assert(result.isSuccess().not())
    assert(result.errors().size == 1)
    assert(result.errors().first() == "Wrong Type")
  }

  @Test
  fun `aggregates errors is an error`() {
    // when
    val result = error("Wrong Type1") and error("Wrong Type2") and error("Wrong Type3")

    // then
    assert(result.isSuccess().not())
    assert(result.errors().size == 3)
    assert(result.errors().containsAll(listOf("Wrong Type1", "Wrong Type2", "Wrong Type3")))
  }

  @Test
  fun `aggregating errors do not modify property name`() {
    // when
    val result = error("prop1", "Wrong Type1") and error("prop2", "Wrong Type2") and error("prop3", "Wrong Type3")

    // then
    assert(result.errors().containsAll(listOf("prop1: Wrong Type1", "prop2: Wrong Type2", "prop3: Wrong Type3")))
  }

  @Test
  fun `all error message are prepended with property name when adding it`() {
    // when
    val result = (error("prop1", "Wrong Type") and error("prop2", "Wrong Type")).forProperty("parent")

    // then
    assert(result.errors().containsAll(listOf("parent.prop1: Wrong Type", "parent.prop2: Wrong Type")))
  }

  @Test
  fun `error message is prepended with property index when adding it`() {
    // when
    val result = error("prop1", "Wrong Type").forIndex(1)

    // then
    assert(result.errors().first() == "[1].prop1: Wrong Type")
  }
}