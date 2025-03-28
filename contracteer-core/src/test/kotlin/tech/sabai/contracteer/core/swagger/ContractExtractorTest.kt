package tech.sabai.contracteer.core.swagger

import org.mockserver.integration.ClientAndServer
import org.mockserver.integration.ClientAndServer.startClientAndServer
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.MediaType.APPLICATION_JSON
import tech.sabai.contracteer.core.contract.ContractParameter
import tech.sabai.contracteer.core.datatype.IntegerDataType
import tech.sabai.contracteer.core.datatype.ObjectDataType
import tech.sabai.contracteer.core.datatype.StringDataType
import tech.sabai.contracteer.core.normalize
import java.io.File
import kotlin.test.Test


class ContractExtractorTest {

  @Test
  fun `loading contracts fails when file does not exist`() {
    // when
    val result = OpenApiLoader.loadContracts("src/test/resources/not_found.yaml")

    // then
    assert(result.isFailure())
    assert(result.errors().first().contains("file not found"))
  }

  @Test
  fun `loading contracts fails when URl does not exist`() {
    // given
    val server = startServer()

    // when
    val result = OpenApiLoader.loadContracts("http://localhost:8080/not_found.yaml")

    // then
    assert(result.isFailure())
    server.stop()
  }

  @Test
  fun `loading contracts succeeds from an existing remote url`() {
    // given
    val server = startServer()

    // when
    val contracts = OpenApiLoader.loadContracts("http://localhost:${server.port}/oas3.yaml")

    // then
    assert(contracts.isSuccess())
    assert(contracts.value!!.size == 2)
    server.stop()
  }

  @Test
  fun `supports $ref`() {
    // when
    val contractResults = OpenApiLoader.loadContracts("src/test/resources/use_references.yaml")
    val contract = contractResults.value!!.first()

    // then
    assert(contractResults.value.size == 1)
    //   Request
    assert(contract.request.method == "GET")
    assert(contract.request.path == "/products/{id}")

    //    Parameters
    assert(contract.request.pathParameters.size == 1)
    assert(contract.request.pathParameters.first().name == "id")
    assert(contract.request.pathParameters.first().isRequired)
    assert(contract.request.pathParameters.first().dataType is IntegerDataType)
    assert(contract.request.pathParameters.first().example!!.normalizedValue == 999.normalize())

    // Body
    assert(contract.request.body != null)
    assert(contract.request.body!!.contentType.value == "application/json")
    assert(contract.request.body.dataType is ObjectDataType)
    assert((contract.request.body.dataType as ObjectDataType).properties["prop1"] is StringDataType)

    //   Response
    assert(contract.response.statusCode == 200)
    //      Headers
    assert(contract.response.headers.asMap()["x-optional"]!!.isRequired.not())
    assert(contract.response.headers.asMap()["x-optional"]!!.dataType is IntegerDataType)
    assert(contract.response.headers.asMap()["x-optional"]!!.example!!.normalizedValue == 999.normalize())
  }

  @Test
  fun `auto generated contract for 2xx status code`() {
    // when
    val contractResults = OpenApiLoader.loadContracts("src/test/resources/2xx_auto_generated_contract.yaml")
    val contract = contractResults.value!!.first()

    // then
    assert(contractResults.value.size == 1)
    //   Request
    assert(contract.request.method == "GET")
    assert(contract.request.path == "/products/{id}")
    //      Path Parameters
    assert(contract.request.pathParameters.size == 1)
    assert(contract.request.pathParameters.first().name == "id")
    assert(contract.request.pathParameters.first().isRequired)
    assert(contract.request.pathParameters.first().dataType is IntegerDataType)
    //      Query Parameters
    assert(contract.request.queryParameters.size == 1)
    assert(contract.request.queryParameters.first().name == "query")
    assert(contract.request.queryParameters.first().isRequired.not())
    assert(contract.request.queryParameters.first().dataType is StringDataType)
    //      Headers
    assert(contract.request.headers.size == 1)
    assert(contract.request.headers.first().name == "header")
    assert(contract.request.headers.first().isRequired)
    assert(contract.request.headers.first().dataType is StringDataType)
    //      Cookies
    assert(contract.request.cookies.size == 1)
    assert(contract.request.cookies.first().name == "cookie")
    assert(contract.request.cookies.first().isRequired.not())
    assert(contract.request.cookies.first().dataType is StringDataType)
    //      Body
    assert(contract.request.body != null)
    assert(contract.request.body!!.contentType.value == "application/json")
    assert(contract.request.body.dataType is ObjectDataType)
    assert((contract.request.body.dataType as ObjectDataType).properties["prop1"] is StringDataType)

    //   Response
    assert(contract.response.statusCode == 200)
    //      Headers
    assert(contract.response.headers.asMap()["x-optional"]!!.isRequired.not())
    assert(contract.response.headers.asMap()["x-optional"]!!.dataType is IntegerDataType)
    assert(contract.response.headers.asMap()["x-required"]!!.isRequired)
    assert(contract.response.headers.asMap()["x-required"]!!.dataType is IntegerDataType)
  }

  @Test
  fun `loading contracts fails when path parameter is set not required`() {
    // when
    val result = OpenApiLoader.loadContracts("path_parameter_required_error.yaml")
    // then
    assert(result.isFailure())
  }

  @Test
  fun `generate contract for each combination of request-response content-type`() {
    // when
    val contracts = OpenApiLoader.loadContracts("src/test/resources/multiple_content_type.yaml").value!!
    // then
    assert(contracts.size == 4)
    assert(contracts.map { it.request.body!!.contentType.value to it.response.body!!.contentType.value }
             .containsAll(listOf(
               "application/vnd.mycompany.myapp.v2+json" to "application/json",
               "application/vnd.mycompany.myapp.v2+json" to "application/vnd.mycompany.myapp.v2+json",
               "application/json" to "application/json",
               "application/json" to "application/vnd.mycompany.myapp.v2+json"
             )))
  }

  @Test
  fun `do not generate contract with only response example`() {
    // when
    val contracts =
      OpenApiLoader.loadContracts("src/test/resources/examples/response_body_example_only.yaml").value!!
    // then
    assert(contracts.none { it.exampleKey != null })
  }

  @Test
  fun `do not generate contract with only parameter example`() {
    // when
    val contracts = OpenApiLoader.loadContracts("src/test/resources/examples/parameter_example_only.yaml").value!!
    // then
    assert(contracts.none { it.exampleKey != null })
  }

  @Test
  fun `do not generate contract with only request body example`() {
    // when
    val contracts =
      OpenApiLoader.loadContracts("src/test/resources/examples/request_body_example_only.yaml").value!!
    // then
    assert(contracts.none { it.exampleKey != null })
  }

  @Test
  fun `mix auto generated 2xx_contract with example based contract`() {
    // when
    val contracts =
      OpenApiLoader.loadContracts("src/test/resources/examples/mix_2xx_auto_generated_contract_and_example_based_contract.yaml").value!!
    // then
    assert(contracts.size == 2)
    assert(contracts.filter { it.exampleKey == null }.size == 1)
    assert(contracts.filter { it.exampleKey == "NOT_FOUND" }.size == 1)
    assert(contracts.first { it.exampleKey == "NOT_FOUND" }.request.pathParameters.first().value() == 999.normalize())
    assert(contracts.first { it.exampleKey == "NOT_FOUND" }.response.statusCode == 404)
    assert(contracts.first { it.exampleKey == "NOT_FOUND" }.response.body!!.content() == mapOf("error" to "NOT FOUND"))
  }

  @Test
  fun `generate contracts with multiple examples`() {
    // when
    val contracts = OpenApiLoader.loadContracts("src/test/resources/examples/multiple_examples.yaml").value!!
    // then
    assert(contracts.size == 2)
    assert(contracts.find { it.exampleKey == "GET_DETAILS" } != null)
    assert(contracts.find { it.exampleKey == "NOT_FOUND" } != null)
  }

  @Test
  fun `generate contracts with multiple content-type and same example key for all`() {
    // when
    val contracts =
      OpenApiLoader.loadContracts("src/test/resources/examples/multiple_content_type_with_same_example.yaml").value!!
    // then
    assert(contracts.size == 4)
    assert(contracts.map { it.request.body!!.contentType.value to it.response.body!!.contentType.value }
             .containsAll(listOf(
               "application/vnd.mycompany.myapp.v2+json" to "application/json",
               "application/vnd.mycompany.myapp.v2+json" to "application/vnd.mycompany.myapp.v2+json",
               "application/json" to "application/json",
               "application/json" to "application/vnd.mycompany.myapp.v2+json"
             )))
  }

  @Test
  fun `does not generate contracts when 2xx response is missing`() {
    // when
    val result = OpenApiLoader.loadContracts("src/test/resources/missing_2xx_response.yaml")

    // then
    assert(result.isFailure())
    assert(result.errors().size == 1)
  }

  @Test
  fun `does not load Open API when examples are invalid`() {
    // when
    val result = OpenApiLoader.loadContracts("src/test/resources/examples/invalid_examples.yaml")

    // then
    assert(result.isFailure())
    assert(result.errors().size == 5)
    assert(result.errors().containsAll(listOf(
      "POST /products [example: 'CREATE_PRODUCT'] | request ▸ 'body.id': Type mismatch, expected type 'integer'",
      "GET /products/{id} -> 200 [example: 'GET_DETAILS' ] | response ▸ 'body.quantity': Type mismatch, expected type 'integer'",
      "GET /products/{id} -> 404 [example: 'ASYNC' ] | response ▸ 'header.location': Type mismatch, expected type 'string'",
      "GET /products/{id} [example: 'NOT_FOUND'] | request ▸ 'path.id': Type mismatch, expected type 'integer'",
      "GET /products/{id} -> 404 [example: 'NOT_FOUND' ] | response ▸ 'body.error': Type mismatch, expected type 'string'"
    )))

  }

  @Test
  fun `does not fail when loading unsupported OAS feature`() {
    // when
    val result = OpenApiLoader.loadContracts("src/test/resources/unsupported_oas_features.yaml")

    // then
    assert(result.isSuccess())
    assert(result.value!!.isEmpty())
  }
}

private fun List<ContractParameter>.asMap() = associateBy { it.name }

fun startServer(): ClientAndServer {
  val mockServer = startClientAndServer()
  mockServer
    .`when`(
      request()
        .withMethod("GET")
        .withPath("/oas3.yaml")
    )
    .respond(
      response()
        .withStatusCode(200)
        .withBody(
          File("src/test/resources/examples/mix_2xx_auto_generated_contract_and_example_based_contract.yaml").readText(),
          APPLICATION_JSON
        )
    )
  return mockServer
}
