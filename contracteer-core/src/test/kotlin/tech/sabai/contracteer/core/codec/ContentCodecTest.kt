package tech.sabai.contracteer.core.codec

import tech.sabai.contracteer.core.TestFixture.integerDataType
import tech.sabai.contracteer.core.TestFixture.objectDataType
import tech.sabai.contracteer.core.TestFixture.stringDataType
import tech.sabai.contracteer.core.normalize
import tech.sabai.contracteer.core.serde.JsonSerde
import tech.sabai.contracteer.core.serde.PlainTextSerde
import tech.sabai.contracteer.core.valueExtractor
import java.math.BigDecimal
import kotlin.test.Test

class ContentCodecTest {

  @Test
  fun `encode object value as JSON string`() {
    // given
    val codec = ContentCodec("filter", JsonSerde)

    // when
    val result = codec.encode(mapOf("status" to "active", "limit" to 10))

    // then
    assert(result == listOf("filter" to """{"status":"active","limit":10}"""))
  }

  @Test
  fun `encode primitive value as plain text`() {
    // given
    val codec = ContentCodec("name", PlainTextSerde)

    // when
    val result = codec.encode("hello")

    // then
    assert(result == listOf("name" to "hello"))
  }

  @Test
  fun `decode JSON string to object`() {
    // given
    val extractor = valueExtractor("filter" to listOf("""{"status":"active","limit":10}"""))
    val dataType = objectDataType(
      properties = mapOf(
        "status" to stringDataType(),
        "limit" to integerDataType()))

    // when
    val result = ContentCodec("filter", JsonSerde).decode(extractor, dataType)

    // then
    assert(result.isSuccess())
    assert(result.value == mapOf("status" to "active", "limit" to 10.normalize()))
  }

  @Test
  fun `decode returns null when value is missing`() {
    // given
    val extractor = valueExtractor()

    // when
    val result = ContentCodec("filter", JsonSerde).decode(extractor, stringDataType())

    // then
    assert(result.isSuccess())
    assert(result.value == null)
  }

  @Test
  fun `decode returns failure for invalid JSON`() {
    // given
    val extractor = valueExtractor("filter" to listOf("not-valid-json{"))
    val dataType = objectDataType(
      properties = mapOf("status" to stringDataType()))

    // when
    val result = ContentCodec("filter", JsonSerde).decode(extractor, dataType)

    // then
    assert(result.isFailure())
  }
}
