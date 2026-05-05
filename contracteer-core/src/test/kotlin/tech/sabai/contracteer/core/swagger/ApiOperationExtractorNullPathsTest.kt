package tech.sabai.contracteer.core.swagger

import io.swagger.v3.oas.models.OpenAPI
import tech.sabai.contracteer.core.assertSuccess
import kotlin.test.Test

class ApiOperationExtractorNullPathsTest {

  @Test
  fun `extracts no operations from a document whose paths block is omitted`() {
    // Given
    val openAPI = OpenAPI().apply { paths = null }

    // When
    val result = ApiOperationExtractor(emptySharedComponents()).extract(openAPI)

    // Then
    val operations = result.assertSuccess()
    assert(operations.isEmpty()) { "Expected no operations but got $operations" }
  }

  private fun emptySharedComponents() = SharedComponents(
    schemas = emptyMap(),
    parameters = emptyMap(),
    requestBodies = emptyMap(),
    headers = emptyMap(),
    examples = emptyMap(),
    responses = emptyMap()
  )
}
