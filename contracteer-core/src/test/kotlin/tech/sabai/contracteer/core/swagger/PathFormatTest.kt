package tech.sabai.contracteer.core.swagger

import tech.sabai.contracteer.core.assertFailure
import kotlin.test.Test

class PathFormatTest {

  @Test
  fun `error path uses bracket notation for parameter names`() {
    val errors = loadSpec()

    assert(errors.any { it.startsWith("POST /users/{id}: 'request.query[filter]'") }) {
      "Expected bracket notation for query parameter. Actual errors:\n${errors.joinToString("\n")}"
    }
  }

  @Test
  fun `error path has no schema names for request body properties`() {
    val errors = loadSpec()

    assert(errors.any { it.startsWith("POST /users/{id}: 'request.body.address.city'") }) {
      "Expected clean property path without schema names. Actual errors:\n${errors.joinToString("\n")}"
    }
  }

  @Test
  fun `error path has no duplicate segments and no schema names for response body properties`() {
    val errors = loadSpec()

    assert(errors.any { it.startsWith("POST /users/{id}: 'response[200].body.tags.label'") }) {
      "Expected clean property path without duplicates or schema names. Actual errors:\n${errors.joinToString("\n")}"
    }
  }

  @Test
  fun `error path includes content type in brackets for multi-content response body`() {
    val errors = loadMultiContentSpec()

    assert(errors.any { it.startsWith("POST /documents: 'response[200].body[application/xml].metadata.author'") }) {
      "Expected content type in brackets for multi-content response body. Actual errors:\n${errors.joinToString("\n")}"
    }
  }

  @Test
  fun `error path includes content type in brackets for multi-content request body`() {
    val errors = loadMultiContentSpec()

    assert(errors.any { it.startsWith("POST /documents: 'request.body[application/xml].title'") }) {
      "Expected content type in brackets for multi-content request body. Actual errors:\n${errors.joinToString("\n")}"
    }
  }

  @Test
  fun `error path does not include content type for single-content response body`() {
    val errors = loadSpec()

    assert(errors.any { it.startsWith("POST /users/{id}: 'response[200].body.tags.label'") }) {
      "Expected no content type qualifier for single-content response body. Actual errors:\n${errors.joinToString("\n")}"
    }
    assert(errors.none { it.contains("body[application/json]") }) {
      "Single-content body should not have content type qualifier. Actual errors:\n${errors.joinToString("\n")}"
    }
  }

  @Test
  fun `scenario error path includes content type for multi-content request body`() {
    val errors = loadMultiContentScenarioSpec()

    assert(errors.any { it.contains("request.body[text/plain]") }) {
      "Expected content type in scenario request body error path. Actual errors:\n${errors.joinToString("\n")}"
    }
  }

  @Test
  fun `scenario error path includes content type for multi-content response body`() {
    val errors = loadMultiContentScenarioSpec()

    assert(errors.any { it.contains("response.body[text/plain]") }) {
      "Expected content type in scenario response body error path. Actual errors:\n${errors.joinToString("\n")}"
    }
  }

  private fun loadSpec() =
    OpenApiLoader.loadOperations("src/test/resources/error/error_path_format.yaml").assertFailure()

  private fun loadMultiContentSpec() =
    OpenApiLoader.loadOperations("src/test/resources/error/error_path_format_multi_content.yaml").assertFailure()

  private fun loadMultiContentScenarioSpec() =
    OpenApiLoader.loadOperations("src/test/resources/error/error_path_format_multi_content_scenario.yaml").assertFailure()
}
