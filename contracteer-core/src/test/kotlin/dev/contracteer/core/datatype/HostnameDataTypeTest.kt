package dev.contracteer.core.datatype

import org.junit.jupiter.api.Test
import dev.contracteer.core.dsl.hostnameType

class HostnameDataTypeTest {

  @Test
  fun `validates a value of type string representing a hostname`() {
    // given
    val hostnameDataType = hostnameType()

    // when
    val result = hostnameDataType.validate("example.com")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validates a single-label hostname`() {
    // given
    val hostnameDataType = hostnameType()

    // when
    val result = hostnameDataType.validate("localhost")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validates a hostname with hyphens inside labels`() {
    // given
    val hostnameDataType = hostnameType()

    // when
    val result = hostnameDataType.validate("foo-bar.example-site.com")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate a hostname with a label starting with a hyphen`() {
    // given
    val hostnameDataType = hostnameType()

    // when
    val result = hostnameDataType.validate("-foo.example.com")

    // then
    assert(result.isFailure())
  }

  @Test
  fun `does not validate a hostname with a label ending with a hyphen`() {
    // given
    val hostnameDataType = hostnameType()

    // when
    val result = hostnameDataType.validate("foo-.example.com")

    // then
    assert(result.isFailure())
  }

  @Test
  fun `does not validate a hostname with a label longer than 63 characters`() {
    // given
    val hostnameDataType = hostnameType()
    val longLabel = "a".repeat(64)

    // when
    val result = hostnameDataType.validate("$longLabel.com")

    // then
    assert(result.isFailure())
  }

  @Test
  fun `does not validate a hostname longer than 253 characters`() {
    // given
    val hostnameDataType = hostnameType()
    val tooLongHostname = (1..14).joinToString(".") { "a".repeat(17) } + ".com"

    // when
    val result = hostnameDataType.validate(tooLongHostname)

    // then
    assert(tooLongHostname.length > 253)
    assert(result.isFailure())
  }

  @Test
  fun `does not validate a hostname containing invalid characters`() {
    // given
    val hostnameDataType = hostnameType()

    // when
    val result = hostnameDataType.validate("foo_bar.example.com")

    // then
    assert(result.isFailure())
  }

  @Test
  fun `does not validate an empty hostname`() {
    // given
    val hostnameDataType = hostnameType()

    // when
    val result = hostnameDataType.validate("")

    // then
    assert(result.isFailure())
  }

  @Test
  fun `validates null value if it is nullable`() {
    // given
    val hostnameDataType = hostnameType(isNullable = true)

    // when
    val result = hostnameDataType.validate(null)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate null value if it is not nullable`() {
    // given
    val hostnameDataType = hostnameType(isNullable = false)

    // when
    val result = hostnameDataType.validate(null)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `should generate a string representing a valid hostname`() {
    // given
    val hostnameDataType = hostnameType()

    // when
    val randomHostname = hostnameDataType.randomValue()!!

    // then
    assert(hostnameDataType.validate(randomHostname).isSuccess())
  }

  @Test
  fun `validates a string with enum values`() {
    // given
    val hostnameDataType = hostnameType(enum = listOf("api.example.com", "service.example.com"))

    // when
    val result = hostnameDataType.validate("api.example.com")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate a string outside enum values`() {
    // given
    val hostnameDataType = hostnameType(enum = listOf("api.example.com", "service.example.com"))

    // when
    val result = hostnameDataType.validate("other.example.com")

    // then
    assert(result.isFailure())
  }

  @Test
  fun `generates random value with enum values`() {
    // given
    val enum = listOf("api.example.com", "service.example.com")
    val hostnameDataType = hostnameType(enum = enum)

    // when
    val result = hostnameDataType.randomValue()

    // then
    assert(enum.contains(result))
  }
}