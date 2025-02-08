package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Test

class Base64DataTypeTest {

  @Test
  fun `validates base64 encoded string`() {
    // given
    val base64DataType = Base64DataType()

    // when
    val result = base64DataType.validate("Sm9obiBEb2U=")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate string value which is not base64 encoded`() {
    // given
    val base64DataType = Base64DataType()

    // when
    val result = base64DataType.validate("Hello1234")

    // then
    assert(result.isFailure())
  }
  @Test
  fun `validates null value if it is nullable`() {
    // given
    val base64DataType = Base64DataType(isNullable = true)

    // when
    val result = base64DataType.validate(null)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate null value if it is not nullable`() {
    // given
    val base64DataType = Base64DataType(isNullable = false)

    // when
    val result = base64DataType.validate(null)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `should generate string representing base64 data`() {
    // given
    val base64DataType = Base64DataType()

    // when
    val randomBase64 = base64DataType.randomValue()

    // then
    assert(base64DataType.validate(randomBase64).isSuccess())
  }
}