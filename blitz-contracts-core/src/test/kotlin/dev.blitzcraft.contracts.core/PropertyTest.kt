package dev.blitzcraft.contracts.core

import dev.blitzcraft.contracts.core.datatype.IntegerDataType
import kotlin.test.Test
import kotlin.test.assertNotNull

class PropertyTest {

  @Test
  fun `returns a non null value if the is no example`() {
    // given
    val property = Property("prop", IntegerDataType())

    // when
    val value = property.value()

    // then
    assertNotNull(value)
  }

  @Test
  fun `returns Example value when it is provided`() {
    // given
    val property = Property("prop", IntegerDataType(), Example(3))

    // when
    val value = property.value()

    // then
    assert(value == 3)
  }

  @Test
  fun `validates successfully a null value`() {
    // given
    val property = Property("prop", IntegerDataType())

    // when
    val result = property.validateValue(null)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `parses successfully a null string`() {
    // given
    val property = Property("prop", IntegerDataType())

    // when
    val result = property.parseAndValidate(null)

    // then
    assert(result.isSuccess())
  }
}

