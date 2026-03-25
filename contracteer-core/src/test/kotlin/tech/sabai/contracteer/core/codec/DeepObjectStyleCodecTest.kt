package tech.sabai.contracteer.core.codec

import tech.sabai.contracteer.core.rgbObjectDataType
import tech.sabai.contracteer.core.valueExtractor
import kotlin.test.Test

class DeepObjectStyleCodecTest {

  @Test
  fun `encode object`() {
    val result = DeepObjectStyleCodec("color").encode(mapOf("R" to 100, "G" to 200, "B" to 150))
    assert(result == listOf("color[R]" to "100", "color[G]" to "200", "color[B]" to "150"))
  }

  @Test
  fun `decode object`() {
    // given — each property is a separate query param with name[key] format
    val extractor = valueExtractor("color[R]" to listOf("100"), "color[G]" to listOf("200"), "color[B]" to listOf("150"))

    // when
    val result = DeepObjectStyleCodec("color").decode(extractor, rgbObjectDataType())

    // then
    assert(result.isSuccess())
    val obj = result.value as Map<*, *>
    assert(obj["R"] == 100.toBigDecimal())
    assert(obj["G"] == 200.toBigDecimal())
    assert(obj["B"] == 150.toBigDecimal())
  }

  @Test
  fun `decode returns null when value is absent`() {
    // when
    val result = DeepObjectStyleCodec("color").decode(valueExtractor(), rgbObjectDataType())

    // then
    assert(result.isSuccess())
    assert(result.value == null)
  }
}
