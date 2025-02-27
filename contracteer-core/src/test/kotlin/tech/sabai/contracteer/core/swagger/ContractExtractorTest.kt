package tech.sabai.contracteer.core.swagger

import tech.sabai.contracteer.core.contract.ContractParameter
import tech.sabai.contracteer.core.datatype.*
import tech.sabai.contracteer.core.normalize
import kotlin.io.path.Path
import kotlin.test.Test


class ContractExtractorTest {

  @Test
  fun `auto generated contract with all data types`() {
    // when
    val contractResults =
      Path("src/test/resources/2xx_auto_generated_contract_with_all_datatypes.yaml").loadContracts()
    val contract = contractResults.value!!.first()

    // then
    assert(contractResults.value!!.size == 1)
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
    assert(contract.response.body!!.contentType.value == "application/json")
    assert(contract.response.body!!.dataType.name == "product_details")
    assert(contract.response.body!!.dataType is ObjectDataType)
    assert(contract.response.body!!.dataType.asObjectDataType().properties["allOf"] is AllOfDataType)

    val oneOf = contract.response.body!!.dataType.asObjectDataType().properties["oneOf"]
    assert(oneOf is OneOfDataType)
    assert((oneOf as OneOfDataType).discriminator!!.propertyName == "discriminator")
    assert(oneOf.discriminator!!.mapping["obj1"]!!.name == "object1")

    val anyOf = contract.response.body!!.dataType.asObjectDataType().properties["anyOf"]
    assert(anyOf is AnyOfDataType)
    assert((anyOf as AnyOfDataType).discriminator!!.propertyName == "discriminator")
    assert(anyOf.discriminator!!.mapping["obj1"]!!.name == "object1")
  }

  @Test
  fun `generate contract for each combination of request-response content-type`() {
    // when
    val contracts = Path("src/test/resources/multiple_content_type.yaml").loadContracts().value!!
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
      Path("src/test/resources/examples/response_body_example_only.yaml").loadContracts().value!!
    // then
    assert(contracts.none { it.exampleKey != null })
  }

  @Test
  fun `do not generate contract with only parameter example`() {
    // when
    val contracts = Path("src/test/resources/examples/parameter_example_only.yaml").loadContracts().value!!
    // then
    assert(contracts.none { it.exampleKey != null })
  }

  @Test
  fun `do not generate contract with only request body example`() {
    // when
    val contracts =
      Path("src/test/resources/examples/request_body_example_only.yaml").loadContracts().value!!
    // then
    assert(contracts.none { it.exampleKey != null })
  }

  @Test
  fun `mix auto generated 2xx_contract with example based contract`() {
    // when
    val contracts =
      Path("src/test/resources/examples/mix_2xx_auto_generated_contract_and_example_based_contract.yaml").loadContracts().value!!
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
    val loadContracts = Path("src/test/resources/examples/multiple_examples.yaml").loadContracts()
    val contracts = loadContracts.value!!
    // then
    assert(contracts.size == 2)
    assert(contracts.find { it.exampleKey == "GET_DETAILS" } != null)
    assert(contracts.find { it.exampleKey == "NOT_FOUND" } != null)
  }

  @Test
  fun `generate contracts with multiple content-type and same example key for all`() {
    // when
    val loadContracts = Path("src/test/resources/examples/multiple_content_type_with_same_example.yaml").loadContracts()
    val contracts = loadContracts.value!!
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
}

private fun List<ContractParameter>.asMap() = associateBy { it.name }
private fun DataType<*>.asObjectDataType() = this as ObjectDataType
