package tech.sabai.contracteer.core.swagger

import tech.sabai.contracteer.core.assertFailure
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
  fun `fails when path parameter is not required`() {
    // when
    val result = OpenApiLoader.loadOperations("src/test/resources/error/path_parameter_required_error.yaml")

    // then
    result.assertFailure()
  }

  @Test
  fun `fails when response 204 declares a body`() {
    // when
    val result = OpenApiLoader.loadOperations("src/test/resources/error/bodyless_204_with_body.yaml")

    // then
    val errors = result.assertFailure()
    assert(errors.any { it.contains("204") && it.contains("MUST NOT") }) { "Expected error about 204 bodyless but got: $errors" }
  }

  @Test
  fun `fails when response 205 declares a body`() {
    // when
    val result = OpenApiLoader.loadOperations("src/test/resources/error/bodyless_205_with_body.yaml")

    // then
    val errors = result.assertFailure()
    assert(errors.any { it.contains("205") && it.contains("MUST NOT") }) { "Expected error about 205 bodyless but got: $errors" }
  }

  @Test
  fun `fails when response 304 declares a body`() {
    // when
    val result = OpenApiLoader.loadOperations("src/test/resources/error/bodyless_304_with_body.yaml")

    // then
    val errors = result.assertFailure()
    assert(errors.any { it.contains("304") && it.contains("MUST NOT") }) { "Expected error about 304 bodyless but got: $errors" }
  }

  @Test
  fun `fails when 1xx response declares a body`() {
    // when
    val result = OpenApiLoader.loadOperations("src/test/resources/error/bodyless_1xx_with_body.yaml")

    // then
    val errors = result.assertFailure()
    assert(errors.any { it.contains("100") && it.contains("MUST NOT") }) { "Expected error about 1xx bodyless but got: $errors" }
  }

  @Test
  fun `fails when HEAD response declares a body`() {
    // when
    val result = OpenApiLoader.loadOperations("src/test/resources/error/head_with_response_body.yaml")

    // then
    val errors = result.assertFailure()
    assert(errors.any { it.contains("HEAD") && it.contains("MUST NOT") }) { "Expected error about HEAD bodyless but got: $errors" }
  }

  @Test
  fun `fails when non 400 scenario example violates schema`() {
    // when
    val result = OpenApiLoader.loadOperations("src/test/resources/error/invalid_non_400_examples.yaml")

    // then
    val errors = result.assertFailure()
    assert(errors.size == 5) { "Expected 5 validation errors but got ${errors.size}: $errors" }
  }
}
