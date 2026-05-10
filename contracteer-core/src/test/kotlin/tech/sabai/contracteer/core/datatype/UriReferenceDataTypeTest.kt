package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.dsl.uriReferenceType
import java.net.URI

class UriReferenceDataTypeTest {

  @Test
  fun `validates an absolute URI`() {
    // given
    val uriReferenceDataType = uriReferenceType()

    // when
    val result = uriReferenceDataType.validate("https://example.com/path?query=1")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validates a path-absolute relative reference`() {
    // given
    val uriReferenceDataType = uriReferenceType()

    // when
    val result = uriReferenceDataType.validate("/path/here")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validates a path-noscheme relative reference`() {
    // given
    val uriReferenceDataType = uriReferenceType()

    // when
    val result = uriReferenceDataType.validate("path/here")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validates a relative reference with dot-segments`() {
    // given
    val uriReferenceDataType = uriReferenceType()

    // when
    val result = uriReferenceDataType.validate("../sibling/resource")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate a string containing spaces`() {
    // given
    val uriReferenceDataType = uriReferenceType()

    // when
    val result = uriReferenceDataType.validate("/path with space")

    // then
    assert(result.isFailure())
  }

  @Test
  fun `does not validate a malformed URI reference`() {
    // given
    val uriReferenceDataType = uriReferenceType()

    // when
    val result = uriReferenceDataType.validate("ht!tp:// not-valid")

    // then
    assert(result.isFailure())
  }

  @Test
  fun `validates null value if it is nullable`() {
    // given
    val uriReferenceDataType = uriReferenceType(isNullable = true)

    // when
    val result = uriReferenceDataType.validate(null)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate null value if it is not nullable`() {
    // given
    val uriReferenceDataType = uriReferenceType(isNullable = false)

    // when
    val result = uriReferenceDataType.validate(null)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `should generate a string representing a valid URI reference`() {
    // given
    val uriReferenceDataType = uriReferenceType()

    // when
    val randomUriReference = uriReferenceDataType.randomValue()!!

    // then
    assert(uriReferenceDataType.validate(randomUriReference).isSuccess())
  }

  @Test
  fun `should generate a relative reference rather than an absolute URI`() {
    // given
    val uriReferenceDataType = uriReferenceType()

    // when
    val randomUriReference = uriReferenceDataType.randomValue()!!

    // then
    assert(!URI(randomUriReference).isAbsolute) {
      "Expected a relative reference, but got an absolute URI: $randomUriReference"
    }
  }

  @Test
  fun `validates a string with enum values`() {
    // given
    val uriReferenceDataType = uriReferenceType(enum = listOf("/api/v1", "/api/v2"))

    // when
    val result = uriReferenceDataType.validate("/api/v1")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate a string outside enum values`() {
    // given
    val uriReferenceDataType = uriReferenceType(enum = listOf("/api/v1", "/api/v2"))

    // when
    val result = uriReferenceDataType.validate("/api/v3")

    // then
    assert(result.isFailure())
  }

  @Test
  fun `generates random value with enum values`() {
    // given
    val enum = listOf("/api/v1", "/api/v2")
    val uriReferenceDataType = uriReferenceType(enum = enum)

    // when
    val result = uriReferenceDataType.randomValue()

    // then
    assert(enum.contains(result))
  }
}