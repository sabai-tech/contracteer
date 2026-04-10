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
  fun `fails when non 400 scenario example violates schema`() {
    // when
    val result = OpenApiLoader.loadOperations("src/test/resources/error/invalid_non_400_examples.yaml")

    // then
    val errors = result.assertFailure()
    assert(errors.size == 5) { "Expected 5 validation errors but got ${errors.size}: $errors" }
  }
}
