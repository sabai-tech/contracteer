package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.DataTypeFixture.stringDataType

class StringDataTypeTest {

  @Test
  fun `validates a value of type string`() {
    // given
    val stringDataType = stringDataType()

    // when
    val result = stringDataType.validate("john doe")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate value whose type is not string`() {
    // given
    val stringDataType = stringDataType()

    // when
    val result = stringDataType.validate(true)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `validates null value if it is nullable`() {
    // given
    val stringDataType = stringDataType(isNullable = true)

    // when
    val result = stringDataType.validate(null)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate null value if it is not nullable`() {
    // given
    val stringDataType = stringDataType(isNullable = false)

    // when
    val result = stringDataType.validate(null)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `validates a string with enum values`() {
    // given
    val stringDataType = stringDataType(enum = listOf("Hello", "World"))

    // when
    val result = stringDataType.validate("World")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate a string with enum values`() {
    // given
    val stringDataType = stringDataType(enum = listOf("Hello", "World"))

    // when
    val result = stringDataType.validate("John")

    // then
    assert(result.isFailure())
  }

  @Test
  fun `generates random value with enum values`() {
    // given
    val enum = listOf("Hello", "World")
    val stringDataType = stringDataType(enum = enum)

    // when
    val result = stringDataType.randomValue()

    // then
    assert(enum.contains(result))
  }

}