package dev.blitzcraft.contracts.core.datatype

import org.junit.jupiter.api.Test

class NumberDataTypeTest {

  @Test
  fun `validates a decimal value`() {
    // given
    val decimalDataType = NumberDataType()

    // when
    val result = decimalDataType.validate(123.45)

    // then
    assert(result.isSuccess())
  }
  @Test
  fun `validates an integer value`() {
    // given
    val decimalDataType = NumberDataType()

    // when
    val result = decimalDataType.validate(123)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate value which is not a number`() {
    // given
    val decimalDataType = NumberDataType()

    // when
    val result = decimalDataType.validate(true)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `validates null value if it is nullable`() {
    // given
    val decimalDataType = NumberDataType(isNullable = true)

    // when
    val result = decimalDataType.validate(null)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate null value if it is not nullable`() {
    // given
    val decimalDataType = NumberDataType(isNullable = false)

    // when
    val result = decimalDataType.validate(null)

    // then
    assert(result.isFailure())
  }
}