package tech.sabai.contracteer.core.serde

import tech.sabai.contracteer.core.assertSuccess
import tech.sabai.contracteer.core.dsl.arrayType
import tech.sabai.contracteer.core.dsl.binaryType
import tech.sabai.contracteer.core.dsl.integerType
import tech.sabai.contracteer.core.dsl.objectType
import tech.sabai.contracteer.core.dsl.stringType
import kotlin.test.Test

class MultipartSerdeTest {

  @Test
  fun `serialize object with primitive properties`() {
    // given
    val serde = multipartSerde("name" to textPart(), "age" to textPart())

    // when
    val result = serde.serialize(mapOf("name" to "John", "age" to 30))

    // then
    assert(result.contains("Content-Disposition: form-data; name=\"name\""))
    assert(result.contains("Content-Type: text/plain"))
    assert(result.contains("John"))
    assert(result.contains("Content-Disposition: form-data; name=\"age\""))
    assert(result.contains("30"))
    assert(result.startsWith("--contracteer-boundary"))
    assert(result.endsWith("--contracteer-boundary--\r\n"))
  }

  @Test
  fun `serialize object with JSON part`() {
    // given
    val serde = multipartSerde("name" to textPart(), "metadata" to jsonPart())

    // when
    val result = serde.serialize(mapOf("name" to "John", "metadata" to mapOf("key" to "value")))

    // then
    assert(result.contains("Content-Disposition: form-data; name=\"metadata\""))
    assert(result.contains("Content-Type: application/json"))
    assert(result.contains("{\"key\":\"value\"}"))
  }

  @Test
  fun `serialize object with binary part`() {
    // given
    val serde = multipartSerde("name" to textPart(), "file" to binaryPart())

    // when
    val result = serde.serialize(mapOf("name" to "John", "file" to "binary-content"))

    // then
    assert(result.contains("Content-Disposition: form-data; name=\"file\"; filename=\"file\""))
    assert(result.contains("Content-Type: application/octet-stream"))
    assert(result.contains("binary-content"))
  }


  @Test
  fun `deserialize object with primitive properties`() {
    // given
    val serde = multipartSerde("name" to textPart(), "age" to textPart())
    val dataType = objectType {
      properties {
        "name" to stringType()
        "age" to integerType()
      }
    }
    val body = buildMultipartBody(
      "name" to ("text/plain" to "John"),
      "age" to ("text/plain" to "30")
    )

    // when
    val result = serde.deserialize(body, dataType)

    // then
    val obj = result.assertSuccess() as Map<*, *>
    assert(obj["name"] == "John")
    assert(obj["age"] == 30.toBigDecimal())
  }

  @Test
  fun `deserialize object with JSON part`() {
    // given
    val serde = multipartSerde("name" to textPart(), "metadata" to jsonPart())
    val dataType = objectType {
      properties {
        "name" to stringType()
        "metadata" to objectType { properties { "key" to stringType() } }
      }
    }
    val body = buildMultipartBody(
      "name" to ("text/plain" to "John"),
      "metadata" to ("application/json" to "{\"key\":\"value\"}")
    )

    // when
    val result = serde.deserialize(body, dataType)

    // then
    val obj = result.assertSuccess() as Map<*, *>
    assert(obj["name"] == "John")
    val metadata = obj["metadata"] as Map<*, *>
    assert(metadata["key"] == "value")
  }

  @Test
  fun `deserialize object with binary part`() {
    // given
    val serde = multipartSerde("name" to textPart(), "file" to binaryPart())
    val dataType = objectType {
      properties {
        "name" to stringType()
        "file" to binaryType()
      }
    }
    val body = buildMultipartBody(
      "name" to ("text/plain" to "John"),
      "file" to ("application/octet-stream" to "binary-content")
    )

    // when
    val result = serde.deserialize(body, dataType)

    // then
    val obj = result.assertSuccess() as Map<*, *>
    assert(obj["name"] == "John")
    assert(obj["file"] == "binary-content")
  }

  @Test
  fun `deserialize extracts boundary from body`() {
    // given
    val serde = multipartSerde("name" to textPart())
    val dataType = objectType { properties { "name" to stringType() } }
    val body = buildMultipartBody("name" to ("text/plain" to "John"), boundary = "custom-boundary")

    // when
    val result = serde.deserialize(body, dataType)

    // then
    val obj = result.assertSuccess() as Map<*, *>
    assert(obj["name"] == "John")
  }

  @Test
  fun `deserialize returns null for null source`() {
    // given
    val serde = multipartSerde("name" to textPart())
    val dataType = objectType { properties { "name" to stringType() } }

    // when
    val result = serde.deserialize(null, dataType)

    // then
    assert(result.assertSuccess() == null)
  }

  @Test
  fun `deserialize fails when body is not multipart format`() {
    // given
    val serde = multipartSerde("name" to textPart())
    val dataType = objectType { properties { "name" to stringType() } }

    // when
    val result = serde.deserialize("this is not a multipart body", dataType)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `serialize array of files as multiple parts with same name`() {
    // given
    val serde = multipartSerde("name" to textPart(), "files" to fileParts())

    // when
    val result = serde.serialize(mapOf("name" to "John", "files" to listOf("content1", "content2")))

    // then — two file parts, each with its own Content-Disposition
    val fileDispositions = "Content-Disposition: form-data; name=\"files\"; filename=\"files\"".toRegex().findAll(result).count()
    assert(fileDispositions == 2)
    assert(result.contains("content1"))
    assert(result.contains("content2"))
  }

  @Test
  fun `deserialize multiple parts with same name into array`() {
    // given
    val serde = multipartSerde("name" to textPart(), "files" to fileParts())
    val dataType = objectType {
      properties {
        "name" to stringType()
        "files" to arrayType(items = binaryType())
      }
    }
    val body = buildMultipartBody(
      "name" to ("text/plain" to "John"),
      "files" to ("application/octet-stream" to "content1"),
      "files" to ("application/octet-stream" to "content2")
    )

    // when
    val result = serde.deserialize(body, dataType)

    // then
    val obj = result.assertSuccess() as Map<*, *>
    assert(obj["name"] == "John")
    assert(obj["files"] == listOf("content1", "content2"))
  }

  @Test
  fun `serialize then deserialize roundtrip with file array`() {
    // given
    val serde = multipartSerde("name" to textPart(), "files" to fileParts())
    val dataType = objectType {
      properties {
        "name" to stringType()
        "files" to arrayType(items = binaryType())
      }
    }
    val original = mapOf("name" to "John", "files" to listOf("content1", "content2"))

    // when
    val serialized = serde.serialize(original)
    val result = serde.deserialize(serialized, dataType)

    // then
    val obj = result.assertSuccess() as Map<*, *>
    assert(obj["name"] == "John")
    assert(obj["files"] == listOf("content1", "content2"))
  }

  @Test
  fun `serialize then deserialize roundtrip`() {
    // given
    val serde = multipartSerde("name" to textPart(), "metadata" to jsonPart())
    val dataType = objectType {
      properties {
        "name" to stringType()
        "metadata" to objectType { properties { "key" to stringType() } }
      }
    }
    val original = mapOf("name" to "John", "metadata" to mapOf("key" to "value"))

    // when
    val serialized = serde.serialize(original)
    val result = serde.deserialize(serialized, dataType)

    // then
    val obj = result.assertSuccess() as Map<*, *>
    assert(obj["name"] == "John")
    val metadata = obj["metadata"] as Map<*, *>
    assert(metadata["key"] == "value")
  }
}

// --- Helpers ---

private fun textPart() = PartConfig("text/plain", PlainTextSerde)
private fun jsonPart() = PartConfig("application/json", JsonSerde)
private fun binaryPart() = PartConfig("application/octet-stream", PlainTextSerde, isFile = true)
private fun fileParts() = PartConfig("application/octet-stream", PlainTextSerde, isFile = true, expandArray = true)

private fun multipartSerde(vararg parts: Pair<String, PartConfig>) =
  MultipartSerde(parts.toMap())

private fun buildMultipartBody(
  vararg parts: Pair<String, Pair<String, String>>,
  boundary: String = "contracteer-boundary"
): String =
  parts.joinToString("") { (name, contentAndValue) ->
    val (contentType, value) = contentAndValue
    "--$boundary\r\n" +
      "Content-Disposition: form-data; name=\"$name\"\r\n" +
      "Content-Type: $contentType\r\n" +
      "\r\n" +
      "$value\r\n"
  } + "--$boundary--\r\n"
