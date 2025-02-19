package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.DataTypeFixture.integerDataType
import tech.sabai.contracteer.core.normalize

class IntegerDataTypeTest {

  @Test
  fun `validates a value of type Integer`() {
    // given
    val integerDataType = integerDataType()

    // when
    val result = integerDataType.validate(123)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validates a floating point value when decimal part equals to zero`() {
    // given
    val integerDataType = integerDataType()

    // when
    val result = integerDataType.validate(1.0)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate a floating point value when decimal is not equal to zero`() {
    // given
    val integerDataType = integerDataType()

    // when
    val result = integerDataType.validate(1.1)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `does not validate value which is not a number`() {
    // given
    val integerDataType = integerDataType()

    // when
    val result = integerDataType.validate(true)

    // then
    assert(result.isFailure())
  }
  @Test
  fun `validates null value if it is nullable`() {
    // given
    val integerDataType = integerDataType(isNullable = true)

    // when
    val result = integerDataType.validate(null)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate null value if it is not nullable`() {
    // given
    val integerDataType = integerDataType(isNullable = false)

    // when
    val result = integerDataType.validate(null)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `validates a integer value with enum values`() {
    // given
    val integerDataType = integerDataType(enum = listOf(1, 2))

    // when
    val result = integerDataType.validate(1)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate an integer value  with enum values`() {
    // given
    val integerDataType = integerDataType(enum = listOf(1, 2))

    // when
    val result = integerDataType.validate(3)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `generates random value with enum values`() {
    // given
    val enum = listOf(1, 2)
    val dateDataType = integerDataType(enum = enum)

    // when
    val result = dateDataType.randomValue()

    // then
    assert(enum.map { it.normalize() }.contains(result))
  }
}