package tech.sabai.contracteer.core.serde

import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.TestFixture.arrayDataType
import tech.sabai.contracteer.core.TestFixture.integerDataType
import tech.sabai.contracteer.core.TestFixture.objectDataType
import tech.sabai.contracteer.core.TestFixture.stringDataType
import tech.sabai.contracteer.core.normalize
import java.math.BigDecimal

class JsonSerdeTest {

  @Test
  fun `succeeds to serialize ObjectDatatype`() {
    // given
    val value = mapOf(
      "id" to 123,
      "nested" to mapOf("name" to "John"),
      "array" to listOf(1,2,3)
    )

    // when
    val result = JsonSerde.serialize(value)

    // then
    assert(result == """{"id":123,"nested":{"name":"John"},"array":[1,2,3]}""")
  }

  @Test
  fun `succeeds to deserialize when string matches datatype`() {
    // given
    val value = """
        {
          "id": 123,
          "nested": {
            "name": "John"
          },
          "array": [ 1,2,3]
        }""".trimIndent()
    val dataType = objectDataType(properties = mapOf(
      "id" to integerDataType(),
      "nested" to objectDataType(properties = mapOf("name" to stringDataType())),
      "array" to arrayDataType(integerDataType())
    ))

    // when
    val result = JsonSerde.deserialize(value, dataType)

    // then
    assert(result.isSuccess())
    assert(result.value == mapOf(
      "id" to BigDecimal.valueOf(123),
      "nested" to mapOf("name" to "John"),
      "array" to listOf(BigDecimal.valueOf(1), BigDecimal.valueOf(2), BigDecimal.valueOf(3))
    ))
  }

  @Test
  fun `fails to deserialize when string does not match datatype`() {
    // given
    val value = "Hello World"
    val dataType = objectDataType(properties = mapOf(
      "id" to integerDataType(),
      "nested" to objectDataType(properties = mapOf("name" to stringDataType())),
      "array" to arrayDataType(integerDataType())
    ))

    // when
    val result = JsonSerde.deserialize(value, dataType)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `deserialize normalizes values`() {
    // given
    val value = """{"id": 42, "items": [1, 2, 3]}"""
    val dataType = objectDataType(properties = mapOf(
      "id" to integerDataType(),
      "items" to arrayDataType(integerDataType())
    ))

    // when
    val result = JsonSerde.deserialize(value, dataType)

    // then
    assert(result.isSuccess())
    assert(result.value == mapOf(
      "id" to 42.normalize(),
      "items" to listOf(1, 2, 3).normalize()
    ))
  }

  @Test
  fun `fails to deserialize when string is null`() {
    // given
    val dataType = objectDataType(properties = mapOf(
      "id" to integerDataType(),
      "nested" to objectDataType(properties = mapOf("name" to stringDataType())),
      "array" to arrayDataType(integerDataType())
    ))

    // when
    val result = JsonSerde.deserialize(null, dataType)

    // then
    assert(result.isSuccess())
    assert(result.value == null)
  }
}


