package dev.blitzcraft.contracts.core.datatype

import org.junit.jupiter.api.Test

class StringDataTypeTest {

  @Test
  fun `validates a value of type string`() {
    // given
    val stringDataType = StringDataType()

    // when
    val result = stringDataType.validate("john doe")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate value whose type is not string`() {
    // given
    val stringDataType = StringDataType()

    // when
    val result = stringDataType.validate(true)

    // then
    assert(result.isFailure())
  }
  @Test
  fun `validates null value if it is nullable`() {
    // given
    val stringDataType = StringDataType(isNullable = true)

    // when
    val result = stringDataType.validate(null)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate null value if it is not nullable`() {
    // given
    val stringDataType = StringDataType(isNullable = false)

    // when
    val result = stringDataType.validate(null)

    // then
    assert(result.isFailure())
  }
}