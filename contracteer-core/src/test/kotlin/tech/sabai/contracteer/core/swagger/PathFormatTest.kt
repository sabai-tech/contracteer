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

  private fun loadSpec() =
    OpenApiLoader.loadOperations("src/test/resources/error/error_path_format.yaml").assertFailure()
}
