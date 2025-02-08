package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Test

class ArrayDataTypeTest {

  @Test
  fun `validates null value if it is nullable`() {
    // given
    val arrayDataType = ArrayDataType(itemDataType = StringDataType(), isNullable = true)

    // when
    val result = arrayDataType.validate(null)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate null value if it is not nullable`() {
    // given
    val arrayDataType = ArrayDataType(itemDataType = StringDataType(), isNullable = false)

    // when
    val result = arrayDataType.validate(null)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `does validates value whose type is not array`() {
    // given
    val arrayDataType = ArrayDataType(itemDataType = StringDataType())

    // when
    val result = arrayDataType.validate("value")

    // then
    assert(result.isFailure())
  }

  @Test
  fun `does validates array with wrong item type `() {
    // given
    val arrayDataType = ArrayDataType(itemDataType = StringDataType())

    // when
    val result = arrayDataType.validate(arrayOf(1, 2, 3))

    // then
    assert(result.isFailure())
  }

  @Test
  fun `does not validate array when item type is not nullable`() {
    // given
    val arrayDataType = ArrayDataType(itemDataType = StringDataType(isNullable = false))

    // when
    val result = arrayDataType.validate(arrayOf("1", null, "3"))

    // then
    assert(result.isFailure())
    assert(result.errors().size == 1)
    assert(listOf("[1]", "Cannot be null").all { result.errors().first().contains(it) })
  }

  @Test
  fun `validates an array with right item type`() {
    // given
    val arrayDataType = ArrayDataType(itemDataType = IntegerDataType())

    // when
    val result = arrayDataType.validate(arrayOf(1, 2, 3))

    // then
    assert(result.isSuccess())
  }
}