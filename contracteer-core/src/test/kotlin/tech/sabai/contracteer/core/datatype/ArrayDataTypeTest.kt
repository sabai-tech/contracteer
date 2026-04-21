package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.dsl.arrayType
import tech.sabai.contracteer.core.dsl.booleanType
import tech.sabai.contracteer.core.dsl.integerType
import tech.sabai.contracteer.core.dsl.numberType
import tech.sabai.contracteer.core.dsl.stringType
import tech.sabai.contracteer.core.normalize
import java.math.BigDecimal

class ArrayDataTypeTest {

  @Test
  fun `validates null value if it is nullable`() {
    // given
    val arrayDataType = arrayType(items = stringType(), isNullable = true)

    // when
    val result = arrayDataType.validate(null)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate null value if it is not nullable`() {
    // given
    val arrayDataType = arrayType(items = stringType(), isNullable = false)

    // when
    val result = arrayDataType.validate(null)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `does not validate value whose type is not array`() {
    // given
    val arrayDataType = arrayType(items = stringType())

    // when
    val result = arrayDataType.validate("value")

    // then
    assert(result.isFailure())
  }

  @Test
  fun `does not validate array with wrong item type`() {
    // given
    val arrayDataType = arrayType(items = stringType())

    // when
    val result = arrayDataType.validate(listOf(1, 2, 3))

    // then
    assert(result.isFailure())
  }

  @Test
  fun `does not validate array when item type is not nullable`() {
    // given
    val arrayDataType = arrayType(items = stringType(isNullable = false))

    // when
    val result = arrayDataType.validate(listOf("1", null, "3"))

    // then
    assert(result.isFailure())
    assert(result.errors().size == 1)
    assert(listOf("[1]", "Value cannot be null").all { result.errors().first().contains(it) })
  }

  @Test
  fun `validates an array with right item type`() {
    // given
    val arrayDataType = arrayType(items = integerType())

    // when
    val result = arrayDataType.validate(listOf(1, 2, 3))

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validates an array with enum values`() {
    // given
    val arrayDataType = arrayType(items = integerType(), enum = listOf(listOf(1, 3), listOf(2, 4)))

    // when
    val result = arrayDataType.validate(listOf(2, 4))

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate an array with enum values`() {
    // given
    val arrayDataType = arrayType(items = integerType(), enum = listOf(listOf(1, 3), listOf(2, 4)))

    // when
    val result = arrayDataType.validate(listOf(1, 2))

    // then
    assert(result.isFailure())
  }

  @Test
  fun `generates random value with enum values`() {
    // given
    val enum = listOf(listOf(1, 3), listOf(2, 4))
    val arrayDataType = arrayType(items = integerType(), enum = enum)

    // when
    val result = arrayDataType.randomValue()!!

    // then
    assert(enum.map { it.normalize() }.contains(result))
  }

  @Nested
  inner class WithMinItems {

    @Test
    fun `creation fails when minItems is negative`() {
      // when
      val result = ArrayDataType.create("array", stringType(), minItems = -1)

      // then
      assert(result.isFailure())
    }

    @Test
    fun `validation fails when array has fewer items than minItems`() {
      // given
      val arrayDataType = arrayType(items = stringType(), minItems = 3)

      // when
      val result = arrayDataType.validate(listOf("a", "b"))

      // then
      assert(result.isFailure())
    }

    @Test
    fun `validation succeeds when array has exactly minItems`() {
      // given
      val arrayDataType = arrayType(items = stringType(), minItems = 2)

      // when
      val result = arrayDataType.validate(listOf("a", "b"))

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `generates array with at least minItems elements`() {
      // given
      val arrayDataType = arrayType(items = stringType(), minItems = 3)

      // when
      val result = arrayDataType.randomValue()!!

      // then
      assert(result.size >= 3)
    }
  }

  @Nested
  inner class WithMaxItems {

    @Test
    fun `creation fails when maxItems is negative`() {
      // when
      val result = ArrayDataType.create("array", stringType(), maxItems = -1)

      // then
      assert(result.isFailure())
    }

    @Test
    fun `generates empty array when maxItems is 0`() {
      // given
      val arrayDataType = arrayType(items = stringType(), maxItems = 0)

      // when
      val result = arrayDataType.randomValue()!!

      // then
      assert(result.isEmpty())
    }

    @Test
    fun `validation fails when array has more items than maxItems`() {
      // given
      val arrayDataType = arrayType(items = stringType(), maxItems = 2)

      // when
      val result = arrayDataType.validate(listOf("a", "b", "c"))

      // then
      assert(result.isFailure())
    }

    @Test
    fun `validation succeeds when array has exactly maxItems`() {
      // given
      val arrayDataType = arrayType(items = stringType(), maxItems = 3)

      // when
      val result = arrayDataType.validate(listOf("a", "b", "c"))

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `generates array with at most maxItems elements`() {
      // given
      val arrayDataType = arrayType(items = stringType(), maxItems = 3)

      // when
      val values = (1..20).map { arrayDataType.randomValue()!! }

      // then
      assert(values.all { it.size <= 3 })
    }
  }

  @Nested
  inner class WithMinAndMaxItems {

    @Test
    fun `creation fails when minItems is greater than maxItems`() {
      // when
      val result = ArrayDataType.create("array", stringType(), minItems = 5, maxItems = 2)

      // then
      assert(result.isFailure())
    }

    @Test
    fun `generates array within minItems and maxItems bounds`() {
      // given
      val arrayDataType = arrayType(items = stringType(), minItems = 2, maxItems = 4)

      // when
      val values = (1..20).map { arrayDataType.randomValue()!! }

      // then
      assert(values.all { it.size in 2..4 })
    }
  }

  @Nested
  inner class WithUniqueItems {

    @Test
    fun `validation fails when array has duplicate items`() {
      // given
      val arrayDataType = arrayType(items = stringType(), uniqueItems = true)

      // when
      val result = arrayDataType.validate(listOf("a", "b", "a"))

      // then
      assert(result.isFailure())
    }

    @Test
    fun `validation succeeds when array has unique items`() {
      // given
      val arrayDataType = arrayType(items = stringType(), uniqueItems = true)

      // when
      val result = arrayDataType.validate(listOf("a", "b", "c"))

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `validation succeeds when uniqueItems is false and array has duplicates`() {
      // given
      val arrayDataType = arrayType(items = stringType(), uniqueItems = false)

      // when
      val result = arrayDataType.validate(listOf("a", "b", "a"))

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `creation fails when minItems exceeds boolean item cardinality`() {
      // when
      val result = ArrayDataType.create(
        "array",
        booleanType(),
        uniqueItems = true,
        minItems = 3
      )

      // then
      assert(result.isFailure())
    }

    @Test
    fun `creation fails when minItems exceeds enum item cardinality`() {
      // when
      val result = ArrayDataType.create(
        "array",
        stringType(enum = listOf("a", "b")),
        uniqueItems = true, minItems = 3)

      // then
      assert(result.isFailure())
    }

    @Test
    fun `creation fails when minItems exceeds integer range cardinality`() {
      // when
      val result = ArrayDataType.create(
        "array",
        integerType(minimum = BigDecimal.ZERO,
                        maximum = BigDecimal.TWO),
        uniqueItems = true,
        minItems = 4
      )

      // then
      assert(result.isFailure())
    }

    @Test
    fun `creation fails when minItems exceeds integer range cardinality with multipleOf`() {
      // when
      val result = ArrayDataType.create(
        "array",
        integerType(minimum = BigDecimal.ZERO,
                        maximum = BigDecimal.TEN,
                        multipleOf = BigDecimal(5)),
        uniqueItems = true,
        minItems = 4
      )

      // then
      assert(result.isFailure())
    }

    @Test
    fun `creation fails when minItems exceeds number range cardinality with multipleOf`() {
      // when
      val result = ArrayDataType.create(
        "array",
        numberType(minimum = BigDecimal.ZERO,
                       maximum = BigDecimal.TEN,
                       multipleOf = BigDecimal(5)),
        uniqueItems = true,
        minItems = 4
      )

      // then
      assert(result.isFailure())
    }

    @Test
    fun `creation fails when minItems exceeds number range cardinality with exclusiveMinimum`() {
      // when — exclusiveMinimum excludes 0 as a multiple, leaving only 5 and 10 (cardinality 2)
      val result = ArrayDataType.create(
        "array",
        numberType(minimum = BigDecimal.ZERO,
                       maximum = BigDecimal.TEN,
                       exclusiveMinimum = true,
                       multipleOf = BigDecimal(5)),
        uniqueItems = true,
        minItems = 3
      )

      // then
      assert(result.isFailure())
    }

    @Test
    fun `generates array with unique items`() {
      // given
      val arrayDataType = arrayType(
        items = stringType(enum = listOf("a", "b", "c", "d", "e")),
        uniqueItems = true,
        minItems = 3,
        maxItems = 5
      )

      // when
      val values = (1..20).map { arrayDataType.randomValue()!! }

      // then
      assert(values.all { it.size == it.distinct().size })
    }
  }
}