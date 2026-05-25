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

  @Test
  fun `accepts multipart composite body (allOf of objects)`() {
    // when
    val operations = OpenApiLoader
      .loadOperations("src/test/resources/operation/multipart/multipart_composite_body.yaml")
      .assertSuccess()

    // then
    val body = operations.single().requestSchema.bodies.single()
    assert(body.contentType.value == "multipart/form-data")
    val serde = body.serde as MultipartSerde
    assert(serde.partConfigs["name"]?.serde is PlainTextSerde)
    assert(serde.partConfigs["avatar"]?.isFile == true)
    assert(serde.partConfigs["description"]?.serde is PlainTextSerde)

    // and
    val original = mapOf("name" to "John", "avatar" to "binary-bytes", "description" to "hello")
    val serialized = serde.serialize(original)
    val deserialized = serde.deserialize(serialized, body.dataType).assertSuccess() as Map<*, *>
    assert(deserialized["name"] == "John")
    assert(deserialized["avatar"] == "binary-bytes")
    assert(deserialized["description"] == "hello")
  }

  @Test
  fun `extracts composite array of binary files as expandable parts`() {
    // when
    val operations = OpenApiLoader
      .loadOperations("src/test/resources/operation/multipart/multipart_composite_file_array.yaml")
      .assertSuccess()

    // then
    val serde = operations.single().requestSchema.bodies.single().serde as MultipartSerde
    assert(serde.partConfigs["files"]?.contentType == "application/octet-stream")
    assert(serde.partConfigs["files"]?.isFile == true)
    assert(serde.partConfigs["files"]?.expandArray == true)
  }

  @Test
  fun `rejects multipart kind-divergent part without explicit content type`() {
    // when
    val result = OpenApiLoader.loadOperations("src/test/resources/operation/multipart/multipart_kind_divergent_no_ct.yaml")

    // then
    assert(result.isFailure())
    assert(result.errors().any { it.contains("payload") && it.contains("contentType") })
  }

  @Test
  fun `accepts multipart kind-divergent part with explicit content type`() {
    // when
    val operations = OpenApiLoader
      .loadOperations("src/test/resources/operation/multipart/multipart_kind_divergent_explicit.yaml")
      .assertSuccess()

    // then
    val serde = operations.single().requestSchema.bodies.single().serde as MultipartSerde
    assert(serde.partConfigs["payload"]?.contentType == "application/octet-stream")
  }

  // --- Helpers ---

  private fun loadOperations() =
    OpenApiLoader.loadOperations("src/test/resources/operation/multipart/multipart_body.yaml").assertSuccess()
}