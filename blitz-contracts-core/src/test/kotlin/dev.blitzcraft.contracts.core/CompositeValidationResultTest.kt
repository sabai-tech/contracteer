package dev.blitzcraft.contracts.core

import kotlin.test.Test

class CompositeValidationResultTest {

  @Test
  fun `CompositeValidationResult is success when all child are success`() {
    // when
    val validationResult = CompositeValidationResult(listOf(
      SimpleValidationResult(propertyName = "prop1"),
      SimpleValidationResult(propertyName = "prop2"))
    )

    // expect
    assert(validationResult.isSuccess())
  }

  @Test
  fun `CompositeValidationResult is failure when one child is failure`() {
    // when
    val validationResult = CompositeValidationResult(listOf(
      SimpleValidationResult("prop1"),
      SimpleValidationResult("prop2", "Wrong Type"))
    )

    // expect
    assert(validationResult.isSuccess().not())
  }

  @Test
  fun `Error messages contains full property path and error description separated by colon`() {
    // given
    val validationResult = CompositeValidationResult("user", listOf(
      SimpleValidationResult("id", "error 1"),
      CompositeValidationResult("products", listOf(
        SimpleValidationResult("[2]", "error 2")))))

    // when
    val errors = validationResult.errors()

    // then
    assert(errors.size == 2)
    assert(errors.all { it.startsWith("user.id") || it.startsWith("user.products[2]") })
  }

  @Test
  fun `Error messages does not contain succeeded validation`() {
    // given
    val validationResult = CompositeValidationResult(listOf(
      SimpleValidationResult(),
      SimpleValidationResult("error")
    ))

    // when
    val errors = validationResult.errors()

    // then
    assert(errors.size == 1)
  }
}