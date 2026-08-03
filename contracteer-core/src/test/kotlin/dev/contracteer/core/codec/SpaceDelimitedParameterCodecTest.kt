package dev.contracteer.core.codec

import dev.contracteer.core.assertSuccess
import dev.contracteer.core.dsl.anyOfType
import dev.contracteer.core.dsl.arrayType
import dev.contracteer.core.dsl.stringType
import kotlin.test.Test

class SpaceDelimitedParameterCodecTest {

  @Test
  fun `encode array`() {
    val result = SpaceDelimitedParameterCodec("color").encode(listOf("blue", "black", "brown"))
    assert(result == listOf("color" to "blue black brown"))
  }

  @Test
  fun `decode array`() {
    // given — the HTTP framework URL-decodes %20 to spaces
    val values = mapOf("color" to listOf("blue black brown"))

    // when
    val result = SpaceDelimitedParameterCodec("color").decode(values, arrayType(items = stringType()))

    // then
    assert(result.assertSuccess() ==listOf("blue", "black", "brown"))
  }

  @Test
  fun `decode returns null when value is absent`() {
    // when
    val result = SpaceDelimitedParameterCodec("color").decode(emptyMap(), arrayType(items = stringType()))

    // then
    assert(result.assertSuccess() ==null)
  }

  @Test
  fun `decode anyOf of arrays`() {
    // given
    val schema = anyOfType {
      subType(arrayType(items = stringType()))
      subType(arrayType(items = stringType()))
    }
    val values = mapOf("color" to listOf("blue black brown"))

    // when
    val result = SpaceDelimitedParameterCodec("color").decode(values, schema)

    // then
    assert(result.assertSuccess() == listOf("blue", "black", "brown"))
  }
}
