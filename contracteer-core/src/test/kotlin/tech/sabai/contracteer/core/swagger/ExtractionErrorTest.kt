package tech.sabai.contracteer.core.swagger

import tech.sabai.contracteer.core.assertFailure
import tech.sabai.contracteer.core.assertSuccess
import kotlin.test.Test

class ExtractionErrorTest {

  @Test
  fun `fails when file does not exist`() {
    // when
    val result = OpenApiLoader.loadOperations("src/test/resources/error/not_found.yaml")

    // then
    val errors = result.assertFailure()
    assert(errors.first().contains("file not found"))
  }

  @Test
  fun `fails when no 2xx response exists`() {
    // when
    val result = OpenApiLoader.loadOperations("src/test/resources/error/missing_2xx_response.yaml")

    // then
    result.assertFailure()
  }

  @Test
  fun `fails when path parameter is not required`() {
    // when
    val result = OpenApiLoader.loadOperations("src/test/resources/error/path_parameter_required_error.yaml")

    // then
    result.assertFailure()
  }

  @Test
  fun `fails when non 400 scenario example violates schema`() {
    // when
    val result = OpenApiLoader.loadOperations("src/test/resources/error/invalid_non_400_examples.yaml")

    // then
    val errors = result.assertFailure()
    assert(errors.size == 5) { "Expected 5 validation errors but got ${errors.size}: $errors" }
  }

  @Test
  fun `rejects JSON content type with non structured schema`() {
    // when
    val result = OpenApiLoader.loadOperations("src/test/resources/error/json_content_type_with_non_structured_schema.yaml")

    // then
    result.assertFailure()
  }

  @Test
  fun `does not fail when loading unsupported OAS features`() {
    // when
    val result = OpenApiLoader.loadOperations("src/test/resources/error/unsupported_oas_features.yaml")

    // then
    val operations = result.assertSuccess()
    assert(operations.isEmpty())
  }

  @Test
  fun `filters scenarios with XML content types`() {
    // when
    val result = OpenApiLoader.loadOperations("src/test/resources/error/xml_scenarios.yaml")

    // then
    val operations = result.assertSuccess()
    val scenarios = operations.first().scenarios
    assert(operations.size == 1)
    assert(scenarios.all { it.response.body?.contentType?.isXml() != true })
    assert(scenarios.isNotEmpty())
  }
}
