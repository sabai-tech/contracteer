package dev.blitzcraft.contracts.core

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.ParseOptions
import kotlin.test.Test
import kotlin.test.assertNotNull

class ContractExtractorTest {


  @Test
  fun `generate contracts for api mixing random values and example`() {
    // given
    val api = loadApiFrom("api_mixing_random_values_and_example_for_4xx_status.yaml")
    // when
    val contracts = api.contracts()
    // then
    assert(contracts.size == 2)
    assert(contracts.map { it.response.statusCode }.containsAll(listOf(200, 404)))
    assert(contracts.first { it.response.statusCode == 200 }.request.pathParameters["id"]!!.example == null)
    assert(contracts.first { it.response.statusCode == 404 }.request.pathParameters["id"]!!.example!!.value == 999)
    assert(contracts.first { it.response.statusCode == 404 }.response.body!!.content().asMap()["error"] == "NOT FOUND")

  }

  @Test
  fun `generate contracts for 2xx with random values`() {
    // given
    val api = loadApiFrom("no_example/api_2xx_responses.yaml")
    // when
    val contracts = api.contracts()
    // then
    assert(contracts.size == 2)
    assert(contracts.map { it.response.statusCode }.containsAll(listOf(200, 201)))
  }

  @Test
  fun `generate contracts for 2xx with array as body content `() {
    // given
    val api = loadApiFrom("no_example/api_array_random_values.yaml")
    // when
    val contracts = api.contracts()
    // then
    assert(contracts.size == 2)
    assert(contracts.map { it.response.statusCode }.containsAll(listOf(200, 201)))
  }

  @Test
  fun `generate contract for each combination of request-response content-type`() {
    // given
    val api = loadApiFrom("no_example/api_multiple_content_type.yaml")
    // when
    val contracts = api.contracts()
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
    // given
    val api = loadApiFrom("examples/api_with_response_body_example_only.yaml")
    // when
    val contracts = api.contracts()
    // then
    assert(contracts.none { it.exampleKey != null })
  }

  @Test
  fun `do not generate contract with only parameter example`() {
    // given
    val api = loadApiFrom("examples/api_with_parameter_example_only.yaml")
    // when
    val contracts = api.contracts()
    // then
    assert(contracts.none { it.exampleKey != null })
  }

  @Test
  fun `do not generate contract with only request body example`() {
    // given
    val api = loadApiFrom("examples/api_with_request_body_example_only.yaml")
    // when
    val contracts = api.contracts()
    // then
    assert(contracts.none { it.exampleKey != null })
  }

  @Test
  fun `generate a contract with a single example`() {
    // given
    val api = loadApiFrom("examples/api_with_example_for_4xx_status.yaml")
    // when
    val contracts = api.contracts()
    // then
    assert(contracts.filter { it.exampleKey != null }.size == 1)
    assert(contracts.first { it.exampleKey != null }.request.pathParameters["id"]!!.value() == 999)
    assert(contracts.first { it.exampleKey != null }.response.statusCode == 404)
    assert(contracts.first { it.exampleKey != null }.exampleKey == "NOT_FOUND")
    assert(contracts.first { it.exampleKey != null }.response.body!!.content() == mapOf("error" to "NOT FOUND"))
  }

  @Test
  fun `generate contracts with array example as body content`() {
    // given
    val api = loadApiFrom("examples/api_array_examples.yaml")
    // when
    val contracts = api.contracts()
    // then
    assert(contracts.size == 2)
    assert(contracts.map { it.response.statusCode }.containsAll(listOf(200, 201)))
    assert(contracts.first { it.response.statusCode == 200 }.exampleKey == "GET_DETAILS")
    assert(contracts.first { it.response.statusCode == 201 }.exampleKey == "CREATE_PRODUCTS")
  }

  @Test
  fun `generate contracts with multiple examples`() {
    // given
    val api = loadApiFrom("examples/api_with_multiple_examples.yaml")
    // when
    val contracts = api.contracts()
    // then
    assert(contracts.size == 4)
    assertNotNull(contracts.find { it.request.method == "GET" && it.response.statusCode == 200 })
    assertNotNull(contracts.find { it.request.method == "GET" && it.response.statusCode == 404 })
    assertNotNull(contracts.find { it.request.method == "POST" && it.response.statusCode == 201 })
    assertNotNull(contracts.find { it.request.method == "POST" && it.response.statusCode == 202 })
  }

  @Test
  fun `generate contracts with multiple content-type and same example key for all`() {
    // given
    val api = loadApiFrom("examples/api_multiple_content_type_and_same_example_for_all.yaml")
    // when
    val contracts = api.contracts()
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

private fun loadApiFrom(fileName: String): OpenAPI {
  val parseOptions = ParseOptions()
  parseOptions.isResolveFully = true
  return OpenAPIV3Parser().read("src/test/resources/$fileName", null, parseOptions)
}

private fun Any?.asMap(): Map<*, *> = this as Map<*, *>
