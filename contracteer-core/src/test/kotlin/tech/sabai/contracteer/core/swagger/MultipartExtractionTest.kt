package tech.sabai.contracteer.core.swagger

import tech.sabai.contracteer.core.assertSuccess
import tech.sabai.contracteer.core.serde.MultipartSerde
import tech.sabai.contracteer.core.serde.JsonSerde
import tech.sabai.contracteer.core.serde.PlainTextSerde
import kotlin.test.Test

class MultipartExtractionTest {

  @Test
  fun `extracts operation with multipart body using default content types`() {
    // when
    val operation = loadOperations().single { it.path == "/defaults" }

    // then
    val body = operation.requestSchema.bodies.single()
    assert(body.contentType.value == "multipart/form-data")
    assert(body.serde is MultipartSerde)
    val serde = body.serde as MultipartSerde

    // primitive → text/plain
    assert(serde.partConfigs["name"]?.contentType == "text/plain")
    assert(serde.partConfigs["name"]?.serde is PlainTextSerde)

    // binary → application/octet-stream
    assert(serde.partConfigs["avatar"]?.contentType == "application/octet-stream")
    assert(serde.partConfigs["avatar"]?.serde is PlainTextSerde)

    // array → application/json (Contracteer uses JSON for all arrays in multipart)
    assert(serde.partConfigs["tags"]?.contentType == "application/json")
    assert(serde.partConfigs["tags"]?.serde is JsonSerde)

    // object → application/json
    assert(serde.partConfigs["profile"]?.contentType == "application/json")
    assert(serde.partConfigs["profile"]?.serde is JsonSerde)
  }

  @Test
  fun `extracts operation with custom encoding overriding default content type`() {
    // when
    val operation = loadOperations().single { it.path == "/custom-encoding" }

    // then
    val serde = operation.requestSchema.bodies.single().serde as MultipartSerde
    assert(serde.partConfigs["name"]?.contentType == "text/plain")
    assert(serde.partConfigs["metadata"]?.contentType == "application/vnd.api+json")
    assert(serde.partConfigs["metadata"]?.serde is JsonSerde)
  }

  @Test
  fun `extracts array of binary files as expandable parts`() {
    // when
    val operation = loadOperations().single { it.path == "/file-array" }

    // then
    val serde = operation.requestSchema.bodies.single().serde as MultipartSerde
    assert(serde.partConfigs["files"]?.contentType == "application/octet-stream")
    assert(serde.partConfigs["files"]?.isFile == true)
    assert(serde.partConfigs["files"]?.expandArray == true)
  }

  @Test
  fun `rejects multipart with non-object schema`() {
    // when
    val result = OpenApiLoader.loadOperations("src/test/resources/operation/multipart/multipart_invalid_schema.yaml")

    // then
    assert(result.isFailure())
    assert(result.errors().any { it.contains("requires object schema") })
  }

  // --- Helpers ---

  private fun loadOperations() =
    OpenApiLoader.loadOperations("src/test/resources/operation/multipart/multipart_body.yaml").assertSuccess()
}