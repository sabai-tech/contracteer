package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.assertSuccess
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.TestFixture.numberDataType
import tech.sabai.contracteer.core.normalize
import java.math.BigDecimal

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
  fun `validation succeeds for an integer value`() {
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
      val numberDataType = numberDataType(enum = enum)

      // when
      val result = numberDataType.randomValue()

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
      assert(Range.create(10.toBigDecimal(), 20.toBigDecimal()).assertSuccess().contains(result).isSuccess())
    }

    @Test
    fun `validation rejects value outside float range`() {
      // given
      val numberDataType = numberDataType(
        minimum = Float.MAX_VALUE.toBigDecimal().negate(),
        maximum = Float.MAX_VALUE.toBigDecimal()
      )

      // when
      val result = numberDataType.validate(Double.MAX_VALUE)

      // then
      assert(result.isFailure())
    }

    @Test
    fun `validation accepts value within float range`() {
      // given
      val numberDataType = numberDataType(
        minimum = Float.MAX_VALUE.toBigDecimal().negate(),
        maximum = Float.MAX_VALUE.toBigDecimal()
      )

      // when
      val result = numberDataType.validate(1_000_000.5)

      // then
      assert(result.isSuccess())
    }
  }

  @Nested
  inner class WithMultipleOf {

    @Test
    fun `creation fails when range contains no multiples`() {
      // when
      val result = NumberDataType.create(
        name = "number",
        minimum = 5.toBigDecimal(),
        maximum = 8.toBigDecimal(),
        multipleOf = 10.toBigDecimal()
      )

      // then
      assert(result.isFailure())
    }

    @Test
    fun `validation succeeds when value is a multiple`() {
      // given
      val numberDataType = numberDataType(multipleOf = 0.01.toBigDecimal())

      // when
      val result = numberDataType.validate(1.23)

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `validation fails when value is not a multiple`() {
      // given
      val numberDataType = numberDataType(multipleOf = 0.01.toBigDecimal())

      // when
      val result = numberDataType.validate(1.234)

      // then
      assert(result.isFailure())
    }

    @Test
    fun `generates random value that is a multiple`() {
      // given
      val numberDataType = numberDataType(
        minimum = 0.toBigDecimal(),
        maximum = 10.toBigDecimal(),
        multipleOf = 0.5.toBigDecimal()
      )

      // when
      val result = numberDataType.randomValue()

      // then
      assert(result.remainder(0.5.toBigDecimal()).compareTo(BigDecimal.ZERO) == 0)
    }
  }
}