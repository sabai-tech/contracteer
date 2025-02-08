package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Test

class DateDataTypeTest {

  @Test
  fun `validates a value of type string representing a date`() {
    // given
    val dateDateType = DateDataType()

    // when
    val result = dateDateType.validate("2024-12-20")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate string value which does not represent a date`() {
    // given
    val dateDateType = DateDataType()

    // when
    val result = dateDateType.validate("john doe")

    // then
    assert(result.isFailure())
  }

  @Test
  fun `validates null value if it is nullable`() {
    // given
    val dateDateType = DateDataType(isNullable = true)

    // when
    val result = dateDateType.validate(null)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate null value if it is not nullable`() {
    // given
    val dateDateType = DateDataType(isNullable = false)

    // when
    val result = dateDateType.validate(null)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `should generate a string representing a date`() {
    // given
    val dateDateType = DateDataType()

    // when
    val randomDate = dateDateType.randomValue()

    // then
    assert(dateDateType.validate(randomDate).isSuccess())
  }
}