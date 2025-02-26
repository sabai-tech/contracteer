package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.DataTypeFixture.numberDataType
import tech.sabai.contracteer.core.normalize

class NumberDataTypeTest {

  @Test
  fun `validates a decimal value`() {
    // given
    val numberDataType = numberDataType()

    // when
    val result = numberDataType.validate(123.45)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validates an integer value`() {
    // given
    val numberDataType = numberDataType()

    // when
    val result = numberDataType.validate(123)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate value which is not a number`() {
    // given
    val numberDataType = numberDataType()

    // when
    val result = numberDataType.validate(true)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `validates null value if it is nullable`() {
    // given
    val numberDataType = numberDataType(isNullable = true)

    // when
    val result = numberDataType.validate(null)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate null value if it is not nullable`() {
    // given
    val numberDataType = numberDataType(isNullable = false)

    // when
    val result = numberDataType.validate(null)

    // then
    assert(result.isFailure())
  }


  @Nested
  inner class WithEnum {
    @Test
    fun `validates a number value with enum values`() {
      // given
      val numberDataType = numberDataType(enum = listOf(1.1, 2.2))

      // when
      val result = numberDataType.validate(1.1)

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `does not validate a number with enum values`() {
      // given
      val numberDataType = numberDataType(enum = listOf(1, 2))

      // when
      val result = numberDataType.validate(3)

      // then
      assert(result.isFailure())
    }

    @Test
    fun `generates random value with enum values`() {
      // given
      val enum = listOf(1, 2)
      val dateDataType = numberDataType(enum = enum)

      // when
      val result = dateDataType.randomValue()

      // then
      assert(enum.map { it.normalize() }.contains(result))
    }
  }

  @Nested
  inner class WithRange {

    @Test
    fun `validates a value inside the range`() {
      // given
      val numberDataType = numberDataType(minimum = 10.toBigDecimal(), maximum = 20.toBigDecimal())

      // when
      val result = numberDataType.validate(11)

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `does not validate a value outside the range`() {
      // given
      val numberDataType = numberDataType(minimum = 10.toBigDecimal(), maximum = 20.toBigDecimal())

      // when
      val result = numberDataType.validate(30)

      // then
      assert(result.isFailure())
    }

    @Test
    fun `generates a random value inside the range`() {
      // given
      val numberDataType = numberDataType(minimum = 10.toBigDecimal(), maximum = 20.toBigDecimal())

      // when
      val result = numberDataType.randomValue()

      // then
      assert(Range.create(10.toBigDecimal(), 20.toBigDecimal()).value!!.contains(result).isSuccess())
    }
  }
}