package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.DataTypeFixture.booleanDataType

class BooleanDataTypeTest {
  @Test
  fun `validates value whose type is Boolean`() {
    // given
    val booleanDataType = booleanDataType()

    // when
    val result = booleanDataType.validate(false)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate value whose type is not Boolean`() {
    // given
    val booleanDataType = booleanDataType()

    // when
    val result = booleanDataType.validate(12)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `validates null value if it is nullable`() {
    // given
    val booleanDataType = booleanDataType(isNullable = true)

    // when
    val result = booleanDataType.validate(null)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate null value if it is not nullable`() {
     // given
    val booleanDataType = booleanDataType(isNullable = false)

     // when
    val result = booleanDataType.validate(null)

     // then
    assert(result.isFailure())
  }

  @Test
  fun `validates a boolean with enum values`() {
    // given
    val booleanDataType = booleanDataType(enum = listOf(true))

    // when
    val result = booleanDataType.validate(true)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate a boolean with enum values`() {
    // given
    val booleanDataType = booleanDataType(enum = listOf(true))

    // when
    val result = booleanDataType.validate(false)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `generates random value with enum values`() {
    // given
    val enum = listOf(true)
    val booleanDataType = booleanDataType(enum = enum)

    // when
    val result = booleanDataType.randomValue()

    // then
    assert(result)
  }
}

