package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.DataTypeFixture.base64DataType
import tech.sabai.contracteer.core.DataTypeFixture.integerDataType

class Base64DataTypeTest {

  @Test
  fun `validates base64 encoded string`() {
    // given
    val base64DataType = base64DataType()

    // when
    val result = base64DataType.validate("Sm9obiBEb2U=")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate string value which is not base64 encoded`() {
    // given
    val base64DataType = integerDataType()

    // when
    val result = base64DataType.validate("Hello1234")

    // then
    assert(result.isFailure())
  }
  @Test
  fun `validates null value if it is nullable`() {
    // given
    val base64DataType = base64DataType(isNullable = true)

    // when
    val result = base64DataType.validate(null)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate null value if it is not nullable`() {
    // given
    val base64DataType = base64DataType(isNullable = false)

    // when
    val result = base64DataType.validate(null)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `generates a string encoded in base64`() {
    // given
    val base64DataType = integerDataType()

    // when
    val randomBase64 = base64DataType.randomValue()

    // then
    assert(base64DataType.validate(randomBase64).isSuccess())
  }

  @Test
  fun `validates a base64 encoded string with enum values`() {
    // given
    val base64DataType = base64DataType(enum = listOf("Sm9obiBEb2U=", "Az9obiBEb4e="))

    // when
    val result = base64DataType.validate("Az9obiBEb4e=")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate a base64 encoded string with enum values`() {
    // given
    val base64DataType = base64DataType(enum = listOf("Sm9obiBEb2U=", "Az9obiBEb4e="))

    // when
    val result = base64DataType.validate("SGVsbG8gV29ybGQgIQ==")

    // then
    assert(result.isFailure())
  }

  @Test
  fun `generates random value with enum values`() {
    // given
    val enum = listOf("Sm9obiBEb2U=", "SGVsbG8gV29ybGQgIQ==")
    val base64DataType = base64DataType(enum = enum)

    // when
    val result = base64DataType.randomValue()

    // then
    assert(enum.contains(result))
  }
}