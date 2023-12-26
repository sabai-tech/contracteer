package dev.blitzcraft.contracts.core.datatype

import kotlin.test.Test

class BooleanDataTypeTest {
  @Test
  fun `validates a value of type Boolean`() {
    // given
    val booleanDataType = BooleanDataType()

    // when
    val result = booleanDataType.validateValue(false)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate value whose type is not Boolean`() {
    // given
    val booleanDataType = BooleanDataType()

    // when
    val result = booleanDataType.validateValue(12)

    // then
    assert(result.isSuccess().not())
  }

  @Test
  fun `parses and validates a string representation of a boolean`() {
    // given
    val booleanDataType = BooleanDataType()

    // when
    val result = booleanDataType.parseAndValidate("false")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not parse a string representing something else than a boolean`() {
    // given
    val booleanDataType = BooleanDataType()

    // when
    val result = booleanDataType.parseAndValidate("12")

    // then
    assert(result.isSuccess().not())
  }
}