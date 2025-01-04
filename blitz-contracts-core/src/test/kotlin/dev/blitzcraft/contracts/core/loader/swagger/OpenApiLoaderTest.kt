package dev.blitzcraft.contracts.core.loader.swagger

import java.nio.file.Path
import kotlin.test.Test

class OpenApiLoaderTest {

  @Test
  fun `does not load Open API when 2xx response is missing`() {
    // when
    val result = Path.of("src/test/resources/missing_2xx_response.yaml").loadOpenApiSpec()

    // then
    assert(result.contracts.isEmpty())
    assert(result.errors.size == 2)
  }

  @Test
  fun `does not load Open API when examples are invalid`() {
    // when
    val result = Path.of("src/test/resources/examples/invalid_examples.yaml").loadOpenApiSpec()

    // then
    assert(result.contracts.isEmpty())
    assert(result.errors.size == 5)
    assert(result.errors.containsAll(listOf(
      "POST /products (application/json) -> 201 (application/json) with example 'CREATE_PRODUCT', request body 'id': Wrong type. Expected type: integer",
      "GET /products/{id}  -> 200 (application/json) with example 'GET_DETAILS', response body 'quantity': Wrong type. Expected type: integer",
      "GET /products/{id}  -> 404 (application/json) with example 'ASYNC', response header 'location': Wrong type. Expected type: string",
      "GET /products/{id}  -> 404 (application/json) with example 'NOT_FOUND', request path parameter 'id': Wrong type. Expected type: integer",
      "GET /products/{id}  -> 404 (application/json) with example 'NOT_FOUND', request path parameter 'id': Wrong type. Expected type: integer",
      "GET /products/{id}  -> 404 (application/json) with example 'NOT_FOUND', response body 'error': Wrong type. Expected type: string"
    )))
  }
}



