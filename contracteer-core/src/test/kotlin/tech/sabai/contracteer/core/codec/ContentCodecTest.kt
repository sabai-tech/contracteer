package tech.sabai.contracteer.core.codec

import tech.sabai.contracteer.core.assertSuccess
import tech.sabai.contracteer.core.dsl.integerType
import tech.sabai.contracteer.core.dsl.objectType
import tech.sabai.contracteer.core.dsl.stringType
import tech.sabai.contracteer.core.normalize
import tech.sabai.contracteer.core.serde.JsonSerde
import tech.sabai.contracteer.core.serde.PlainTextSerde
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
    val values = mapOf("filter" to listOf("""{"status":"active","limit":10}"""))
    val dataType = objectType {
      properties {
        "status" to stringType()
        "limit" to integerType()
      }
    }

    // when
    val result = ContentCodec("filter", JsonSerde).decode(values, dataType)

    // then
    assert(result.assertSuccess() == mapOf("status" to "active", "limit" to 10.normalize()))
  }

  @Test
  fun `decode returns null when value is missing`() {
    // when
    val result = ContentCodec("filter", JsonSerde).decode(emptyMap(), stringType())

    // then
    assert(result.assertSuccess() == null)
  }

  @Test
  fun `decode returns failure for invalid JSON`() {
    // given
    val values = mapOf("filter" to listOf("not-valid-json{"))
    val dataType = objectType { properties { "status" to stringType() } }

    // when
    val result = ContentCodec("filter", JsonSerde).decode(values, dataType)

    // then
    assert(result.isFailure())
  }
}
