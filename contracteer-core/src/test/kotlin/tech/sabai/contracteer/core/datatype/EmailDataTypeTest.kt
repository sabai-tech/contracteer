package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.DataTypeFixture.emailDataType

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

  @Test
  fun `validates a string representing an email with enum values`() {
    // given
    val emailDataType = emailDataType(enum = listOf("john@example.com", "ane@example.com"))

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