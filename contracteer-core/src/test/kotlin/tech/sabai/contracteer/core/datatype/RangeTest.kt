package tech.sabai.contracteer.core.datatype

import java.math.BigDecimal
import kotlin.test.Test

class RangeTest {


  @Test
  fun `does not create when minimum is greater than maximum`() {
    // when
    val result = Range.create(bigDecimal(20), bigDecimal(10))

    // then
    assert(result.isFailure())
  }

  @Test
  fun `does not create when range is defined by one value and one bound is exclusive`() {
    // when
    val result = Range.create(bigDecimal(10), bigDecimal(10), exclusiveMinimum = true)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `closed range`() {
    val range = Range.create(bigDecimal(10), bigDecimal(20)).value!!

    assert(range.contains(bigDecimal(10)).isSuccess())
    assert(range.contains(bigDecimal(20)).isSuccess())
    assert(range.contains(bigDecimal(11.799)).isSuccess())
    assert(range.contains(bigDecimal(15)).isSuccess())
    assert(range.contains(bigDecimal(9)).isFailure())
    assert(range.contains(bigDecimal(21)).isFailure())
  }

  @Test
  fun `opened range`() {
    val range = Range.create(bigDecimal(10), bigDecimal(20), exclusiveMinimum = true, exclusiveMaximum = true).value!!

    assert(range.contains(bigDecimal(10)).isFailure())
    assert(range.contains(bigDecimal(20)).isFailure())
    assert(range.contains(bigDecimal(11.799)).isSuccess())
    assert(range.contains(bigDecimal(15)).isSuccess())
    assert(range.contains(bigDecimal(9)).isFailure())
    assert(range.contains(bigDecimal(21)).isFailure())
  }

  @Test
  fun `range with infinite lower bound`() {
    val range = Range.create(minimum = bigDecimal(10)).value!!

    assert(range.contains(bigDecimal(10)).isSuccess())
    assert(range.contains(bigDecimal(20)).isSuccess())
    assert(range.contains(bigDecimal(10_000_000)).isSuccess())
    assert(range.contains(bigDecimal(12.799)).isSuccess())
    assert(range.contains(bigDecimal(9)).isFailure())
    assert(range.contains(bigDecimal(21)).isSuccess())
  }

  @Test
  fun `range with infinite upper bound`() {
    val range = Range.create(maximum = bigDecimal(10)).value!!

    assert(range.contains(bigDecimal(10)).isSuccess())
    assert(range.contains(bigDecimal(20)).isFailure())
    assert(range.contains(bigDecimal(10_000_000)).isFailure())
    assert(range.contains(bigDecimal(-10_000_000.1)).isSuccess())
    assert(range.contains(bigDecimal(9)).isSuccess())
  }

  @Test
  fun `infinite range`() {
    val range = Range.create().value!!

    assert(range.contains(bigDecimal(10)).isSuccess())
    assert(range.contains(bigDecimal(20)).isSuccess())
    assert(range.contains(bigDecimal(10_000_000)).isSuccess())
    assert(range.contains(bigDecimal(-10_000_000.1)).isSuccess())
    assert(range.contains(bigDecimal(9)).isSuccess())
  }

  @Test
  fun `does not contain integer value `() {
    // given
    val range = Range.create(bigDecimal(11.3), bigDecimal(11.4)).value!!

    // when
    val containsAtLeastOneInteger = range.containsIntegers()

    // then
    assert(containsAtLeastOneInteger.not())
  }

  @Test
  fun `generates random integer value in the range`() {
    // given
    val range = Range.create(10.toBigDecimal(), 20.toBigDecimal()).value!!

    // when
    val value = range.randomIntegerValue()

    //
    assert(range.contains(value).isSuccess())
  }

  @Test
  fun `generates random value in the range`() {
    // given
    val range = Range.create(10.toBigDecimal(), 20.toBigDecimal()).value!!

    // when
    val value = range.randomValue()

    //
    assert(range.contains(value).isSuccess())
  }


  private fun bigDecimal(value: Long) = BigDecimal.valueOf(value)
  private fun bigDecimal(value: Double) = BigDecimal.valueOf(value)
}