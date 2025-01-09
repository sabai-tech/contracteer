package dev.blitzcraft.contracts.core.datatype

import org.junit.jupiter.api.Test

class BooleanDataTypeTest {
  @Test
  fun `validates value whose type is Boolean`() {
    // given
    val booleanDataType = BooleanDataType()

    // when
    val result = booleanDataType.validate(false)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate value whose type is not Boolean`() {
    // given
    val booleanDataType = BooleanDataType()

    // when
    val result = booleanDataType.validate(12)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `validates null value if it is nullable`() {
    // given
    val booleanDataType = BooleanDataType(isNullable = true)

    // when
    val result = booleanDataType.validate(null)

    // then
    assert(result.isSuccess())
  }


  @Test
  fun `does not validate null value if it is not nullable`() {
     // given
    val booleanDataType = BooleanDataType(isNullable = false)

     // when
    val result = booleanDataType.validate(null)

     // then
    assert(result.isFailure())
  }
}

