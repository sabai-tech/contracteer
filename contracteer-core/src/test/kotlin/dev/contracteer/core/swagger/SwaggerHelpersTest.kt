package dev.contracteer.core.swagger

import io.swagger.v3.oas.models.SpecVersion
import io.swagger.v3.oas.models.media.JsonSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import dev.contracteer.core.Result.Companion.failure
import dev.contracteer.core.Result.Companion.success
import dev.contracteer.core.assertFailure
import dev.contracteer.core.assertSuccess
import java.math.BigDecimal
import kotlin.test.Test

class SwaggerHelpersTest {

  @Test
  fun `effectiveType returns the single non-null type from a 3 0 schema`() {
    // Given
    val schema = StringSchema().apply { specVersion = SpecVersion.V30 }

    // When
    val type = schema.effectiveType()

    // Then
    assert(type == "string") { "Expected 'string' but got '$type'" }
  }

  @Test
  fun `effectiveType returns the single non-null type from a 3 1 nullable schema`() {
    // Given
    val schema = JsonSchema().apply {
      specVersion = SpecVersion.V31
      types = linkedSetOf("string", "null")
    }

    // When
    val type = schema.effectiveType()

    // Then
    assert(type == "string") { "Expected 'string' but got '$type'" }
  }

  @Test
  fun `effectiveType returns null for a 3 1 multi-type schema with no null branch`() {
    // Given
    val schema = JsonSchema().apply {
      specVersion = SpecVersion.V31
      types = linkedSetOf("string", "integer")
    }

    // When
    val type = schema.effectiveType()

    // Then
    assert(type == null) { "Expected null but got '$type'" }
  }

  @Test
  fun `effectiveType returns null when types is missing`() {
    // Given
    val schema = JsonSchema().apply { specVersion = SpecVersion.V31 }

    // When
    val type = schema.effectiveType()

    // Then
    assert(type == null) { "Expected null but got '$type'" }
  }

  @Test
  fun `effectiveType ignores the legacy type field on a 3 1 schema`() {
    // Given
    val schema = JsonSchema().apply {
      specVersion = SpecVersion.V31
      types = linkedSetOf("string", "integer")
      type = "string"
    }

    // When
    val type = schema.effectiveType()

    // Then
    assert(type == null) { "Expected null but got '$type' — 3.1 must not read the 3.0 'type' field" }
  }

  @Test
  fun `isNullable reads nullable in 3 0`() {
    // Given
    val schema = StringSchema().apply {
      specVersion = SpecVersion.V30
      nullable = true
    }

    // When
    val nullable = schema.isNullable()

    // Then
    assert(nullable)
  }

  @Test
  fun `isNullable defaults to false in 3 0 when nullable is not set`() {
    // Given
    val schema = StringSchema().apply { specVersion = SpecVersion.V30 }

    // When
    val nullable = schema.isNullable()

    // Then
    assert(!nullable)
  }

  @Test
  fun `isNullable detects the null branch in a 3 1 type array`() {
    // Given
    val schema = JsonSchema().apply {
      specVersion = SpecVersion.V31
      types = linkedSetOf("string", "null")
    }

    // When
    val nullable = schema.isNullable()

    // Then
    assert(nullable)
  }

  @Test
  fun `isNullable returns false for a single-type 3 1 schema`() {
    // Given
    val schema = JsonSchema().apply {
      specVersion = SpecVersion.V31
      types = linkedSetOf("string")
    }

    // When
    val nullable = schema.isNullable()

    // Then
    assert(!nullable)
  }

  @Test
  fun `isAnyType returns false for a 3 1 schema with only the null type`() {
    // given
    val schema = JsonSchema().apply {
      specVersion = SpecVersion.V31
      types = linkedSetOf("null")
    }

    // when
    val isAny = schema.isAnyType()

    // then
    assert(!isAny)
  }

  @Test
  fun `isAnyType returns true for an unconstrained 3 1 schema`() {
    // given
    val schema = JsonSchema().apply { specVersion = SpecVersion.V31 }

    // when
    val isAny = schema.isAnyType()

    // then
    assert(isAny)
  }

  @Test
  fun `effectiveExclusiveMinimum reads the boolean flag in 3 0`() {
    // Given
    val schema = Schema<Any>().apply {
      specVersion = SpecVersion.V30
      minimum = BigDecimal(5)
      exclusiveMinimum = true
    }

    // When
    val exclusive = schema.effectiveExclusiveMinimum()

    // Then
    assert(exclusive)
  }

  @Test
  fun `effectiveExclusiveMinimum returns true for 3 1 when exclusiveMinimumValue is set`() {
    // Given
    val schema = JsonSchema().apply {
      specVersion = SpecVersion.V31
      types = linkedSetOf("integer")
      exclusiveMinimumValue = BigDecimal(5)
    }

    // When
    val exclusive = schema.effectiveExclusiveMinimum()

    // Then
    assert(exclusive)
  }

  @Test
  fun `effectiveExclusiveMinimum returns false for 3 1 when only minimum is set`() {
    // Given
    val schema = JsonSchema().apply {
      specVersion = SpecVersion.V31
      types = linkedSetOf("integer")
      minimum = BigDecimal(5)
    }

    // When
    val exclusive = schema.effectiveExclusiveMinimum()

    // Then
    assert(!exclusive)
  }

  @Test
  fun `effectiveExclusiveMaximum reads the boolean flag in 3 0`() {
    // Given
    val schema = Schema<Any>().apply {
      specVersion = SpecVersion.V30
      maximum = BigDecimal(100)
      exclusiveMaximum = true
    }

    // When
    val exclusive = schema.effectiveExclusiveMaximum()

    // Then
    assert(exclusive)
  }

  @Test
  fun `effectiveExclusiveMaximum returns true for 3 1 when exclusiveMaximumValue is set`() {
    // Given
    val schema = JsonSchema().apply {
      specVersion = SpecVersion.V31
      types = linkedSetOf("integer")
      exclusiveMaximumValue = BigDecimal(100)
    }

    // When
    val exclusive = schema.effectiveExclusiveMaximum()

    // Then
    assert(exclusive)
  }

  @Test
  fun `effectiveMinimum returns the inclusive minimum in 3 0`() {
    // Given
    val schema = Schema<Any>().apply {
      specVersion = SpecVersion.V30
      minimum = BigDecimal(5)
    }

    // When
    val min = schema.effectiveMinimum()

    // Then
    assert(min == BigDecimal(5))
  }

  @Test
  fun `effectiveMinimum picks the more restrictive bound when both are set in 3 1`() {
    // Given
    val schema = JsonSchema().apply {
      specVersion = SpecVersion.V31
      types = linkedSetOf("integer")
      minimum = BigDecimal(5)
      exclusiveMinimumValue = BigDecimal(7)
    }

    // When
    val min = schema.effectiveMinimum()
    val exclusive = schema.effectiveExclusiveMinimum()

    // Then the exclusive 7 wins as the more restrictive bound
    assert(min == BigDecimal(7)) { "Expected 7 but got $min" }
    assert(exclusive) { "Expected exclusive but got inclusive" }
  }

  @Test
  fun `effectiveMinimum keeps the inclusive bound when it is more restrictive in 3 1`() {
    // Given
    val schema = JsonSchema().apply {
      specVersion = SpecVersion.V31
      types = linkedSetOf("integer")
      minimum = BigDecimal(7)
      exclusiveMinimumValue = BigDecimal(5)
    }

    // When
    val min = schema.effectiveMinimum()
    val exclusive = schema.effectiveExclusiveMinimum()

    // Then the inclusive 7 wins
    assert(min == BigDecimal(7)) { "Expected 7 but got $min" }
    assert(!exclusive) { "Expected inclusive but got exclusive" }
  }

  @Test
  fun `effectiveMaximum picks the more restrictive bound when both are set in 3 1`() {
    // Given
    val schema = JsonSchema().apply {
      specVersion = SpecVersion.V31
      types = linkedSetOf("integer")
      maximum = BigDecimal(100)
      exclusiveMaximumValue = BigDecimal(50)
    }

    // When
    val max = schema.effectiveMaximum()
    val exclusive = schema.effectiveExclusiveMaximum()

    // Then the exclusive 50 wins as the more restrictive bound
    assert(max == BigDecimal(50)) { "Expected 50 but got $max" }
    assert(exclusive)
  }

  @Test
  fun `effectiveConst returns null for a 3 0 schema`() {
    // Given
    val schema = StringSchema().apply { specVersion = SpecVersion.V30 }

    // When
    val const = schema.effectiveConst()

    // Then
    assert(const == null)
  }

  @Test
  fun `effectiveConst returns the const value for a 3 1 schema`() {
    // Given
    val schema = JsonSchema().apply {
      specVersion = SpecVersion.V31
      const = "fixed"
    }

    // When
    val const = schema.effectiveConst()

    // Then
    assert(const == "fixed") { "Expected 'fixed' but got '$const'" }
  }

  @Test
  fun `effectiveContentEncoding returns null for a 3 0 schema`() {
    // Given
    val schema = StringSchema().apply { specVersion = SpecVersion.V30 }

    // When
    val encoding = schema.effectiveContentEncoding()

    // Then
    assert(encoding == null)
  }

  @Test
  fun `effectiveContentEncoding returns the contentEncoding value for a 3 1 schema`() {
    // Given
    val schema = JsonSchema().apply {
      specVersion = SpecVersion.V31
      types = linkedSetOf("string")
      contentEncoding = "base64"
    }

    // When
    val encoding = schema.effectiveContentEncoding()

    // Then
    assert(encoding == "base64")
  }

  @Test
  fun `effectiveContentEncoding normalizes mixed-case values to lowercase`() {
    // Given
    val schema = JsonSchema().apply {
      specVersion = SpecVersion.V31
      types = linkedSetOf("string")
      contentEncoding = "Base64"
    }

    // When
    val encoding = schema.effectiveContentEncoding()

    // Then
    assert(encoding == "base64") { "Expected 'base64' but got '$encoding'" }
  }

  @Test
  fun `effectiveContentMediaType returns null for a 3 0 schema`() {
    // Given
    val schema = StringSchema().apply { specVersion = SpecVersion.V30 }

    // When
    val mediaType = schema.effectiveContentMediaType()

    // Then
    assert(mediaType == null)
  }

  @Test
  fun `effectiveContentMediaType returns the contentMediaType value for a 3 1 schema`() {
    // Given
    val schema = JsonSchema().apply {
      specVersion = SpecVersion.V31
      types = linkedSetOf("string")
      contentMediaType = "application/octet-stream"
    }

    // When
    val mediaType = schema.effectiveContentMediaType()

    // Then
    assert(mediaType == "application/octet-stream")
  }

  @Test
  fun `mapEnum applies transform to each non-null enum element`() {
    // Given
    val schema = JsonSchema().apply { enum = mutableListOf<Any?>(1, 2) }

    // When
    val result = schema.mapEnum { value -> success((value as Int) * 2) }

    // Then
    assert(result.assertSuccess() == listOf(2, 4))
  }

  @Test
  fun `mapEnum passes null enum elements through without invoking transform`() {
    // Given
    val schema = JsonSchema().apply { enum = mutableListOf<Any?>(null, null) }

    // When
    val result = schema.mapEnum<Int> { _ -> failure("transform should not run on null") }

    // Then
    assert(result.assertSuccess() == listOf(null, null))
  }

  @Test
  fun `mapEnum aggregates failures from multiple invalid elements`() {
    // Given
    val schema = JsonSchema().apply { enum = mutableListOf<Any?>("first", "second") }

    // When
    val result = schema.mapEnum<String> { value -> failure("invalid: $value") }

    // Then
    val errors = result.assertFailure()
    assert(errors.contains("invalid: first"))
    assert(errors.contains("invalid: second"))
  }
}
