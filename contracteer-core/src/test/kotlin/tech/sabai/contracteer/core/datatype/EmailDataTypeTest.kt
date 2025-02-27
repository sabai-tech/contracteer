package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.DataTypeFixture.emailDataType
import tech.sabai.contracteer.core.DataTypeFixture.stringDataType

class EmailDataTypeTest {

  @Test
  fun `validates a value of type string representing an email address`() {
    // given
    val emailDataType = emailDataType()

    // when
    val result = emailDataType.validate("john@example.com")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate string value which does not represent an email address`() {
    // given
    val emailDataType = emailDataType()

    // when
    val result = emailDataType.validate("john doe @example")

    // then
    assert(result.isFailure())
  }

  @Test
  fun `validates null value if it is nullable`() {
    // given
    val emailDataType = emailDataType(isNullable = true)

    // when
    val result = emailDataType.validate(null)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate null value if it is not nullable`() {
    // given
    val emailDataType = emailDataType(isNullable = false)

    // when
    val result = emailDataType.validate(null)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `should generate a string representing an email address`() {
      // given
      val emailDataType = emailDataType()

      // when
      val randomEmail = emailDataType.randomValue()

      // then
      assert(emailDataType.validate(randomEmail).isSuccess())
  }

  @Nested
  inner class WithEnum {
    @Test
    fun `validates a string representing an email with enum values`() {
      // given
      val emailDataType = emailDataType(enum = listOf("john@example.com", "jane@example.com"))

      // when
      val result = emailDataType.validate("john@example.com")

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `does not validate a string representing an email with enum values`() {
      // given
      val emailDataType = emailDataType(enum = listOf("john@example.com", "ane@example.com"))

      // when
      val result = emailDataType.validate("john@jane.doe")

      // then
      assert(result.isFailure())
    }

    @Test
    fun `generates random value with enum values`() {
      // given
      val enum = listOf("john@example.com", "ane@example.com")
      val emailDataType = emailDataType(enum = enum)

      // when
      val result = emailDataType.randomValue()

      // then
      assert(enum.contains(result))
    }
  }

  @Nested
  inner class WithLengthRange {

    @Test
    fun `does not create when maxLength is negative`() {
      // when
      val result = EmailDataType.create(maxLength = -1)

      // then
      assert(result.isFailure())
    }

    @Test
    fun `does not create when minLength is less than 6`() {
      // when
      val result = EmailDataType.create(minLength = 3)

      // then
      assert(result.isFailure())
    }
    @Test
    fun `defaults to 6 when minLength is null`() {
      // when
      val result = EmailDataType.create(minLength = null)

      // then
      assert(result.isSuccess())
      assert(result.value!!.lengthRange.minimum == 6.toBigDecimal())
    }

    @Test
    fun `does not validate when value length is not in the range`() {
      // given
      val emailDataType = emailDataType(minLength = 6, maxLength = 7)

      // when
      val result = emailDataType.validate("john@example.com")

      // then
      assert(result.isFailure())
    }

    @Test
    fun `validates when value length is in the range`() {
      // given
      val emailDataType = emailDataType(minLength = 6, maxLength = 150)

      // when
      val result = emailDataType.validate("john@example.com")

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `generates random value with length inside the range`() {
      // given
      val stringDataType = stringDataType(minLength = 6, maxLength = 10)

      // when
      val result = stringDataType.randomValue()

      // then
      assert(
        Range.create(6.toBigDecimal(), 10.toBigDecimal()).value!!.contains(result.length.toBigDecimal()).isSuccess()
      )
    }
  }
}