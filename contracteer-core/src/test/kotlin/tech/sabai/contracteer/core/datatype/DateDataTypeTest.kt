package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.TestFixture.dateDataType

class DateDataTypeTest {

  @Test
  fun `validates a value of type string representing a date`() {
    // given
    val dateDateType = dateDataType()

    // when
    val result = dateDateType.validate("2024-12-20")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate string value which does not represent a date`() {
    // given
    val dateDateType = dateDataType()

    // when
    val result = dateDateType.validate("john doe")

    // then
    assert(result.isFailure())
  }

  @Test
  fun `validates null value if it is nullable`() {
    // given
    val dateDateType = dateDataType(isNullable = true)

    // when
    val result = dateDateType.validate(null)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate null value if it is not nullable`() {
    // given
    val dateDateType = dateDataType(isNullable = false)

    // when
    val result = dateDateType.validate(null)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `should generate a string representing a date`() {
    // given
    val dateDateType = dateDataType()

    // when
    val randomDate = dateDateType.randomValue()

    // then
    assert(dateDateType.validate(randomDate).isSuccess())
  }

  @Test
  fun `validates a string representing a date with enum values`() {
    // given
    val dateDataType = dateDataType(enum = listOf("2024-12-20", "2030-10-20"))

    // when
    val result = dateDataType.validate("2030-10-20")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate a string representing a date with enum values`() {
    // given
    val dateDataType = dateDataType(enum = listOf("2024-12-20", "2030-10-20"))

    // when
    val result = dateDataType.validate("2015-11-20")

    // then
    assert(result.isFailure())
  }

  @Test
  fun `generates random value with enum values`() {
    // given
    val enum = listOf("2024-12-20", "2030-10-20")
    val dateDataType = dateDataType(enum = enum)

    // when
    val result = dateDataType.randomValue()

    // then
    assert(enum.contains(result))
  }
}