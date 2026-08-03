package dev.contracteer.core.codec

import dev.contracteer.core.assertFailure
import dev.contracteer.core.assertSuccess
import dev.contracteer.core.dsl.allOfType
import dev.contracteer.core.dsl.anyOfType
import dev.contracteer.core.dsl.integerType
import dev.contracteer.core.dsl.objectType
import dev.contracteer.core.dsl.oneOfType
import dev.contracteer.core.dsl.stringType
import dev.contracteer.core.rgbObjectType
import kotlin.test.Test

@Suppress("UNCHECKED_CAST")
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

  @Test
  fun `decode allOf of objects merges properties from both branches`() {
    // given
    val schema = allOfType {
      subType(objectType("base") { properties { "name" to stringType() } })
      subType(objectType("extra") { properties { "age" to integerType() } })
    }
    val values = mapOf("user[name]" to listOf("ada"), "user[age]" to listOf("36"))

    // when
    val result = DeepObjectParameterCodec("user").decode(values, schema)

    // then
    val obj = result.assertSuccess() as Map<*, *>
    assert(obj["name"] == "ada")
    assert(obj["age"] == 36.toBigDecimal())
    schema.validate(obj as Map<String, Any?>).assertSuccess()
  }

  @Test
  fun `decode oneOf of objects decodes properties from any branch`() {
    // given
    val schema = oneOfType {
      subType(objectType("primary", allowAdditionalProperties = false) {
        properties { "kind" to stringType(); "primary" to stringType() }
      })
      subType(objectType("secondary", allowAdditionalProperties = false) {
        properties { "kind" to stringType(); "secondary" to stringType() }
      })
    }
    val values = mapOf("payload[kind]" to listOf("primary"), "payload[primary]" to listOf("yes"))

    // when
    val result = DeepObjectParameterCodec("payload").decode(values, schema)

    // then
    val obj = result.assertSuccess() as Map<*, *>
    assert(obj["kind"] == "primary")
    assert(obj["primary"] == "yes")
    schema.validate(obj as Map<String, Any?>).assertSuccess()
  }

  @Test
  fun `decode anyOf of objects decodes properties from any branch`() {
    // given
    val schema = anyOfType {
      subType(objectType("base") { properties { "name" to stringType() } })
      subType(objectType("contact") { properties { "email" to stringType() } })
    }
    val values = mapOf("payload[name]" to listOf("ada"), "payload[email]" to listOf("ada@example.com"))

    // when
    val result = DeepObjectParameterCodec("payload").decode(values, schema)

    // then
    val obj = result.assertSuccess() as Map<*, *>
    assert(obj["name"] == "ada")
    assert(obj["email"] == "ada@example.com")
    schema.validate(obj as Map<String, Any?>).assertSuccess()
  }

  @Test
  fun `decode allOf composite honors closed additionalProperties policy across branches`() {
    // given
    val schema = allOfType {
      subType(objectType("base", allowAdditionalProperties = false) {
        properties { "name" to stringType() }
      })
      subType(objectType("extra", allowAdditionalProperties = false) {
        properties { "age" to integerType() }
      })
    }
    val values = mapOf(
      "user[name]" to listOf("ada"),
      "user[age]" to listOf("36"),
      "user[unexpected]" to listOf("ignored")
    )

    // when
    val result = DeepObjectParameterCodec("user").decode(values, schema)

    // then
    val obj = result.assertSuccess() as Map<*, *>
    assert(obj.keys.containsAll(setOf("name", "age", "unexpected")))
    assert(schema.validate(obj as Map<String, Any?>).assertFailure()
            .any { it.contains("Additional properties are not allowed") && it.contains("unexpected") })
  }
}
