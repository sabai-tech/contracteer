package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.TestFixture.numberDataType
import tech.sabai.contracteer.core.normalize

class NumberDataTypeTest {

  @Test
  fun `validation succeeds for a decimal value`() {
    // given
    val numberDataType = numberDataType()

    // when
    val result = numberDataType.validate(123.45)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validation succeeds  an integer value`() {
    // given
    val numberDataType = numberDataType()

    // when
    val result = numberDataType.validate(123)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validation fails for a value which is not a number`() {
    // given
    val numberDataType = numberDataType()

    // when
    val result = numberDataType.validate(true)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `validation succeeds for a null value when nullable`() {
    // given
    val numberDataType = numberDataType(isNullable = true)

    // when
    val result = numberDataType.validate(null)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validation fails for a null value when not nullable`() {
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
    fun `validation succeeds when the value is included in the enum`() {
      // given
      val numberDataType = numberDataType(enum = listOf(1.1.toBigDecimal(), 2.2.toBigDecimal()))

      // when
      val result = numberDataType.validate(1.1)

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `validation fails when the value is not included in the enum`() {
      // given
      val numberDataType = numberDataType(enum = listOf(1.toBigDecimal(), 2.toBigDecimal()))

      // when
      val result = numberDataType.validate(3)

      // then
      assert(result.isFailure())
    }

    @Test
    fun `generates valid random value with enum`() {
      // given
      val enum = listOf(1.toBigDecimal(), 2.toBigDecimal())
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
    fun `validation succeeds when the value is within the range`() {
      // given
      val numberDataType = numberDataType(minimum = 10.toBigDecimal(), maximum = 20.toBigDecimal())

      // when
      val result = numberDataType.validate(11)

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `validation fails when the value is outside of the range`() {
      // given
      val numberDataType = numberDataType(minimum = 10.toBigDecimal(), maximum = 20.toBigDecimal())

      // when
      val result = numberDataType.validate(30)

      // then
      assert(result.isFailure())
    }

    @Test
    fun `generates a valid random value within the range`() {
      // given
      val numberDataType = numberDataType(minimum = 10.toBigDecimal(), maximum = 20.toBigDecimal())

      // when
      val result = numberDataType.randomValue()

      // then
      assert(Range.create(10.toBigDecimal(), 20.toBigDecimal()).value!!.contains(result).isSuccess())
    }
  }
}