package dev.blitzcraft.contracts.core.datatype

import kotlin.test.Test

class DecimalDataTypeTest {

  @Test
  fun `validates a value of type Float`() {
    // given
    val decimalDataType = DecimalDataType()

    // when
    val result = decimalDataType.validateValue(1.23)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate value whose type is not Float`() {
    // given
    val decimalDataType = DecimalDataType()

    // when
    val result = decimalDataType.validateValue(12)

    // then
    assert(result.isSuccess().not())
  }

  @Test
  fun `parses and validates a string representation of a Decimal`() {
    // given
    val decimalDataType = DecimalDataType()

    // when
    val result = decimalDataType.parseAndValidate("123.456")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not parse a string representing something else than a decimal`() {
    // given
    val decimalDataType = DecimalDataType()

    // when
    val result = decimalDataType.validateValue("Hello")

    // then
    assert(result.isSuccess().not())
  }
}