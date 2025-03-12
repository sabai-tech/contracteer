package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.TestFixture.base64DataType

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
    val base64DataType = base64DataType()

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
  fun `generates a random string encoded in base64`() {
    // given
    val base64DataType = base64DataType()

    // when
    val randomBase64 = base64DataType.randomValue()
    println(randomBase64)

    // then
    assert(base64DataType.validate(randomBase64).isSuccess())
  }

  @Nested
  inner class WithEnum {

    @Test
    fun `does not create when enum length is not in the length range`() {
      // when
      val result = Base64DataType.create(minLength = 1, maxLength = 2, enum = listOf("Sm9obiBEb2U=", "Az9obiBEb4e="))

      // then
      assert(result.isFailure())
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

  @Nested
  inner class WithLengthRange {

    @Test
    fun `does not create when maxLength is negative`() {
      // when
      val result = Base64DataType.create(maxLength = -1)

      // then
      assert(result.isFailure())
    }

    @Test
    fun `does not create when minLength is less than 4`() {
      // when
      val result = Base64DataType.create(minLength = 3)

      // then
      assert(result.isFailure())
    }

    @Test
    fun `does not create when minLength is not a multiple of 4`() {
      // when
      val result = Base64DataType.create(minLength = 13)

      // then
      assert(result.isFailure())
    }

    @Test
    fun `does not create when maxLength is not a multiple of 4`() {
      // when
      val result = Base64DataType.create(maxLength = 11)

      // then
      assert(result.isFailure())
    }

    @Test
    fun `does not validate when value length is not in the range`() {
      // given
      val base64DataType = base64DataType(minLength = 4, maxLength = 8)

      // when
      val result = base64DataType.validate("SGVsbG8gV29ybGQgIQ==")

      // then
      assert(result.isFailure())
    }

    @Test
    fun `validates when value length is in the range`() {
      // given
      val base64DataType = base64DataType(maxLength = 32)

      // when
      val result = base64DataType.validate("SGVsbG8gV29ybGQgIQ==")

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `generates random value with length inside the range`() {
      // given
      val base64DataType = base64DataType(minLength = 12, maxLength = 16)

      // when
      val result = base64DataType.randomValue()

      // then
      assert(
        Range.create(12.toBigDecimal(), 16.toBigDecimal()).value!!.contains(result.length.toBigDecimal()).isSuccess()
      )
    }
  }
}
