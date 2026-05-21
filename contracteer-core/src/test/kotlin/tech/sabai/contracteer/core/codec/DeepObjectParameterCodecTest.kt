package tech.sabai.contracteer.core.codec

import tech.sabai.contracteer.core.assertFailure
import tech.sabai.contracteer.core.assertSuccess
import tech.sabai.contracteer.core.dsl.integerType
import tech.sabai.contracteer.core.dsl.objectType
import tech.sabai.contracteer.core.rgbObjectType
import kotlin.test.Test

class DeepObjectParameterCodecTest {

  @Test
  fun `encode object`() {
    val result = DeepObjectParameterCodec("color").encode(mapOf("R" to 100, "G" to 200, "B" to 150))
    assert(result == listOf("color[R]" to "100", "color[G]" to "200", "color[B]" to "150"))
  }

  @Test
  fun `decode object`() {
    // given — each property is a separate query param with name[key] format
    val values = mapOf("color[R]" to listOf("100"), "color[G]" to listOf("200"), "color[B]" to listOf("150"))

    // when
    val result = DeepObjectParameterCodec("color").decode(values, rgbObjectType())

    // then
    val obj = result.assertSuccess() as Map<*, *>
    assert(obj["R"] == 100.toBigDecimal())
    assert(obj["G"] == 200.toBigDecimal())
    assert(obj["B"] == 150.toBigDecimal())
  }

  @Test
  fun `decode returns null when value is absent`() {
    // when
    val result = DeepObjectParameterCodec("color").decode(emptyMap(), rgbObjectType())

    // then
    assert(result.assertSuccess() == null)
  }

  @Test
  fun `decode object with default additionalProperties omits extras from decoded map`() {
    // given
    val values = mapOf("color[R]" to listOf("100"),
                                   "color[G]" to listOf("200"),
                                   "color[B]" to listOf("150"),
                                   "color[alpha]" to listOf("50"))

    // when
    val result = DeepObjectParameterCodec("color").decode(values, rgbObjectType())

    // then
    val obj = result.assertSuccess() as Map<*, *>
    assert(obj.keys == setOf("R", "G", "B"))
    assert(!obj.containsKey("alpha"))
  }

  @Test
  fun `decode object with additionalProperties false includes extras so validation can reject them`() {
    // given
    val schema = objectType(allowAdditionalProperties = false) {
      properties {
        "R" to integerType()
        "G" to integerType()
        "B" to integerType()
      }
    }
    val values = mapOf("color[R]" to listOf("100"),
                                   "color[G]" to listOf("200"),
                                   "color[B]" to listOf("150"),
                                   "color[alpha]" to listOf("50"))

    // when
    val result = DeepObjectParameterCodec("color").decode(values, schema)

    // then
    val obj = result.assertSuccess() as Map<*, *>
    assert(obj.keys.containsAll(setOf("R", "G", "B", "alpha")))
    assert(schema.validate(obj as Map<String, Any?>).assertFailure()
            .any { it.contains("Additional properties are not allowed") && it.contains("alpha") })
  }

  @Test
  fun `decode object with additionalProperties schema deserializes extras against that schema`() {
    // given
    val schema = objectType(additionalPropertiesDataType = integerType()) {
      properties {
        "R" to integerType()
        "G" to integerType()
        "B" to integerType()
      }
    }
    val values = mapOf("color[R]" to listOf("100"),
                                   "color[G]" to listOf("200"),
                                   "color[B]" to listOf("150"),
                                   "color[alpha]" to listOf("50"))

    // when
    val result = DeepObjectParameterCodec("color").decode(values, schema)

    // then
    val obj = result.assertSuccess() as Map<*, *>
    assert(obj["R"] == 100.toBigDecimal())
    assert(obj["alpha"] == 50.toBigDecimal())
    assert(schema.validate(obj as Map<String, Any?>).assertSuccess() == obj)
  }

  @Test
  fun `decode object with additionalProperties schema fails when extra value type-mismatches`() {
    // given
    val schema = objectType(additionalPropertiesDataType = integerType()) {
      properties { "R" to integerType() }
    }
    val values = mapOf("color[R]" to listOf("100"),
                                   "color[alpha]" to listOf("not-an-integer"))

    // when
    val result = DeepObjectParameterCodec("color").decode(values, schema)

    // then
    result.assertFailure()
  }
}
