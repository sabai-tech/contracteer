package dev.blitzcraft.contracts.core.datatype

import dev.blitzcraft.contracts.core.convert
import org.junit.jupiter.api.Test

class DecimalDataTypeTest {

  @Test
  fun `validates a value of type Decimal`() {
    // given
    val decimalDataType = DecimalDataType()

    // when
    val result = decimalDataType.validate(123.45.convert())

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate value whose type is not Decimal`() {
    // given
    val decimalDataType = DecimalDataType()

    // when
    val result = decimalDataType.validate(true)

    // then
    assert(result.isSuccess().not())
  }
  @Test
  fun `validates null value if it is nullable`() {
    // given
    val decimalDataType = DecimalDataType(isNullable = true)

    // when
    val result = decimalDataType.validate(null)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate null value if it is not nullable`() {
    // given
    val decimalDataType = DecimalDataType(isNullable = false)

    // when
    val result = decimalDataType.validate(null)

    // then
    assert(result.isSuccess().not())
  }
}