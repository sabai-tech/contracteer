package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.TestFixture.binaryDataType

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

  @Nested
  inner class WithEnum {
    @Test
    fun `does not create when enum length is not in the length range`() {
      // when
      val result = BinaryDataType.create(minLength = 1, maxLength = 2, enum = listOf("aë<â¿á$(", "î¯X[äjr~H"))

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

  @Nested
  inner class WithLengthRange {

    @Test
    fun `does not create when maxLength is negative`() {
      // when
      val result = BinaryDataType.create(maxLength = -1)

      // then
      assert(result.isFailure())
    }

    @Test
    fun `does not validate when value length is not in the range`() {
      // given
      val binaryDataType = binaryDataType(minLength = 1, maxLength = 5)

      // when
      val result = binaryDataType.validate("âÙæÅç*,¸éî¯X[äjr~H")

      // then
      assert(result.isFailure())
    }

    @Test
    fun `validates when value length is in the range`() {
      // given
      val binaryDataType = binaryDataType(minLength = 1, maxLength = 30)

      // when
      val result = binaryDataType.validate("âÙæÅç*,¸éî¯X[äjr~H")

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `generates random value with length inside the range`() {
      // given
      val binaryDataType = binaryDataType(minLength = 1, maxLength = 5)

      // when
      val result = binaryDataType.randomValue()

      // then
      assert(
        Range.create(1.toBigDecimal(), 5.toBigDecimal()).value!!.contains(result.length.toBigDecimal()).isSuccess()
      )
    }
  }
}