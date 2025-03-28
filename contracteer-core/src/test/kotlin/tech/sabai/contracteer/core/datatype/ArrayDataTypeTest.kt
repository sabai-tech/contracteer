package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.TestFixture.arrayDataType
import tech.sabai.contracteer.core.TestFixture.integerDataType
import tech.sabai.contracteer.core.TestFixture.stringDataType
import tech.sabai.contracteer.core.normalize

class ArrayDataTypeTest {

  @Test
  fun `validates null value if it is nullable`() {
    // given
    val arrayDataType = arrayDataType(itemDataType = stringDataType(), isNullable = true)

    // when
    val result = arrayDataType.validate(null)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate null value if it is not nullable`() {
    // given
    val arrayDataType = arrayDataType(itemDataType = stringDataType(), isNullable = false)

    // when
    val result = arrayDataType.validate(null)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `does validates value whose type is not array`() {
    // given
    val arrayDataType = arrayDataType(itemDataType = stringDataType())

    // when
    val result = arrayDataType.validate("value")

    // then
    assert(result.isFailure())
  }

  @Test
  fun `does validates array with wrong item type `() {
    // given
    val arrayDataType = arrayDataType(itemDataType = stringDataType())

    // when
    val result = arrayDataType.validate(listOf(1, 2, 3))

    // then
    assert(result.isFailure())
  }

  @Test
  fun `does not validate array when item type is not nullable`() {
    // given
    val arrayDataType = arrayDataType(itemDataType = stringDataType(isNullable = false))

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
    val arrayDataType = arrayDataType(itemDataType = integerDataType())

    // when
    val result = arrayDataType.validate(listOf(1, 2, 3))

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validates an array with enum values`() {
    // given
    val arrayDataType = arrayDataType(itemDataType = integerDataType(), enum = listOf(listOf(1, 3), listOf(2, 4)))

    // when
    val result = arrayDataType.validate(listOf(2, 4))

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate an array with enum values`() {
    // given
    val arrayDataType = arrayDataType(itemDataType = integerDataType(), enum = listOf(listOf(1, 3), listOf(2, 4)))

    // when
    val result = arrayDataType.validate(listOf(1, 2))

    // then
    assert(result.isFailure())
  }

  @Test
  fun `generates random value with enum values`() {
    // given
    val enum = listOf(listOf(1, 3), listOf(2, 4))
    val arrayDataType = arrayDataType(itemDataType = integerDataType(), enum = enum)

    // when
    val result = arrayDataType.randomValue()

    // then
    assert(enum.map { it.normalize() }.contains(result))
  }
}