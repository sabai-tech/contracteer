package dev.blitzcraft.contracts.core.datatype

import org.junit.jupiter.api.Test

class EmailDataTypeTest {

  @Test
  fun `validates a value of type string representing an email address`() {
    // given
    val emailDataType = EmailDataType()

    // when
    val result = emailDataType.validate("john@example.com")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate string value which does not represent an email address`() {
    // given
    val emailDataType = EmailDataType()

    // when
    val result = emailDataType.validate("john doe @example")

    // then
    assert(result.isFailure())
  }
  @Test
  fun `validates null value if it is nullable`() {
    // given
    val emailDataType = EmailDataType(isNullable = true)

    // when
    val result = emailDataType.validate(null)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate null value if it is not nullable`() {
    // given
    val emailDataType = EmailDataType(isNullable = false)

    // when
    val result = emailDataType.validate(null)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `should generate a string representing an email address`() {
    // given
    val emailDataType = EmailDataType()

    // when
    val randomEmail = emailDataType.randomValue()

    // then
    assert(emailDataType.validate(randomEmail).isSuccess())
  }
}