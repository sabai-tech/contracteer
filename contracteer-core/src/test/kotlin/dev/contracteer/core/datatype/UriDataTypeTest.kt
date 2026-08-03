package dev.contracteer.core.datatype

import org.junit.jupiter.api.Test
import dev.contracteer.core.dsl.uriType

class UriDataTypeTest {

  @Test
  fun `validates an absolute https URI`() {
    // given
    val uriDataType = uriType()

    // when
    val result = uriDataType.validate("https://example.com/path?query=1#fragment")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validates an absolute http URI`() {
    // given
    val uriDataType = uriType()

    // when
    val result = uriDataType.validate("http://example.com")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validates an opaque URI such as mailto`() {
    // given
    val uriDataType = uriType()

    // when
    val result = uriDataType.validate("mailto:foo@example.com")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validates a urn URI`() {
    // given
    val uriDataType = uriType()

    // when
    val result = uriDataType.validate("urn:isbn:0451450523")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate a relative reference`() {
    // given
    val uriDataType = uriType()

    // when
    val result = uriDataType.validate("/path/here")

    // then
    assert(result.isFailure())
  }

  @Test
  fun `does not validate a string containing spaces`() {
    // given
    val uriDataType = uriType()

    // when
    val result = uriDataType.validate("https://example.com/with space")

    // then
    assert(result.isFailure())
  }

  @Test
  fun `does not validate a malformed URI`() {
    // given
    val uriDataType = uriType()

    // when
    val result = uriDataType.validate("ht!tp:// not-valid")

    // then
    assert(result.isFailure())
  }

  @Test
  fun `validates null value if it is nullable`() {
    // given
    val uriDataType = uriType(isNullable = true)

    // when
    val result = uriDataType.validate(null)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate null value if it is not nullable`() {
    // given
    val uriDataType = uriType(isNullable = false)

    // when
    val result = uriDataType.validate(null)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `should generate a string representing a valid absolute URI`() {
    // given
    val uriDataType = uriType()

    // when
    val randomUri = uriDataType.randomValue()!!

    // then
    assert(uriDataType.validate(randomUri).isSuccess())
  }

  @Test
  fun `should generate an https URI`() {
    // given
    val uriDataType = uriType()

    // when
    val randomUri = uriDataType.randomValue()!!

    // then
    assert(randomUri.startsWith("https://"))
  }

  @Test
  fun `validates a string with enum values`() {
    // given
    val uriDataType = uriType(enum = listOf("https://api.example.com", "https://service.example.com"))

    // when
    val result = uriDataType.validate("https://api.example.com")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate a string outside enum values`() {
    // given
    val uriDataType = uriType(enum = listOf("https://api.example.com", "https://service.example.com"))

    // when
    val result = uriDataType.validate("https://other.example.com")

    // then
    assert(result.isFailure())
  }

  @Test
  fun `generates random value with enum values`() {
    // given
    val enum = listOf("https://api.example.com", "https://service.example.com")
    val uriDataType = uriType(enum = enum)

    // when
    val result = uriDataType.randomValue()

    // then
    assert(enum.contains(result))
  }
}