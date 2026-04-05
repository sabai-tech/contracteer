package tech.sabai.contracteer.core.codec

import tech.sabai.contracteer.core.assertSuccess
import tech.sabai.contracteer.core.TestFixture.arrayDataType
import tech.sabai.contracteer.core.TestFixture.stringDataType
import tech.sabai.contracteer.core.rgbObjectDataType
import tech.sabai.contracteer.core.valueExtractor
import kotlin.test.Test

class FormParameterCodecTest {

  @Test
  fun `encode primitive`() {
    assert(FormParameterCodec("color", explode = false).encode("blue") == listOf("color" to "blue"))
  }

  @Test
  fun `encode array with explode false`() {
    val result = FormParameterCodec("color", explode = false).encode(listOf("blue", "black", "brown"))
    assert(result == listOf("color" to "blue,black,brown"))
  }

  @Test
  fun `encode array with explode true`() {
    val result = FormParameterCodec("color", explode = true).encode(listOf("blue", "black", "brown"))
    assert(result == listOf("color" to "blue", "color" to "black", "color" to "brown"))
  }

  @Test
  fun `encode object with explode false`() {
    val result = FormParameterCodec("color", explode = false).encode(mapOf("R" to 100, "G" to 200, "B" to 150))
    assert(result == listOf("color" to "R,100,G,200,B,150"))
  }

  @Test
  fun `encode object with explode true`() {
    val result = FormParameterCodec("color", explode = true).encode(mapOf("R" to 100, "G" to 200, "B" to 150))
    assert(result == listOf("R" to "100", "G" to "200", "B" to "150"))
  }

  @Test
  fun `decode primitive`() {
    // given
    val extractor = valueExtractor("color" to listOf("blue"))

    // when
    val result = FormParameterCodec("color", explode = false).decode(extractor, stringDataType())

    // then
    assert(result.assertSuccess() =="blue")
  }

  @Test
  fun `decode array with explode false`() {
    // given
    val extractor = valueExtractor("color" to listOf("blue,black,brown"))

    // when
    val result = FormParameterCodec("color", explode = false).decode(extractor, arrayDataType(itemDataType = stringDataType()))

    // then
    assert(result.assertSuccess() ==listOf("blue", "black", "brown"))
  }

  @Test
  fun `decode array with explode true`() {
    // given — the HTTP framework returns multiple values for the same key
    val extractor = valueExtractor("color" to listOf("blue", "black", "brown"))

    // when
    val result = FormParameterCodec("color", explode = true).decode(extractor, arrayDataType(itemDataType = stringDataType()))

    // then
    assert(result.assertSuccess() ==listOf("blue", "black", "brown"))
  }

  @Test
  fun `decode object with explode false`() {
    // given
    val extractor = valueExtractor("color" to listOf("R,100,G,200,B,150"))

    // when
    val result = FormParameterCodec("color", explode = false).decode(extractor, rgbObjectDataType())

    // then
    val obj = result.assertSuccess() as Map<*, *>
    assert(obj["R"] == 100.toBigDecimal())
    assert(obj["G"] == 200.toBigDecimal())
    assert(obj["B"] == 150.toBigDecimal())
  }

  @Test
  fun `decode object with explode true`() {
    // given — each property is a separate query param extracted by its own key
    val extractor = valueExtractor("R" to listOf("100"), "G" to listOf("200"), "B" to listOf("150"))

    // when
    val result = FormParameterCodec("color", explode = true).decode(extractor, rgbObjectDataType())

    // then
    val obj = result.assertSuccess() as Map<*, *>
    assert(obj["R"] == 100.toBigDecimal())
    assert(obj["G"] == 200.toBigDecimal())
    assert(obj["B"] == 150.toBigDecimal())
  }

  @Test
  fun `decode returns null when value is absent`() {
    // when
    val result = FormParameterCodec("color", explode = false).decode(valueExtractor(), stringDataType())

    // then
    assert(result.assertSuccess() ==null)
  }
}
