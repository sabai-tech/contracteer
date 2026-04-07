package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.assertSuccess
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.TestFixture.integerDataType
import tech.sabai.contracteer.core.normalize
import java.math.BigDecimal

class IntegerDataTypeTest {

  @Test
  fun `validation succeeds when a value is of type Integer`() {
    // given
    val integerDataType = integerDataType()

    // when
    val result = integerDataType.validate(123)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validation succeeds when the value is a floating point with zero decimal part`() {
    // given
    val integerDataType = integerDataType()

    // when
    val result = integerDataType.validate(1.0)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validation fails when the value is a floating point with a non-zero decimal part`() {
    // given
    val integerDataType = integerDataType()

    // when
    val result = integerDataType.validate(1.1)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `validation fails when the value is not a number`() {
    // given
    val integerDataType = integerDataType()

    // when
    val result = integerDataType.validate(true)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `validation succeeds for a null value when nullable`() {
    // given
    val integerDataType = integerDataType(isNullable = true)

    // when
    val result = integerDataType.validate(null)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validation fails for a null value when not nullable`() {
    // given
    val integerDataType = integerDataType(isNullable = false)

    // when
    val result = integerDataType.validate(null)

    // then
    assert(result.isFailure())
  }

  @Nested
  inner class WithEnum {

    @Test
    fun `validation succeeds when the value is included in the enum`() {
      // given
      val integerDataType = integerDataType(enum = listOf(1.toBigDecimal(), 2.toBigDecimal()))

      // when
      val result = integerDataType.validate(1)

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `validation fails when the value is not included in the enum`() {
      // given
      val integerDataType = integerDataType(enum = listOf(1.toBigDecimal(), 2.toBigDecimal()))

      // when
      val result = integerDataType.validate(3)

      // then
      assert(result.isFailure())
    }

    @Test
    fun `generates valid random value with enum`() {
      // given
      val enum = listOf(1.toBigDecimal(), 2.toBigDecimal())
      val integerDataType = integerDataType(enum = enum)

      // when
      val result = integerDataType.randomValue()!!

      // then
      assert(enum.map { it.normalize() }.contains(result))
    }
  }

  @Nested
  inner class WithRange {

    @Test
    fun `creation fails when range boundaries are non-integer`() {
      // when
      val result = IntegerDataType.create(name = "integer", minimum = 10.1.toBigDecimal(), maximum = 11.2.toBigDecimal())

      // then
      assert(result.isFailure())
    }

    @Test
    fun `validation succeeds when the value is within the range`() {
      // given
      val integerDataType = integerDataType(minimum = 10.toBigDecimal(), maximum = 20.toBigDecimal())

      // when
      val result = integerDataType.validate(11)

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `validation fails when the value is outside of the range`() {
      // given
      val integerDataType = integerDataType(minimum = 10.toBigDecimal(), maximum = 20.toBigDecimal())

      // when
      val result = integerDataType.validate(30)

      // then
      assert(result.isFailure())
    }

    @Test
    fun `generates a valid random value within the range`() {
      // given
      val integerDataType = integerDataType(minimum = 10.toBigDecimal(), maximum = 20.toBigDecimal())

      // when
      val result = integerDataType.randomValue()!!

      // then
      assert(Range.create(10.toBigDecimal(), 20.toBigDecimal()).assertSuccess().contains(result).isSuccess())
    }

    @Test
    fun `validation rejects value outside int32 range`() {
      // given
      val integerDataType = integerDataType(
        minimum = Int.MIN_VALUE.toBigDecimal(),
        maximum = Int.MAX_VALUE.toBigDecimal()
      )

      // when
      val result = integerDataType.validate(3_000_000_000L)

      // then
      assert(result.isFailure())
    }

    @Test
    fun `validation accepts value within int32 range`() {
      // given
      val integerDataType = integerDataType(
        minimum = Int.MIN_VALUE.toBigDecimal(),
        maximum = Int.MAX_VALUE.toBigDecimal()
      )

      // when
      val result = integerDataType.validate(2_000_000_000)

      // then
      assert(result.isSuccess())
    }
  }

  @Nested
  inner class WithMultipleOf {

    @Test
    fun `creation fails when range contains no multiples`() {
      // when
      val result = IntegerDataType.create(
        name = "integer",
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
      val integerDataType = integerDataType(multipleOf = 5.toBigDecimal())

      // when
      val result = integerDataType.validate(15)

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `validation fails when value is not a multiple`() {
      // given
      val integerDataType = integerDataType(multipleOf = 5.toBigDecimal())

      // when
      val result = integerDataType.validate(13)

      // then
      assert(result.isFailure())
    }

    @Test
    fun `generates random value that is a multiple`() {
      // given
      val integerDataType = integerDataType(
        minimum = 0.toBigDecimal(),
        maximum = 100.toBigDecimal(),
        multipleOf = 7.toBigDecimal()
      )

      // when
      val result = integerDataType.randomValue()!!

      // then
      assert(result.remainder(7.toBigDecimal()).compareTo(BigDecimal.ZERO) == 0)
    }
  }
}