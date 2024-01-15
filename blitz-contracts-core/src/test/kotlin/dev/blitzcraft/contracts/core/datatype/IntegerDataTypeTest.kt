package dev.blitzcraft.contracts.core.datatype

import dev.blitzcraft.contracts.core.convert
import org.junit.jupiter.api.Test

class IntegerDataTypeTest {

  @Test
  fun `validates a value of type Integer`() {
    // given
    val integerDataType = IntegerDataType()

    // when
    val result = integerDataType.validate(123.convert())

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate value whose type is not Integer`() {
    // given
    val integerDataType = IntegerDataType()

    // when
    val result = integerDataType.validate(true)

    // then
    assert(result.isSuccess().not())
  }
  @Test
  fun `validates null value if it is nullable`() {
    // given
    val integerDataType = IntegerDataType(isNullable = true)

    // when
    val result = integerDataType.validate(null)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate null value if it is not nullable`() {
    // given
    val integerDataType = IntegerDataType(isNullable = false)

    // when
    val result = integerDataType.validate(null)

    // then
    assert(result.isSuccess().not())
  }
}