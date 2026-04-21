package tech.sabai.contracteer.core.codec

import tech.sabai.contracteer.core.assertSuccess
import tech.sabai.contracteer.core.dsl.arrayType
import tech.sabai.contracteer.core.dsl.stringType
import tech.sabai.contracteer.core.rgbObjectDataType
import tech.sabai.contracteer.core.valueExtractor
import kotlin.test.Test

class MatrixParameterCodecTest {

  @Test
  fun `encode primitive`() {
    assert(MatrixParameterCodec("color", explode = false).encode("blue") == listOf("color" to ";color=blue"))
  }

  @Test
  fun `encode array with explode false`() {
    val result = MatrixParameterCodec("color", explode = false).encode(listOf("blue", "black", "brown"))
    assert(result == listOf("color" to ";color=blue,black,brown"))
  }

  @Test
  fun `encode array with explode true`() {
    val result = MatrixParameterCodec("color", explode = true).encode(listOf("blue", "black", "brown"))
    assert(result == listOf("color" to ";color=blue;color=black;color=brown"))
  }

  @Test
  fun `encode object with explode false`() {
    val result = MatrixParameterCodec("color", explode = false).encode(mapOf("R" to 100, "G" to 200, "B" to 150))
    assert(result == listOf("color" to ";color=R,100,G,200,B,150"))
  }

  @Test
  fun `encode object with explode true`() {
    val result = MatrixParameterCodec("color", explode = true).encode(mapOf("R" to 100, "G" to 200, "B" to 150))
    assert(result == listOf("color" to ";R=100;G=200;B=150"))
  }

  @Test
  fun `decode primitive`() {
    // given
    val extractor = valueExtractor("color" to listOf(";color=blue"))

    // when
    val result = MatrixParameterCodec("color", explode = false).decode(extractor, stringType())

    // then
    assert(result.assertSuccess() =="blue")
  }

  @Test
  fun `decode array with explode false`() {
    // given
    val extractor = valueExtractor("color" to listOf(";color=blue,black,brown"))

    // when
    val result = MatrixParameterCodec("color", explode = false).decode(extractor, arrayType(items = stringType()))

    // then
    assert(result.assertSuccess() ==listOf("blue", "black", "brown"))
  }

  @Test
  fun `decode array with explode true`() {
    // given
    val extractor = valueExtractor("color" to listOf(";color=blue;color=black;color=brown"))

    // when
    val result = MatrixParameterCodec("color", explode = true).decode(extractor, arrayType(items = stringType()))

    // then
    assert(result.assertSuccess() ==listOf("blue", "black", "brown"))
  }

  @Test
  fun `decode object with explode false`() {
    // given
    val extractor = valueExtractor("color" to listOf(";color=R,100,G,200,B,150"))

    // when
    val result = MatrixParameterCodec("color", explode = false).decode(extractor, rgbObjectDataType())

    // then
    val obj = result.assertSuccess() as Map<*, *>
    assert(obj["R"] == 100.toBigDecimal())
    assert(obj["G"] == 200.toBigDecimal())
    assert(obj["B"] == 150.toBigDecimal())
  }

  @Test
  fun `decode object with explode true`() {
    // given
    val extractor = valueExtractor("color" to listOf(";R=100;G=200;B=150"))

    // when
    val result = MatrixParameterCodec("color", explode = true).decode(extractor, rgbObjectDataType())

    // then
    val obj = result.assertSuccess() as Map<*, *>
    assert(obj["R"] == 100.toBigDecimal())
    assert(obj["G"] == 200.toBigDecimal())
    assert(obj["B"] == 150.toBigDecimal())
  }

  @Test
  fun `decode returns null when value is absent`() {
    // when
    val result = MatrixParameterCodec("color", explode = false).decode(valueExtractor(), stringType())

    // then
    assert(result.assertSuccess() ==null)
  }

  @Test
  fun `decode fails when value does not start with semicolon`() {
    // given
    val extractor = valueExtractor("color" to listOf("color=blue"))

    // when
    val result = MatrixParameterCodec("color", explode = false).decode(extractor, stringType())

    // then
    assert(result.isFailure())
  }
}
