package tech.sabai.contracteer.core.serde

import tech.sabai.contracteer.core.assertSuccess
import tech.sabai.contracteer.core.dsl.arrayType
import tech.sabai.contracteer.core.dsl.integerType
import tech.sabai.contracteer.core.dsl.objectType
import tech.sabai.contracteer.core.dsl.stringType
import tech.sabai.contracteer.core.codec.FormParameterCodec
import tech.sabai.contracteer.core.codec.PipeDelimitedParameterCodec
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
      "name" to PropertyEncoding(FormParameterCodec("name", explode = true)),
      "colors" to PropertyEncoding(PipeDelimitedParameterCodec("colors"))
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
    val dataType = objectType {
      properties {
        "name" to stringType()
        "age" to integerType()
      }
    }

    // when
    val result = serde.deserialize("name=John&age=30", dataType)

    // then
    val obj = result.assertSuccess() as Map<*, *>
    assert(obj["name"] == "John")
    assert(obj["age"] == 30.toBigDecimal())
  }

  @Test
  fun `deserialize object with array property using default encoding`() {
    // given
    val serde = formUrlEncodedSerde("name", "colors")
    val dataType = objectType {
      properties {
        "name" to stringType()
        "colors" to arrayType(items = stringType())
      }
    }

    // when
    val result = serde.deserialize("name=John&colors=blue&colors=black", dataType)

    // then
    val obj = result.assertSuccess() as Map<*, *>
    assert(obj["name"] == "John")
    assert(obj["colors"] == listOf("blue", "black"))
  }

  @Test
  fun `deserialize object with custom encoding`() {
    // given
    val serde = FormUrlEncodedSerde(mapOf(
      "name" to PropertyEncoding(FormParameterCodec("name", explode = true)),
      "colors" to PropertyEncoding(PipeDelimitedParameterCodec("colors"))
    ))
    val dataType = objectType {
      properties {
        "name" to stringType()
        "colors" to arrayType(items = stringType())
      }
    }

    // when
    val result = serde.deserialize("name=John&colors=blue|black", dataType)

    // then
    val obj = result.assertSuccess() as Map<*, *>
    assert(obj["name"] == "John")
    assert(obj["colors"] == listOf("blue", "black"))
  }

  @Test
  fun `deserialize URL-decodes special characters in values`() {
    // given
    val serde = formUrlEncodedSerde("name", "city")
    val dataType = objectType {
      properties {
        "name" to stringType()
        "city" to stringType()
      }
    }

    // when
    val result = serde.deserialize("name=John+Doe&city=New+York", dataType)

    // then
    val obj = result.assertSuccess() as Map<*, *>
    assert(obj["name"] == "John Doe")
    assert(obj["city"] == "New York")
  }

  @Test
  fun `serialize preserves reserved characters when allowReserved is true`() {
    // given
    val serde = FormUrlEncodedSerde(mapOf(
      "callback" to PropertyEncoding(FormParameterCodec("callback", explode = true), allowReserved = true),
      "name" to PropertyEncoding(FormParameterCodec("name", explode = true))
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
      "callback" to PropertyEncoding(FormParameterCodec("callback", explode = true), allowReserved = false),
      "name" to PropertyEncoding(FormParameterCodec("name", explode = true))
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
    val dataType = objectType { properties { "name" to stringType() } }

    // when
    val result = serde.deserialize(null, dataType)

    // then
    assert(result.assertSuccess() == null)
  }
}

/** Creates a FormUrlEncodedSerde with default encoding (form, explode=true) for the given property names. */
private fun formUrlEncodedSerde(vararg propertyNames: String) =
  FormUrlEncodedSerde(propertyNames.associateWith { PropertyEncoding(FormParameterCodec(it, explode = true)) })
