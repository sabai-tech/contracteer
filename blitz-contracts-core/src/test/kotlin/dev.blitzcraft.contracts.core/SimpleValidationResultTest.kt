package dev.blitzcraft.contracts.core

import kotlin.test.Test

class SimpleValidationResultTest {
  @Test
  fun `Error message contains property name and error description separated by colon`() {
    // given
    val validationResult = SimpleValidationResult("prop", "Wrong Type")

    // when
    val errors = validationResult.errors()

    // then
    assert(errors.size == 1)
    assert(errors.first() == "prop: Wrong Type")
  }

  @Test
  fun `Error message contains error description separated when property name is not set`() {
    // given
    val validationResult = SimpleValidationResult("Wrong Type")

    // when
    val errors = validationResult.errors()

    // then
    assert(errors.size == 1)
    assert(errors.first() == "Wrong Type")
  }
}