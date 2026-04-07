package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.assertSuccess
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.TestFixture.stringDataType

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

  @Nested
  inner class WithEnum {
    @Test
    fun `does not create when enum length is not in the length range`() {
      // when
      val result = StringDataType.create(
        name = "string",
        openApiType = "string",
        minLength = 1,
        maxLength = 2,
        enum = listOf("ABC", "DEF"))

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
      val result = stringDataType.randomValue()!!

      // then
      assert(enum.contains(result))
    }
  }

  @Nested
  inner class WithPattern {

    @Test
    fun `validates a string matching the pattern`() {
      // given
      val dataType = stringDataType(pattern = "^[A-Z]{2}-\\d{4}$")

      // when
      val result = dataType.validate("AB-1234")

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `does not validate a string not matching the pattern`() {
      // given
      val dataType = stringDataType(pattern = "^[A-Z]{2}-\\d{4}$")

      // when
      val result = dataType.validate("invalid")

      // then
      assert(result.isFailure())
    }

    @Test
    fun `generates random value matching the pattern`() {
      // given
      val dataType = stringDataType(pattern = "^[A-Z]{2}-\\d{4}$")

      // when
      val value = dataType.randomValue()!!

      // then
      assert(Regex("^[A-Z]{2}-\\d{4}$").matches(value))
    }

    @Test
    fun `pattern takes precedence over length constraints for validation`() {
      // given
      val dataType = stringDataType(pattern = "^[A-Z]{2}$", minLength = 5, maxLength = 10)

      // when
      val result = dataType.validate("AB")

      // then — pattern matches, length ignored
      assert(result.isSuccess())
    }

    @Test
    fun `pattern takes precedence over length constraints for generation`() {
      // given
      val dataType = stringDataType(pattern = "^\\d{3}$", minLength = 10)

      // when
      val value = dataType.randomValue()!!

      // then — generated from pattern, not length
      assert(Regex("^\\d{3}$").matches(value))
    }

    @Test
    fun `creation fails when pattern is not a valid regex`() {
      // when
      val result = StringDataType.create(
        name = "string",
        openApiType = "string",
        pattern = "[invalid(")

      // then
      assert(result.isFailure())
    }

    @Test
    fun `enum values are validated against pattern at creation`() {
      // when
      val result = StringDataType.create(
        name = "string",
        openApiType = "string",
        pattern = "^[A-Z]+$",
        enum = listOf("ACTIVE", "inactive"))

      // then — "inactive" fails pattern validation
      assert(result.isFailure())
    }

    @Test
    fun `enum takes precedence over pattern for generation`() {
      // given
      val enum = listOf("AB-1234", "CD-5678")
      val dataType = stringDataType(pattern = "^[A-Z]{2}-\\d{4}$", enum = enum)

      // when
      val value = dataType.randomValue()!!

      // then
      assert(enum.contains(value))
    }
  }

  @Nested
  inner class WithLengthRange {

    @Test
    fun `does not create when maxLength is negative`() {
      // when
      val result = StringDataType.create(name = "string", openApiType = "string", maxLength = -1)

      // then
      assert(result.isFailure())
    }

    @Test
    fun `does not validate when value length is not in the range`() {
      // given
      val stringDataType = stringDataType(minLength = 1, maxLength = 5)

      // when
      val result = stringDataType.validate("Hello World !")

      // then
      assert(result.isFailure())
    }

    @Test
    fun `validates when value length is in the range`() {
      // given
      val stringDataType = stringDataType(minLength = 1, maxLength = 15)

      // when
      val result = stringDataType.validate("Hello World !")

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `generates random value with length inside the range`() {
      // given
      val stringDataType = stringDataType(minLength = 1, maxLength = 5)

      // when
      val result = stringDataType.randomValue()!!

      // then
      assert(
        Range.create(1.toBigDecimal(), 5.toBigDecimal()).assertSuccess().contains(result.length.toBigDecimal()).isSuccess()
      )
    }

    @Test
    fun `generates random value respecting maxLength when minLength is not set`() {
      // given
      val stringDataType = stringDataType(maxLength = 1)

      // when
      val result = stringDataType.randomValue()!!

      // then
      assert(result.length <= 1)
    }

    @Test
    fun `generates random value respecting minLength greater than 10`() {
      // given
      val stringDataType = stringDataType(minLength = 15, maxLength = 20)

      // when
      val result = stringDataType.randomValue()!!

      // then
      assert(result.length in 15..20)
    }
  }
}
