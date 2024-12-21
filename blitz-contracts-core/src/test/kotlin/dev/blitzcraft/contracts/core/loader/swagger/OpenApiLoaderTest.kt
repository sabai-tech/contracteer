package dev.blitzcraft.contracts.core.loader.swagger

import java.nio.file.Path
import kotlin.test.Test

class OpenApiLoaderTest {

  @Test
  fun `does not load Open API when 2xx response is missing`() {
    // when
    val result = Path.of("src/test/resources/api_missing_2xx_response.yaml").loadOpenApiSpec()

    // then
    assert(result.contracts.isEmpty())
    assert(result.errors.size == 2)
  }

  @Test
  fun `does not load Open API when examples are invalid`() {
    // when
    val result = Path.of("src/test/resources/examples/validate_multiple_examples.yaml").loadOpenApiSpec()

    // then
    assert(result.contracts.isEmpty())
    assert(result.errors.size == 6)
    assert(result.errors.all {
      it.startsWith("path: /products, method: POST, request body: application/json, example: CREATE_PRODUCT -> ") ||
      it.startsWith("path: /products/{id}, method: GET, request parameter: id, example: GET_DETAILS_2 -> ") ||
      it.startsWith("path: /products/{id}, method: GET, request parameter: id, example: NOT_FOUND -> ") ||
      it.startsWith("path: /products/{id}, method: GET, response status code: 200, content: application/json, example: GET_DETAILS -> ") ||
      it.startsWith("path: /products/{id}, method: GET, response status code: 404, content: application/json, example: NOT_FOUND -> ") ||
      it.startsWith("path: /products/{id}, method: GET, response status code: 404, header: location, example: ASYNC -> ")
    })
  }
}



