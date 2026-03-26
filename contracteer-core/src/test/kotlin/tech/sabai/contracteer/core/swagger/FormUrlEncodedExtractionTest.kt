package tech.sabai.contracteer.core.swagger

import tech.sabai.contracteer.core.assertSuccess
import tech.sabai.contracteer.core.codec.FormStyleCodec
import tech.sabai.contracteer.core.codec.PipeDelimitedStyleCodec
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
    assert(serde.propertyCodecs["name"] is FormStyleCodec)
    assert(serde.propertyCodecs["colors"] is PipeDelimitedStyleCodec)
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

  // --- Helpers ---

  private fun loadOperations() =
    OpenApiLoader
      .loadOperations("src/test/resources/operation/form_urlencoded/form_urlencoded_body.yaml")
      .assertSuccess()
}