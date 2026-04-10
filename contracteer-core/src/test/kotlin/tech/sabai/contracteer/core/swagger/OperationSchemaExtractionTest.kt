package tech.sabai.contracteer.core.swagger

import tech.sabai.contracteer.core.assertSuccess
import tech.sabai.contracteer.core.assertSingle
import tech.sabai.contracteer.core.operation.ContentType
import tech.sabai.contracteer.core.datatype.*
import tech.sabai.contracteer.core.operation.ParameterElement
import tech.sabai.contracteer.core.serde.JsonSerde
import kotlin.test.Test

class OperationSchemaExtractionTest {

  @Test
  fun `extracts request parameter schemas`() {
    // when
    val operation = loadSingleOperation("schema_without_examples.yaml")

    // then
    val pathParam = operation.requestSchema.pathParameters.single()
    assert(pathParam.element == ParameterElement.PathParam("id"))
    assert(pathParam.isRequired)
    assert(pathParam.dataType is IntegerDataType)

    val queryParam = operation.requestSchema.queryParameters.single()
    assert(queryParam.element == ParameterElement.QueryParam("query"))
    assert(!queryParam.isRequired)
    assert(queryParam.dataType is StringDataType)

    val headerParam = operation.requestSchema.headers.single()
    assert(headerParam.element == ParameterElement.Header("header"))
    assert(headerParam.isRequired)
    assert(headerParam.dataType is StringDataType)

    val cookieParam = operation.requestSchema.cookies.single()
    assert(cookieParam.element == ParameterElement.Cookie("cookie"))
    assert(!cookieParam.isRequired)
    assert(cookieParam.dataType is StringDataType)
  }

  @Test
  fun `extracts request body schema`() {
    // when
    val operation = loadSingleOperation("schema_without_examples.yaml")

    // then
    val requestBody = operation.requestSchema.bodies.single()
    assert(requestBody.contentType == ContentType("application/json"))
    assert(requestBody.dataType is ObjectDataType)
    assert((requestBody.dataType as ObjectDataType).properties["prop1"] is StringDataType)
  }

  @Test
  fun `extracts non required request body when not specified`() {
    // when
    val operation = loadSingleOperation("schema_without_examples.yaml")

    // then
    assert(!operation.requestSchema.bodies.single().isRequired)
  }

  @Test
  fun `extracts required request body`() {
    // when
    val operation = loadSingleOperation("required_request_body.yaml")

    // then
    assert(operation.requestSchema.bodies.single().isRequired)
  }

  @Test
  fun `extracts response header schemas`() {
    // when
    val operation = loadSingleOperation("schema_without_examples.yaml")

    // then
    val responseSchema = operation.responseFor(200)!!
    val headers = responseSchema.headers.associateBy { it.element }
    assert(headers.size == 2)
    assert(!headers[ParameterElement.Header("x-optional")]!!.isRequired)
    assert(headers[ParameterElement.Header("x-optional")]!!.dataType is IntegerDataType)
    assert(headers[ParameterElement.Header("x-required")]!!.isRequired)
    assert(headers[ParameterElement.Header("x-required")]!!.dataType is IntegerDataType)
  }

  @Test
  fun `ignores Content-Type Accept and Authorization in request headers`() {
    // when
    val operation = loadSingleOperation("ignored_headers.yaml")

    // then
    val headers = operation.requestSchema.headers
    assert(headers.size == 1)
    assert(headers.single().element == ParameterElement.Header("x-request-custom"))
  }

  @Test
  fun `ignores Content-Type in response headers`() {
    // when
    val operation = loadSingleOperation("ignored_headers.yaml")

    // then
    val headers = operation.responseFor(200)!!.headers
    assert(headers.size == 1)
    assert(headers.single().element == ParameterElement.Header("x-response-custom"))
  }

  @Test
  fun `extracts response body schema`() {
    // when
    val operation = loadSingleOperation("schema_without_examples.yaml")

    // then
    val responseSchema = operation.responseFor(200)!!
    val responseBody = responseSchema.bodies.single()
    assert(responseBody.contentType == ContentType("application/json"))
    assert(responseBody.dataType is ObjectDataType)
    val properties = (responseBody.dataType as ObjectDataType).properties
    assert(properties["id"] is IntegerDataType)
    assert(properties["name"] is StringDataType)
  }

  @Test
  fun `extracts 400 response schema`() {
    // when
    val operation = loadSingleOperation("with_400_response_schema.yaml")

    // then
    val successSchema = operation.responseFor(201)!!
    assert(successSchema.bodies.single().contentType == ContentType("application/json"))
    assert(successSchema.bodies.single().dataType is ObjectDataType)

    val errorSchema = operation.badRequestResponse()!!
    assert(errorSchema.bodies.single().contentType == ContentType("application/json"))
    val errorProperties = (errorSchema.bodies.single().dataType as ObjectDataType).properties
    assert(errorProperties["error"] is StringDataType)
    assert(errorProperties["details"] is StringDataType)
  }

  @Test
  fun `resolves references for parameters request bodies response headers and response bodies`() {
    // when
    val operation = loadSingleOperation("references.yaml")

    // then
    assert(operation.path == "/products/{id}")
    assert(operation.method == "GET")

    val pathParam = operation.requestSchema.pathParameters.single()
    assert(pathParam.element == ParameterElement.PathParam("id"))
    assert(pathParam.isRequired)
    assert(pathParam.dataType is IntegerDataType)

    val requestBody = operation.requestSchema.bodies.single()
    assert(requestBody.contentType == ContentType("application/json"))
    assert(requestBody.dataType is ObjectDataType)
    assert((requestBody.dataType as ObjectDataType).properties["prop1"] is StringDataType)

    val responseHeaders = operation.responseFor(200)!!.headers.associateBy { it.element }
    assert(!responseHeaders[ParameterElement.Header("x-optional")]!!.isRequired)
    assert(responseHeaders[ParameterElement.Header("x-optional")]!!.dataType is IntegerDataType)

    val responseBody = operation.responseFor(200)!!.bodies.single()
    assert(responseBody.contentType == ContentType("application/json"))
    assert(responseBody.dataType is ObjectDataType)
    val responseProperties = (responseBody.dataType as ObjectDataType).properties
    assert(responseProperties.keys == setOf("id", "name", "quantity"))
  }

  @Test
  fun `extracts multiple request and response content types`() {
    // when
    val operation = loadSingleOperation("multiple_content_types.yaml")

    // then
    val requestContentTypes = operation.requestSchema.bodies.map { it.contentType.value }.toSet()
    assert(requestContentTypes == setOf("application/json", "application/vnd.mycompany.myapp.v2+json"))

    val responseContentTypes = operation.responseFor(201)!!.bodies.map { it.contentType.value }.toSet()
    assert(responseContentTypes == setOf("application/json", "application/vnd.mycompany.myapp.v2+json"))
  }

  @Test
  fun `accepts JSON content type with object and array body schema`() {
    // when
    val operations = loadOperations("json_body_accepts_object_and_array.yaml")

    // then
    assert(operations.size == 2)
    val objectOperation = operations.first { it.path == "/products/{id}" }
    assert(objectOperation.responseFor(200)!!.bodies.single().dataType is ObjectDataType)

    val arrayOperation = operations.first { it.path == "/products" }
    assert(arrayOperation.responseFor(200)!!.bodies.single().dataType is ArrayDataType)
  }

  @Test
  fun `accepts JSON content type with primitive body schema`() {
    // when
    val operations = loadOperations("json_body_accepts_primitive.yaml")

    // then
    val stringBody = operations.first { it.path == "/products/name" }
      .responseFor(200)!!.bodies.single()
    assert(stringBody.dataType is StringDataType)
    assert(stringBody.serde === JsonSerde)

    val integerBody = operations.first { it.path == "/products/count" }
      .responseFor(200)!!.bodies.single()
    assert(integerBody.dataType is IntegerDataType)
    assert(integerBody.serde === JsonSerde)

    val booleanBody = operations.first { it.path == "/products/in-stock" }
      .responseFor(200)!!.bodies.single()
    assert(booleanBody.dataType is BooleanDataType)
    assert(booleanBody.serde === JsonSerde)
  }

  @Test
  fun `extracts readOnly and writeOnly properties into request and response variants`() {
    // when
    val operation = loadSingleOperation("readonly_writeonly.yaml")
    val requestBody = operation.requestSchema.bodies.single().dataType as ObjectDataType
    val responseBody = operation.responseFor(201)!!.bodies.single().dataType as ObjectDataType

    // then — request body excludes readOnly, includes writeOnly
    assert(!requestBody.properties.containsKey("id"))
    assert(requestBody.properties.containsKey("name"))
    assert(requestBody.properties.containsKey("password"))
    assert(!requestBody.requiredProperties.contains("id"))
    assert(requestBody.requiredProperties.contains("name"))
    assert(requestBody.requiredProperties.contains("password"))

    // then — response body includes readOnly, excludes writeOnly
    assert(responseBody.properties.containsKey("id"))
    assert(responseBody.properties.containsKey("name"))
    assert(!responseBody.properties.containsKey("password"))
    assert(responseBody.requiredProperties.contains("id"))
    assert(responseBody.requiredProperties.contains("name"))
    assert(!responseBody.requiredProperties.contains("password"))
  }

  @Test
  fun `extracts default response`() {
    // when
    val operation = loadSingleOperation("default_response.yaml")

    // then
    assert(operation.responseFor(200) != null)
    assert(operation.defaultResponse != null)
    val defaultBody = operation.defaultResponse!!.bodies.single()
    assert(defaultBody.dataType is ObjectDataType)
    val properties = (defaultBody.dataType as ObjectDataType).properties
    assert(properties.containsKey("error"))
    assert(properties.containsKey("details"))
  }

  @Test
  fun `extracts status code class response`() {
    // when
    val operation = loadSingleOperation("status_code_class_response.yaml")

    // then
    assert(operation.responseFor(200) != null)
    assert(operation.badRequestResponse() != null)
    assert(operation.responseFor(404) != null)
    assert(operation.responseFor(404) === operation.responseFor(403))
    val classBody = operation.responseFor(404)!!.bodies.single()
    assert(classBody.dataType is ObjectDataType)
    assert((classBody.dataType as ObjectDataType).properties.containsKey("error"))
  }

  @Test
  fun `extracts string pattern`() {
    // when
    val operation = loadSingleOperation("string_pattern.yaml")

    // then
    val requestBody = operation.requestSchema.bodies.single().dataType as ObjectDataType
    val codeType = requestBody.properties["code"] as StringDataType
    assert(codeType.pattern == "^[A-Z]{2}-\\d{4}$")
  }

  // --- Helpers ---

  private fun loadSingleOperation(yamlFile: String) =
    loadOperations(yamlFile).assertSingle()

  private fun loadOperations(yamlFile: String) =
    OpenApiLoader.loadOperations("src/test/resources/operation/$yamlFile").assertSuccess()
}
