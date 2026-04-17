package tech.sabai.contracteer.core.swagger

import tech.sabai.contracteer.core.assertSuccess
import tech.sabai.contracteer.core.assertSingle
import tech.sabai.contracteer.core.normalize
import tech.sabai.contracteer.core.operation.ParameterElement
import kotlin.test.Test

class ScenarioExtractionTest {

  @Test
  fun `extracts scenarios with matching request and response example keys`() {
    // when
    val operation = loadSingleOperation("multiple_examples.yaml")

    // then
    assert(operation.scenarios.size == 2)

    val detailsScenario = operation.scenarios.first { it.key == "GET_DETAILS" }
    assert(detailsScenario.path == "/products/{id}")
    assert(detailsScenario.method == "GET")
    assert(detailsScenario.statusCode == 200)
    assert(detailsScenario.request.parameterValues[ParameterElement.PathParam("id")] == 10.normalize())
    assert(detailsScenario.request.body == null)
    assert(detailsScenario.response.body!!.contentType.value == "application/json")
    assert(detailsScenario.response.body.value == mapOf(
      "id" to 10,
      "name" to "La Bouledogue",
      "quantity" to 5
    ).normalize())

    val notFoundScenario = operation.scenarios.first { it.key == "NOT_FOUND" }
    assert(notFoundScenario.statusCode == 404)
    assert(notFoundScenario.request.parameterValues[ParameterElement.PathParam("id")] == 999.normalize())
    assert(notFoundScenario.response.body!!.value == mapOf("error" to "NOT FOUND").normalize())
  }

  @Test
  fun `does not create scenarios when only request parameter examples exist`() {
    // when
    val operation = loadSingleOperation("parameter_example_only.yaml")

    // then
    assert(operation.scenarios.isEmpty())
  }

  @Test
  fun `does not create scenarios when only request body examples exist`() {
    // when
    val operation = loadSingleOperation("request_body_example_only.yaml")

    // then
    assert(operation.scenarios.isEmpty())
  }

  @Test
  fun `does not create scenarios when only response body examples exist`() {
    // when
    val operation = loadSingleOperation("response_body_example_only.yaml")

    // then
    assert(operation.scenarios.isEmpty())
  }

  @Test
  fun `does not create scenario when example key exists only on response side`() {
    // when
    val operation = loadSingleOperation("400_bad_request_with_invalid_example.yaml")

    // then
    val scenarioKeys = operation.scenarios.map { it.key }.toSet()
    assert(!scenarioKeys.contains("INVALID_BODY_WRONG_TYPE"))
  }

  @Test
  fun `keeps 2xx response schema when only 4xx has scenario`() {
    // when
    val operation = loadSingleOperation("2xx_schema_with_4xx_scenario.yaml")

    // then
    assert(operation.responseSchemas.responseFor(200) != null)
    assert(operation.responseSchemas.responseFor(404) != null)
    assert(operation.scenarios.size == 1)
    assert(operation.scenarios.single().statusCode == 404)
  }

  @Test
  fun `creates scenarios for each request and response content type combination`() {
    // when
    val operation = loadSingleOperation("scenario_cartesian_content_types.yaml")

    // then
    assert(operation.scenarios.size == 4)
    val contentTypePairs = operation.scenarios.map {
      it.request.body!!.contentType.value to it.response.body!!.contentType.value
    }.toSet()
    assert(contentTypePairs == setOf(
      "application/json" to "application/json",
      "application/json" to "application/vnd.mycompany.myapp.v2+json",
      "application/vnd.mycompany.myapp.v2+json" to "application/json",
      "application/vnd.mycompany.myapp.v2+json" to "application/vnd.mycompany.myapp.v2+json"
    ))
  }

  @Test
  fun `does not validate 400 scenario examples against request schema`() {
    // when
    val operation = loadSingleOperation("400_bad_request_with_invalid_example.yaml")

    // then
    val badRequestScenarios = operation.scenarios.filter { it.statusCode == 400 }
    assert(badRequestScenarios.size == 5)

    val invalidPath = badRequestScenarios.first { it.key == "INVALID_PATH" }
    assert(invalidPath.request.parameterValues[ParameterElement.PathParam("id")] == "not-a-number")

    val invalidQuery = badRequestScenarios.first { it.key == "INVALID_QUERY" }
    assert(invalidQuery.request.parameterValues[ParameterElement.QueryParam("filter")] == 123.normalize())

    val invalidHeader = badRequestScenarios.first { it.key == "INVALID_HEADER" }
    assert(invalidHeader.request.parameterValues[ParameterElement.Header("X-Custom-Header")] == 999.normalize())

    val invalidCookie = badRequestScenarios.first { it.key == "INVALID_COOKIE" }
    assert(invalidCookie.request.parameterValues[ParameterElement.Cookie("session")] == 456.normalize())

    val invalidBody = badRequestScenarios.first { it.key == "INVALID_BODY_MISSING_FIELD" }
    val bodyValue = invalidBody.request.body!!.value as Map<*, *>
    assert(!bodyValue.containsKey("age"))
  }

  @Test
  fun `extracts scenario from single example keyword with _example key`() {
    // when
    val operation = loadSingleOperation("single_example.yaml")

    // then
    assert(operation.scenarios.size == 1)

    val scenario = operation.scenarios.single()
    assert(scenario.key == "_example")
    assert(scenario.path == "/products/{id}")
    assert(scenario.method == "GET")
    assert(scenario.statusCode == 200)
    assert(scenario.request.parameterValues[ParameterElement.PathParam("id")] == 10.normalize())
    assert(scenario.request.body == null)
    assert(scenario.response.body!!.contentType.value == "application/json")
    assert(scenario.response.body.value == mapOf(
      "id" to 10,
      "name" to "La Bouledogue",
      "quantity" to 5
    ).normalize())
  }

  @Test
  fun `rejects spec when both example and examples are defined on the same element`() {
    // when
    val result = OpenApiLoader.loadOperations("src/test/resources/scenario/single_example_ignored_when_examples_defined.yaml")

    // then
    assert(result.isFailure())
  }

  @Test
  fun `creates scenario from status-code-prefixed key without response examples`() {
    // when
    val operation = loadSingleOperation("status_code_prefixed_example_key.yaml")

    // then
    assert(operation.scenarios.size == 2)

    val detailsScenario = operation.scenarios.first { it.key == "GET_DETAILS" }
    assert(detailsScenario.statusCode == 200)
    assert(detailsScenario.request.parameterValues[ParameterElement.PathParam("id")] == 10.normalize())
    assert(detailsScenario.response.body!!.value == mapOf(
      "id" to 10,
      "name" to "La Bouledogue",
      "quantity" to 5
    ).normalize())

    val notFoundScenario = operation.scenarios.first { it.key == "404_not_found" }
    assert(notFoundScenario.statusCode == 404)
    assert(notFoundScenario.request.parameterValues[ParameterElement.PathParam("id")] == 999.normalize())
    assert(notFoundScenario.response.body == null)
  }

  @Test
  fun `does not create scenario for status-code-prefixed key on non-matching status code`() {
    // when
    val operation = loadSingleOperation("status_code_prefixed_key_exclusivity.yaml")

    // then
    assert(operation.scenarios.size == 1)
    val scenario = operation.scenarios.single()
    assert(scenario.key == "200_get_details")
    assert(scenario.statusCode == 200)
    assert(scenario.request.parameterValues[ParameterElement.PathParam("id")] == 10.normalize())
  }

  @Test
  fun `creates scenario from status-code-prefixed key with class or default response`() {
    // when
    val operations = OpenApiLoader.loadOperations(
      "src/test/resources/scenario/status_code_prefixed_key_with_class_and_default_response.yaml"
    ).assertSuccess()

    // then
    assert(operations.size == 3)

    val classResponseOp = operations.first { it.path == "/products-with-class-response/{id}" }
    assert(classResponseOp.scenarios.size == 1)
    assert(classResponseOp.scenarios.single().key == "404_not_found")
    assert(classResponseOp.scenarios.single().statusCode == 404)
    assert(classResponseOp.scenarios.single().request.parameterValues[ParameterElement.PathParam("id")] == 999.normalize())
    assert(classResponseOp.scenarios.single().response.body == null)

    val defaultResponseOp = operations.first { it.path == "/products-with-default-response/{id}" }
    assert(defaultResponseOp.scenarios.size == 1)
    assert(defaultResponseOp.scenarios.single().key == "404_not_found")
    assert(defaultResponseOp.scenarios.single().statusCode == 404)
    assert(defaultResponseOp.scenarios.single().request.parameterValues[ParameterElement.PathParam("id")] == 999.normalize())
    assert(defaultResponseOp.scenarios.single().response.body == null)

    val classAndDefaultOp = operations.first { it.path == "/products-with-class-and-default-response/{id}" }
    assert(classAndDefaultOp.scenarios.size == 1)
    assert(classAndDefaultOp.scenarios.single().key == "404_not_found")
    assert(classAndDefaultOp.scenarios.single().statusCode == 404)
    assert(classAndDefaultOp.scenarios.single().request.parameterValues[ParameterElement.PathParam("id")] == 999.normalize())
    assert(classAndDefaultOp.scenarios.single().response.body == null)
  }

  @Test
  fun `extracts scenario when requestBody uses a $ref`() {
    // when
    val operation = loadSingleOperation("ref_request_body.yaml")

    // then
    assert(operation.scenarios.size == 1)

    val scenario = operation.scenarios.single()
    assert(scenario.key == "NEW_PRODUCT")
    assert(scenario.statusCode == 201)
    assert(scenario.request.body!!.contentType.value == "application/json")
    assert(scenario.request.body.value == mapOf(
      "id" to 10,
      "name" to "Duvel",
      "quantity" to 24
    ).normalize())
    assert(scenario.response.body!!.contentType.value == "application/json")
    assert(scenario.response.body.value == mapOf(
      "id" to 10,
      "name" to "Duvel",
      "quantity" to 24
    ).normalize())
  }

  // --- Helpers ---
  private fun loadSingleOperation(yamlFile: String) =
    OpenApiLoader.loadOperations("src/test/resources/scenario/$yamlFile")
      .assertSuccess()
      .assertSingle()
}
