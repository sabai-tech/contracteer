package dev.blitzcraft.contracts.core.loader.swagger

import dev.blitzcraft.contracts.core.contract.ContractParameter
import dev.blitzcraft.contracts.core.normalize
import dev.blitzcraft.contracts.core.datatype.*
import kotlin.io.path.Path
import kotlin.test.Test


class ContractExtractorTest {

  @Test
  fun `auto generated contract with all data types`() {
    // when
    val contracts =
      Path("src/test/resources/2xx_auto_generated_contract_with_all_datatypes.yaml").loadOpenApiSpec().contracts
    val contract = contracts.first()

    // then
    assert(contracts.size == 1)
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
    assert(contract.request.body!!.contentType == "application/json")
    assert(contract.request.body!!.dataType is ObjectDataType)
    assert((contract.request.body!!.dataType as ObjectDataType).name == "Inline Schema")
    assert((contract.request.body!!.dataType as ObjectDataType).properties["prop1"] is StringDataType)

    //   Response
    assert(contract.response.statusCode == 200)
    //      Headers
    assert(contract.response.headers.asMap()["x-optional"]!!.isRequired.not())
    assert(contract.response.headers.asMap()["x-optional"]!!.dataType is IntegerDataType)
    assert(contract.response.headers.asMap()["x-required"]!!.isRequired)
    assert(contract.response.headers.asMap()["x-required"]!!.dataType is IntegerDataType)
    //      Body
    assert(contract.response.hasBody())
    assert(contract.response.body!!.contentType == "application/json")
    assert(contract.response.body!!.dataType.name == "product_details")
    assert(contract.response.body!!.dataType is ObjectDataType)
    assert(contract.response.body!!.dataType.asObjectDataType().properties["boolean"] is BooleanDataType)
    assert(contract.response.body!!.dataType.asObjectDataType().properties["integer"] is IntegerDataType)
    assert(contract.response.body!!.dataType.asObjectDataType().properties["number"] is NumberDataType)
    assert(contract.response.body!!.dataType.asObjectDataType().properties["array"] is ArrayDataType)
    assert((contract.response.body!!.dataType.asObjectDataType().properties["array"] as ArrayDataType).itemDataType is IntegerDataType)
    assert(contract.response.body!!.dataType.asObjectDataType().properties["string"] is StringDataType)
    assert(contract.response.body!!.dataType.asObjectDataType().properties["email"] is EmailDataType)
    assert(contract.response.body!!.dataType.asObjectDataType().properties["date"] is DateDataType)
    assert(contract.response.body!!.dataType.asObjectDataType().properties["dateTime"] is DateTimeDataType)
    assert(contract.response.body!!.dataType.asObjectDataType().properties["byte"] is Base64DataType)
    assert(contract.response.body!!.dataType.asObjectDataType().properties["binary"] is StringDataType)
    assert(contract.response.body!!.dataType.asObjectDataType().properties["binary"]!!.openApiType == "string/binary")
    assert(contract.response.body!!.dataType.asObjectDataType().properties["password"] is StringDataType)
    assert(contract.response.body!!.dataType.asObjectDataType().properties["password"]!!.openApiType == "string/password")
    assert(contract.response.body!!.dataType.asObjectDataType().properties["uuid"] is UuidDataType)
    assert(contract.response.body!!.dataType.asObjectDataType().properties["oneOf"] is OneOfDataType)
  }

  @Test
  fun `generate contract for each combination of request-response content-type`() {
    // when
    val contracts = Path("src/test/resources/multiple_content_type.yaml").loadOpenApiSpec().contracts
    // then
    assert(contracts.size == 4)
    assert(contracts.map { it.request.body!!.contentType to it.response.body!!.contentType }
             .containsAll(listOf(
               "application/xml" to "application/json",
               "application/xml" to "application/xml",
               "application/json" to "application/json",
               "application/json" to "application/xml"
             )))
  }

  @Test
  fun `do not generate contract with only response example`() {
    // when
    val contracts =
      Path("src/test/resources/examples/response_body_example_only.yaml").loadOpenApiSpec().contracts
    // then
    assert(contracts.none { it.exampleKey != null })
  }

  @Test
  fun `do not generate contract with only parameter example`() {
    // when
    val contracts = Path("src/test/resources/examples/parameter_example_only.yaml").loadOpenApiSpec().contracts
    // then
    assert(contracts.none { it.exampleKey != null })
  }

  @Test
  fun `do not generate contract with only request body example`() {
    // when
    val contracts =
      Path("src/test/resources/examples/request_body_example_only.yaml").loadOpenApiSpec().contracts
    // then
    assert(contracts.none { it.exampleKey != null })
  }

  @Test
  fun `mix auto generated 2xx_contract with example based contract`() {
    // when
    val contracts =
      Path("src/test/resources/examples/mix_2xx_auto_generated_contract_and_example_based_contract.yaml").loadOpenApiSpec().contracts
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
    val contracts = Path("src/test/resources/examples/multiple_examples.yaml").loadOpenApiSpec().contracts
    // then
    assert(contracts.size == 4)
    assert(contracts.find { it.exampleKey == "GET_DETAILS" } != null)
    assert(contracts.find { it.exampleKey == "NOT_FOUND" } != null)
    assert(contracts.find { it.exampleKey == "CREATE_PRODUCT" } != null)
    assert(contracts.find { it.exampleKey == "ASYNC" } != null)

  }

  @Test
  fun `generate contracts with multiple content-type and same example key for all`() {
    // when
    val contracts =
      Path("src/test/resources/examples/multiple_content_type_with_same_example.yaml").loadOpenApiSpec().contracts
    // then
    assert(contracts.size == 4)
    assert(contracts.map { it.request.body!!.contentType to it.response.body!!.contentType }
             .containsAll(listOf(
               "application/xml" to "application/json",
               "application/xml" to "application/xml",
               "application/json" to "application/json",
               "application/json" to "application/xml"
             )))
  }
}

private fun List<ContractParameter>.asMap() = associateBy { it.name }
private fun DataType<*>.asObjectDataType() = this as ObjectDataType
