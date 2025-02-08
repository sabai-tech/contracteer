package tech.sabai.contracteer.core.swagger

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.contract.Contract
import java.nio.file.Path
import kotlin.test.Test

class OpenApiLoaderTest {

  @Test
  fun `does not generate contracts when 2xx response is missing`() {
    // when
    val result: Result<List<Contract>> = Path.of("src/test/resources/missing_2xx_response.yaml").loadContracts()

    // then
    assert(result.isFailure())
    assert(result.errors().size == 1)
  }

  @Test
  fun `does not load Open API when examples are invalid`() {
    // when
    val result = Path.of("src/test/resources/examples/invalid_examples.yaml").loadContracts()

    // then
    assert(result.isFailure())
    assert(result.errors().size == 5)
    assert(result.errors().containsAll(listOf(
      "path: /products, method: POST, example: CREATE_PRODUCT, request body -> 'id': Wrong type. Expected type: integer",
      "path: /products/{id}, method: GET, response status code: 200, example: GET_DETAILS, body -> 'quantity': Wrong type. Expected type: integer",
      "path: /products/{id}, method: GET, response status code: 404, example: ASYNC, header -> 'location': Wrong type. Expected type: string",
      "path: /products/{id}, method: GET, example: NOT_FOUND, request path parameter -> 'id': Wrong type. Expected type: integer",
      "path: /products/{id}, method: GET, response status code: 404, example: NOT_FOUND, body -> 'error': Wrong type. Expected type: string",
    )))
  }

  @Test
  fun `does not fail when loading unsupported OAS feature`() {
    // when
    val result = Path.of("src/test/resources/unsupported_oas_features.yaml").loadContracts()

    // then
    assert(result.isSuccess())
    assert(result.value!!.isEmpty())
  }
}



