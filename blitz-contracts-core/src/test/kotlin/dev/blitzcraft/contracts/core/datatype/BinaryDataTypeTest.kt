package dev.blitzcraft.contracts.core.datatype

import org.junit.jupiter.api.Test

class BinaryDataTypeTest {

  @Test
  fun `validates a value of type string`() {
    // given
    val binaryDataType = BinaryDataType()

    // when
    val result = binaryDataType.validate("john doe")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate value whose type is not string`() {
    // given
    val binaryDataType = BinaryDataType()

    // when
    val result = binaryDataType.validate(true)

    // then
    assert(result.isFailure())
  }
  @Test
  fun `validates null value if it is nullable`() {
    // given
    val binaryDataType = BinaryDataType(isNullable = true)

    // when
    val result = binaryDataType.validate(null)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate null value if it is not nullable`() {
    // given
    val binaryDataType = BinaryDataType(isNullable = false)

    // when
    val result = binaryDataType.validate(null)

    // then
    assert(result.isFailure())
  }
}