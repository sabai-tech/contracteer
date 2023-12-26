package dev.blitzcraft.contracts.core.datatype

import kotlin.test.Test

class StringDataTypeTest {

  @Test
  fun `validates a value of type String`() {
    // given
    val stringDataType = StringDataType()

    // when
    val result = stringDataType.validateValue("Hello")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate value whose type is not a String`() {
    // given
    val stringDataType = StringDataType()

    // when
    val result = stringDataType.validateValue(true)

    // then
    assert(result.isSuccess().not())
  }

  @Test
  fun `parses and validates a string`() {
    // given
    val stringDataType = StringDataType()

    // when
    val result = stringDataType.parseAndValidate("Hello")

    // then
    assert(result.isSuccess())
  }
  }