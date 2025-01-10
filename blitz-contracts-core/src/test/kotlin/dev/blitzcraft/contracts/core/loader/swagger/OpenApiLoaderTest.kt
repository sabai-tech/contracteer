package dev.blitzcraft.contracts.core.loader.swagger

import dev.blitzcraft.contracts.core.Result
import dev.blitzcraft.contracts.core.contract.Contract
import java.nio.file.Path
import kotlin.test.Test

class OpenApiLoaderTest {

  @Test
  fun `does not generate contracts when 2xx response is missing`() {
    // when
    val result: Result<List<Contract>> = Path.of("src/test/resources/missing_2xx_response.yaml").generateContracts()

    // then
    assert(result.isFailure())
    assert(result.errors().size == 2)
  }

  @Test
  fun `does not load Open API when examples are invalid`() {
    // when
    val result = Path.of("src/test/resources/examples/invalid_examples.yaml").generateContracts()

    // then
    assert(result.isFailure())
    assert(result.errors().size == 5)
    assert(result.errors().containsAll(listOf(
      "POST /products (application/json) -> 201 (application/json) with example 'CREATE_PRODUCT', request body 'id': Wrong type. Expected type: integer",
      "GET /products/{id}  -> 200 (application/json) with example 'GET_DETAILS', response body 'quantity': Wrong type. Expected type: integer",
      "GET /products/{id}  -> 404 (application/json) with example 'ASYNC', response header 'location': Wrong type. Expected type: string",
      "GET /products/{id}  -> 404 (application/json) with example 'NOT_FOUND', request path parameter 'id': Wrong type. Expected type: integer",
      "GET /products/{id}  -> 404 (application/json) with example 'NOT_FOUND', request path parameter 'id': Wrong type. Expected type: integer",
      "GET /products/{id}  -> 404 (application/json) with example 'NOT_FOUND', response body 'error': Wrong type. Expected type: string"
    )))
  }
}



