package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.DataTypeFixture.binaryDataType

class BinaryDataTypeTest {

  @Test
  fun `validates a value of type string`() {
    // given
    val binaryDataType = binaryDataType()

    // when
    val result = binaryDataType.validate("aë<â¿á$(")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate value whose type is not string`() {
    // given
    val binaryDataType = binaryDataType()

    // when
    val result = binaryDataType.validate(true)

    // then
    assert(result.isFailure())
  }
  @Test
  fun `validates null value if it is nullable`() {
    // given
    val binaryDataType = BinaryDataType.create(isNullable = true).value!!

    // when
    val result = binaryDataType.validate(null)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate null value if it is not nullable`() {
    // given
    val binaryDataType = BinaryDataType.create(isNullable = false).value!!

    // when
    val result = binaryDataType.validate(null)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `validates a string with enum values`() {
    // given
    val binaryDataType = binaryDataType(enum = listOf("aë<â¿á$(", "î¯X[äjr~H"))

    // when
    val result = binaryDataType.validate("aë<â¿á$(")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate a string with enum values`() {
    // given
    val binaryDataType = binaryDataType(enum = listOf("aë<â¿á$(", "î¯X[äjr~H"))

    // when
    val result = binaryDataType.validate("âÙæÅç*,¸é")

    // then
    assert(result.isFailure())
  }

  @Test
  fun `generates random value with enum values`() {
    // given
    val enum = listOf("aë<â¿á$(", "î¯X[äjr~H")
    val binaryDataType = binaryDataType(enum = enum)

    // when
    val result = binaryDataType.randomValue()

    // then
    assert(enum.contains(result))
  }
}