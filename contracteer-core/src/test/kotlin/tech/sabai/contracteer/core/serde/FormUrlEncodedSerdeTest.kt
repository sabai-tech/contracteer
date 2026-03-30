package tech.sabai.contracteer.core.serde

import tech.sabai.contracteer.core.TestFixture.arrayDataType
import tech.sabai.contracteer.core.TestFixture.integerDataType
import tech.sabai.contracteer.core.TestFixture.objectDataType
import tech.sabai.contracteer.core.TestFixture.stringDataType
import tech.sabai.contracteer.core.codec.FormStyleCodec
import tech.sabai.contracteer.core.codec.PipeDelimitedStyleCodec
import tech.sabai.contracteer.core.serde.FormUrlEncodedSerde.PropertyEncoding
import kotlin.test.Test

class FormUrlEncodedSerdeTest {

  @Test
  fun `serialize object with primitive properties`() {
    // given
    val serde = formUrlEncodedSerde("name", "age")

    // when
    val result = serde.serialize(mapOf("name" to "John", "age" to 30))

    // then
    assert(result == "name=John&age=30")
  }

  @Test
  fun `serialize object with array property using default encoding`() {
    // given
    val serde = formUrlEncodedSerde("name", "colors")

    // when
    val result = serde.serialize(mapOf("name" to "John", "colors" to listOf("blue", "black")))

    // then
    assert(result == "name=John&colors=blue&colors=black")
  }

  @Test
  fun `serialize object with custom encoding`() {
    // given
    val serde = FormUrlEncodedSerde(mapOf(
      "name" to PropertyEncoding(FormStyleCodec("name", explode = true)),
      "colors" to PropertyEncoding(PipeDelimitedStyleCodec("colors"))
    ))

    // when
    val result = serde.serialize(mapOf("name" to "John", "colors" to listOf("blue", "black")))

    // then
    assert(result == "name=John&colors=blue%7Cblack")
  }

  @Test
  fun `serialize URL-encodes special characters in values`() {
    // given
    val serde = formUrlEncodedSerde("name", "city")

    // when
    val result = serde.serialize(mapOf("name" to "John Doe", "city" to "New York"))

    // then
    assert(result == "name=John+Doe&city=New+York")
  }


  @Test
  fun `deserialize object with primitive properties`() {
    // given
    val serde = formUrlEncodedSerde("name", "age")
    val dataType = objectDataType(properties = mapOf("name" to stringDataType(), "age" to integerDataType()))

    // when
    val result = serde.deserialize("name=John&age=30", dataType)

    // then
    assert(result.isSuccess())
    val obj = result.value as Map<*, *>
    assert(obj["name"] == "John")
    assert(obj["age"] == 30.toBigDecimal())
  }

  @Test
  fun `deserialize object with array property using default encoding`() {
    // given
    val serde = formUrlEncodedSerde("name", "colors")
    val dataType = objectDataType(properties = mapOf(
      "name" to stringDataType(),
      "colors" to arrayDataType(itemDataType = stringDataType())
    ))

    // when
    val result = serde.deserialize("name=John&colors=blue&colors=black", dataType)

    // then
    assert(result.isSuccess())
    val obj = result.value as Map<*, *>
    assert(obj["name"] == "John")
    assert(obj["colors"] == listOf("blue", "black"))
  }

  @Test
  fun `deserialize object with custom encoding`() {
    // given
    val serde = FormUrlEncodedSerde(mapOf(
      "name" to PropertyEncoding(FormStyleCodec("name", explode = true)),
      "colors" to PropertyEncoding(PipeDelimitedStyleCodec("colors"))
    ))
    val dataType = objectDataType(properties = mapOf(
      "name" to stringDataType(),
      "colors" to arrayDataType(itemDataType = stringDataType())
    ))

    // when
    val result = serde.deserialize("name=John&colors=blue|black", dataType)

    // then
    assert(result.isSuccess())
    val obj = result.value as Map<*, *>
    assert(obj["name"] == "John")
    assert(obj["colors"] == listOf("blue", "black"))
  }

  @Test
  fun `deserialize URL-decodes special characters in values`() {
    // given
    val serde = formUrlEncodedSerde("name", "city")
    val dataType = objectDataType(properties = mapOf("name" to stringDataType(), "city" to stringDataType()))

    // when
    val result = serde.deserialize("name=John+Doe&city=New+York", dataType)

    // then
    assert(result.isSuccess())
    val obj = result.value as Map<*, *>
    assert(obj["name"] == "John Doe")
    assert(obj["city"] == "New York")
  }

  @Test
  fun `serialize preserves reserved characters when allowReserved is true`() {
    // given
    val serde = FormUrlEncodedSerde(mapOf(
      "callback" to PropertyEncoding(FormStyleCodec("callback", explode = true), allowReserved = true),
      "name" to PropertyEncoding(FormStyleCodec("name", explode = true))
    ))

    // when
    val result = serde.serialize(mapOf("callback" to "https://example.com/callback?token=abc", "name" to "John Doe"))

    // then
    assert(result == "callback=https://example.com/callback?token=abc&name=John+Doe")
  }

  @Test
  fun `serialize encodes reserved characters when allowReserved is false`() {
    // given
    val serde = FormUrlEncodedSerde(mapOf(
      "callback" to PropertyEncoding(FormStyleCodec("callback", explode = true), allowReserved = false),
      "name" to PropertyEncoding(FormStyleCodec("name", explode = true))
    ))

    // when
    val result = serde.serialize(mapOf("callback" to "https://example.com/callback?token=abc", "name" to "John Doe"))

    // then
    assert(result == "callback=https%3A%2F%2Fexample.com%2Fcallback%3Ftoken%3Dabc&name=John+Doe")
  }

  @Test
  fun `deserialize returns null for null source`() {
    // given
    val serde = formUrlEncodedSerde("name")
    val dataType = objectDataType(properties = mapOf("name" to stringDataType()))

    // when
    val result = serde.deserialize(null, dataType)

    // then
    assert(result.isSuccess())
    assert(result.value == null)
  }
}

/** Creates a FormUrlEncodedSerde with default encoding (form, explode=true) for the given property names. */
private fun formUrlEncodedSerde(vararg propertyNames: String) =
  FormUrlEncodedSerde(propertyNames.associate { it to PropertyEncoding(FormStyleCodec(it, explode = true)) })
