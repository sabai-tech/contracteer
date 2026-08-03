package dev.contracteer.core.operation

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class ContentTypeTest {

  @Test
  fun `validates when content-types are equal`() {
    assert(ContentType("application/json").validate("application/json").isSuccess())
  }

  @Test
  fun `does not validate when content-types are not equal`() {
    assert(ContentType("application/json").validate("application/xml").isFailure())
  }

  @Test
  fun `wildcard matches any content type`() {
    assert(ContentType("*/*").validate("application/json").isSuccess())
  }

  @Test
  fun `subtype wildcard matches same type`() {
    assert(ContentType("image/*").validate("image/jpeg").isSuccess())
  }

  @Test
  fun `subtype wildcard does not match different type`() {
    assert(ContentType("image/*").validate("text/plain").isFailure())
  }

  @Test
  fun `matches when actual has parameters`() {
    assert(ContentType("multipart/form-data").validate("multipart/form-data; boundary=abc").isSuccess())
  }

  @Test
  fun `matches ignoring case`() {
    assert(ContentType("application/json").validate("Application/JSON").isSuccess())
  }

  @ParameterizedTest
  @ValueSource(strings = [
    "application/octet-stream",
    "image/png",
    "application/pdf",
    "application/zip",
    "audio/mpeg",
    "video/mp4"
  ])
  fun `isBinary returns true for binary media types`(value: String) {
    // Given
    val contentType = ContentType(value)

    // When
    val isBinary = contentType.isBinary()

    // Then
    assert(isBinary) { "Expected '$value' to be classified as binary" }
  }

  @ParameterizedTest
  @ValueSource(strings = [
    "application/json",
    "application/vnd.api+json",
    "application/xml",
    "image/svg+xml",
    "application/yaml",
    "application/x-yaml",
    "application/javascript"
  ])
  fun `isBinary returns false for structured text media types`(value: String) {
    // Given
    val contentType = ContentType(value)

    // When
    val isBinary = contentType.isBinary()

    // Then
    assert(!isBinary) { "Expected '$value' to not be classified as binary" }
  }

  @ParameterizedTest
  @ValueSource(strings = ["text/plain", "text/html", "text/csv"])
  fun `isBinary returns false for plain text media types`(value: String) {
    // Given
    val contentType = ContentType(value)

    // When
    val isBinary = contentType.isBinary()

    // Then
    assert(!isBinary) { "Expected '$value' to not be classified as binary" }
  }

  @ParameterizedTest
  @ValueSource(strings = [
    "multipart/form-data",
    "multipart/mixed",
    "application/x-www-form-urlencoded"
  ])
  fun `isBinary returns false for multipart and form-urlencoded`(value: String) {
    // Given
    val contentType = ContentType(value)

    // When
    val isBinary = contentType.isBinary()

    // Then
    assert(!isBinary) { "Expected '$value' to not be classified as binary" }
  }

  @ParameterizedTest(name = "[{index}] {0} -> isBinary={1}")
  @CsvSource(
    "Application/Octet-Stream, true",
    "application/octet-stream; charset=binary, true",
    "Application/JSON, false",
    "application/json; charset=utf-8, false"
  )
  fun `isBinary strips parameters and is case-insensitive`(value: String, expected: Boolean) {
    // Given
    val contentType = ContentType(value)

    // When
    val isBinary = contentType.isBinary()

    // Then
    assert(isBinary == expected) { "Expected isBinary=$expected for '$value' but got $isBinary" }
  }
}
