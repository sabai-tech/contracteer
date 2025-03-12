package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.TestFixture.stringDataType

class StringDataTypeTest {

  @Test
  fun `validates a value of type string`() {
    // given
    val stringDataType = stringDataType()

    // when
    val result = stringDataType.validate("john doe")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate value whose type is not string`() {
    // given
    val stringDataType = stringDataType()

    // when
    val result = stringDataType.validate(true)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `validates null value if it is nullable`() {
    // given
    val stringDataType = stringDataType(isNullable = true)

    // when
    val result = stringDataType.validate(null)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate null value if it is not nullable`() {
    // given
    val stringDataType = stringDataType(isNullable = false)

    // when
    val result = stringDataType.validate(null)

    // then
    assert(result.isFailure())
  }

  @Nested
  inner class WithEnum {
    @Test
    fun `does not create when enum length is not in the length range`() {
      // when
      val result = StringDataType.create(openApiType = "string", minLength = 1, maxLength = 2, enum = listOf("ABC", "DEF"))

      // then
      assert(result.isFailure())
    }

    @Test
    fun `validates a string with enum values`() {
      // given
      val stringDataType = stringDataType(enum = listOf("Hello", "World"))

      // when
      val result = stringDataType.validate("World")

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `does not validate a string with enum values`() {
      // given
      val stringDataType = stringDataType(enum = listOf("Hello", "World"))

      // when
      val result = stringDataType.validate("John")

      // then
      assert(result.isFailure())
    }

    @Test
    fun `generates random value with enum values`() {
      // given
      val enum = listOf("Hello", "World")
      val stringDataType = stringDataType(enum = enum)

      // when
      val result = stringDataType.randomValue()

      // then
      assert(enum.contains(result))
    }
  }

  @Nested
  inner class WithLengthRange {

    @Test
    fun `does not create when maxLength is negative`() {
      // when
      val result = StringDataType.create(openApiType = "string", maxLength = -1)

      // then
      assert(result.isFailure())
    }

    @Test
    fun `does not validate when value length is not in the range`() {
      // given
      val stringDataType = stringDataType(minLength = 1, maxLength = 5)

      // when
      val result = stringDataType.validate("Hello World !")

      // then
      assert(result.isFailure())
    }

    @Test
    fun `validates when value length is in the range`() {
      // given
      val stringDataType = stringDataType(minLength = 1, maxLength = 15)

      // when
      val result = stringDataType.validate("Hello World !")

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `generates random value with length inside the range`() {
      // given
      val stringDataType = stringDataType(minLength = 1, maxLength = 5)

      // when
      val result = stringDataType.randomValue()

      // then
      assert(
        Range.create(1.toBigDecimal(), 5.toBigDecimal()).value!!.contains(result.length.toBigDecimal()).isSuccess()
      )
    }
  }
}
