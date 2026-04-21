package tech.sabai.contracteer.core.codec

import tech.sabai.contracteer.core.assertSuccess
import tech.sabai.contracteer.core.dsl.arrayType
import tech.sabai.contracteer.core.dsl.stringType
import tech.sabai.contracteer.core.valueExtractor
import kotlin.test.Test

class PipeDelimitedParameterCodecTest {

  @Test
  fun `encode array`() {
    val result = PipeDelimitedParameterCodec("color").encode(listOf("blue", "black", "brown"))
    assert(result == listOf("color" to "blue|black|brown"))
  }

  @Test
  fun `decode array`() {
    // given
    val extractor = valueExtractor("color" to listOf("blue|black|brown"))

    // when
    val result = PipeDelimitedParameterCodec("color").decode(extractor, arrayType(items = stringType()))

    // then
    assert(result.assertSuccess() ==listOf("blue", "black", "brown"))
  }

  @Test
  fun `decode returns null when value is absent`() {
    // when
    val result = PipeDelimitedParameterCodec("color").decode(valueExtractor(), arrayType(items = stringType()))

    // then
    assert(result.assertSuccess() ==null)
  }
}
