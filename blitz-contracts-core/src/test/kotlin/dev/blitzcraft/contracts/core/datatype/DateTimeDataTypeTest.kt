package dev.blitzcraft.contracts.core.datatype

import org.junit.jupiter.api.Test

class DateTimeDataTypeTest {

  @Test
  fun `validates a value of type string representing a date-time`() {
    // given
    val dateTimeDateType = DateTimeDataType()

    // when
    val result = dateTimeDateType.validate("2024-12-20T15:30:45+02:00")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate string value which does not represent a date-time`() {
    // given
    val dateTimeDateType = DateTimeDataType()

    // when
    val result = dateTimeDateType.validate("john doe")

    // then
    assert(result.isFailure())
  }
  @Test
  fun `validates null value if it is nullable`() {
    // given
    val dateTimeDateType = DateTimeDataType(isNullable = true)

    // when
    val result = dateTimeDateType.validate(null)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate null value if it is not nullable`() {
    // given
    val dateTimeDateType = DateTimeDataType(isNullable = false)

    // when
    val result = dateTimeDateType.validate(null)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `should generate a string representing a date-time`() {
    // given
    val dateTimeDateType = DateTimeDataType()

    // when
    val randomDateTime = dateTimeDateType.randomValue()

    // then
    assert(dateTimeDateType.validate(randomDateTime).isSuccess())
  }
}