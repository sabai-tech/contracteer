package dev.blitzcraft.contracts.core.datatype

import kotlin.test.Test

class IntegerDataTypeTest {

  @Test
  fun `validates a value of type Integer`() {
    // given
    val integerDataType = IntegerDataType()

    // when
    val result = integerDataType.validateValue(123)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate value whose type is not Integer`() {
    // given
    val integerDataType = IntegerDataType()

    // when
    val result = integerDataType.validateValue(true)

    // then
    assert(result.isSuccess().not())
  }

  @Test
  fun `parses and validates a string representation of an Integer`() {
    // given
    val integerDataType = IntegerDataType()

    // when
    val result = integerDataType.parseAndValidate("123456")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not parse a string representing something else than an Integer`() {
    // given
    val integerDataType = IntegerDataType()

    // when
    val result = integerDataType.validateValue("Hello")

    // then
    assert(result.isSuccess().not())
  }
}