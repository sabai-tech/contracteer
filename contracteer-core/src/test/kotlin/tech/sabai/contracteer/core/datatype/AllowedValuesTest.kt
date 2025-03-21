package tech.sabai.contracteer.core.datatype

import kotlin.test.Test

class AllowedValuesTest {


  @Test
  fun `should not create when values contains null but datatype is not nullable`() {
    // when
    val result = AllowedValues.create(
      listOf(1, null, 2),
      IntegerDataType.create(name = "integer", isNullable = false).value!!)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `should not create when values is empty`() {
    // when
    val result = AllowedValues.create(listOf(), IntegerDataType.create("integer").value!!)

    // then
    assert(result.isFailure())
  }


  @Test
  fun `should not create when values are not validated by datatype`() {
    // when
    val result = AllowedValues.create(listOf("1", "2"), IntegerDataType.create("integer").value!!)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `should create when values are validated by datatype`() {
    // when
    val result = AllowedValues.create(listOf(1, 2, 3), IntegerDataType.create("integer").value!!)

    // then
    assert(result.isSuccess())
  }


  @Test
  fun `should not contain when value if of wrong type`() {
    // given
    val allowedValues = AllowedValues.create(listOf(1, 2, 3), IntegerDataType.create("integer").value!!).value!!

    // when
    val result = allowedValues.contains("john")

    // then
    assert(result.isFailure())
  }

  @Test
  fun `should not contain value which is not in the allowed list`() {
    // given
    val allowedValues = AllowedValues.create(listOf(1, 2, 3), IntegerDataType.create("integer").value!!).value!!

    // when
    val result = allowedValues.contains(4)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `should contain value which is the allowed list`() {
    // given
    val allowedValues = AllowedValues.create(listOf(1, 2, 3), IntegerDataType.create("integer").value!!).value!!

    // when
    val result = allowedValues.contains(3)

    // then
    assert(result.isSuccess())
  }
}