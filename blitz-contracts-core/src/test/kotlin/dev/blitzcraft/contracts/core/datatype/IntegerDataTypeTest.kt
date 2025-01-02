package dev.blitzcraft.contracts.core.datatype

import org.junit.jupiter.api.Test

class IntegerDataTypeTest {

  @Test
  fun `validates a value of type Integer`() {
    // given
    val integerDataType = IntegerDataType()

    // when
    val result = integerDataType.validate(123)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validates a floating point value when decimal part equals to zero`() {
    // given
    val integerDataType = IntegerDataType()

    // when
    val result = integerDataType.validate(1.0)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate a floating point value when decimal is not equal to zero`() {
    // given
    val integerDataType = IntegerDataType()

    // when
    val result = integerDataType.validate(1.1)

    // then
    assert(result.isSuccess().not())
  }

  @Test
  fun `does not validate value which is not a number`() {
    // given
    val integerDataType = IntegerDataType()

    // when
    val result = integerDataType.validate(true)

    // then
    assert(result.isSuccess().not())
  }
  @Test
  fun `validates null value if it is nullable`() {
    // given
    val integerDataType = IntegerDataType(isNullable = true)

    // when
    val result = integerDataType.validate(null)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate null value if it is not nullable`() {
    // given
    val integerDataType = IntegerDataType(isNullable = false)

    // when
    val result = integerDataType.validate(null)

    // then
    assert(result.isSuccess().not())
  }
}