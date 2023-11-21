package dev.blitzcraft.contracts.core

import dev.blitzcraft.contracts.core.datatype.IntegerDataType
import kotlin.test.Test
import kotlin.test.assertNotNull

class PropertyTest {

  @Test
  fun `returns a non null value if the is no example`() {
    // given
    val property = Property(IntegerDataType())

    // when
    val value = property.value()

    // then
    assertNotNull(value)
  }

  @Test
  fun `returns Example value when it is provided`() {
    // given
    val property = Property(IntegerDataType(), Example(3))

    // when
    val value = property.value()

    // then
    assert(value == 3)
  }
}

