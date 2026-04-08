package tech.sabai.contracteer.core.swagger

import tech.sabai.contracteer.core.assertSuccess
import tech.sabai.contracteer.core.codec.FormParameterCodec
import tech.sabai.contracteer.core.codec.PipeDelimitedParameterCodec
import tech.sabai.contracteer.core.serde.FormUrlEncodedSerde
import kotlin.test.Test

class FormUrlEncodedExtractionTest {

  @Test
  fun `extracts operation with form-urlencoded body using default encoding`() {
    // when
    val operation = loadOperations().single { it.path == "/login" }

    // then
    val body = operation.requestSchema.bodies.single()
    assert(body.contentType.value == "application/x-www-form-urlencoded")
    assert(body.serde is FormUrlEncodedSerde)
  }

  @Test
  fun `extracts operation with custom encoding map`() {
    // when
    val operation = loadOperations().single { it.path == "/preferences" }

    // then
    val serde = operation.requestSchema.bodies.single().serde as FormUrlEncodedSerde
    assert(serde.propertyEncodings["name"]?.codec is FormParameterCodec)
    assert(serde.propertyEncodings["colors"]?.codec is PipeDelimitedParameterCodec)
  }

  @Test
  fun `rejects form-urlencoded with non-object schema`() {
    // when
    val result =
      OpenApiLoader.loadOperations("src/test/resources/operation/form_urlencoded/form_urlencoded_invalid_schema.yaml")

    // then
    assert(result.isFailure())
    assert(result.errors().size == 1)
    assert(result.errors().first().contains("requires object schema"))
  }

  @Test
  fun `rejects form-urlencoded with nested object and array-of-objects properties`() {
    // when
    val result =
      OpenApiLoader.loadOperations("src/test/resources/operation/form_urlencoded/form_urlencoded_nested_types.yaml")

    // then
    assert(result.isFailure())
    assert(result.errors().any { it.contains("address") })
    assert(result.errors().any { it.contains("tags") })
  }

  // --- Helpers ---

  private fun loadOperations() =
    OpenApiLoader
      .loadOperations("src/test/resources/operation/form_urlencoded/form_urlencoded_body.yaml")
      .assertSuccess()
}