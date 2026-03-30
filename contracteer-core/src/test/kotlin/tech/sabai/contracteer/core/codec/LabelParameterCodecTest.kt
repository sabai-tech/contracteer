package tech.sabai.contracteer.core.codec

import tech.sabai.contracteer.core.TestFixture.arrayDataType
import tech.sabai.contracteer.core.TestFixture.stringDataType
import tech.sabai.contracteer.core.rgbObjectDataType
import tech.sabai.contracteer.core.valueExtractor
import kotlin.test.Test

class LabelParameterCodecTest {

  // ===== Encode =====

  @Test
  fun `encode primitive`() {
    assert(LabelParameterCodec("color", explode = false).encode("blue") == listOf("color" to ".blue"))
  }

  @Test
  fun `encode array with explode false`() {
    val result = LabelParameterCodec("color", explode = false).encode(listOf("blue", "black", "brown"))
    assert(result == listOf("color" to ".blue,black,brown"))
  }

  @Test
  fun `encode array with explode true`() {
    val result = LabelParameterCodec("color", explode = true).encode(listOf("blue", "black", "brown"))
    assert(result == listOf("color" to ".blue.black.brown"))
  }

  @Test
  fun `encode object with explode false`() {
    val result = LabelParameterCodec("color", explode = false).encode(mapOf("R" to 100, "G" to 200, "B" to 150))
    assert(result == listOf("color" to ".R,100,G,200,B,150"))
  }

  @Test
  fun `encode object with explode true`() {
    val result = LabelParameterCodec("color", explode = true).encode(mapOf("R" to 100, "G" to 200, "B" to 150))
    assert(result == listOf("color" to ".R=100.G=200.B=150"))
  }

  // ===== Decode =====

  @Test
  fun `decode primitive`() {
    // given
    val extractor = valueExtractor("color" to listOf(".blue"))

    // when
    val result = LabelParameterCodec("color", explode = false).decode(extractor, stringDataType())

    // then
    assert(result.isSuccess())
    assert(result.value == "blue")
  }

  @Test
  fun `decode array with explode false`() {
    // given
    val extractor = valueExtractor("color" to listOf(".blue,black,brown"))

    // when
    val result = LabelParameterCodec("color", explode = false).decode(extractor, arrayDataType(itemDataType = stringDataType()))

    // then
    assert(result.isSuccess())
    assert(result.value == listOf("blue", "black", "brown"))
  }

  @Test
  fun `decode array with explode true`() {
    // given
    val extractor = valueExtractor("color" to listOf(".blue.black.brown"))

    // when
    val result = LabelParameterCodec("color", explode = true).decode(extractor, arrayDataType(itemDataType = stringDataType()))

    // then
    assert(result.isSuccess())
    assert(result.value == listOf("blue", "black", "brown"))
  }

  @Test
  fun `decode object with explode false`() {
    // given
    val extractor = valueExtractor("color" to listOf(".R,100,G,200,B,150"))

    // when
    val result = LabelParameterCodec("color", explode = false).decode(extractor, rgbObjectDataType())

    // then
    assert(result.isSuccess())
    val obj = result.value as Map<*, *>
    assert(obj["R"] == 100.toBigDecimal())
    assert(obj["G"] == 200.toBigDecimal())
    assert(obj["B"] == 150.toBigDecimal())
  }

  @Test
  fun `decode object with explode true`() {
    // given
    val extractor = valueExtractor("color" to listOf(".R=100.G=200.B=150"))

    // when
    val result = LabelParameterCodec("color", explode = true).decode(extractor, rgbObjectDataType())

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
    val result = LabelParameterCodec("color", explode = false).decode(valueExtractor(), stringDataType())

    // then
    assert(result.isSuccess())
    assert(result.value == null)
  }

  @Test
  fun `decode fails when value does not start with dot`() {
    // given
    val extractor = valueExtractor("color" to listOf("blue"))

    // when
    val result = LabelParameterCodec("color", explode = false).decode(extractor, stringDataType())

    // then
    assert(result.isFailure())
  }
}
