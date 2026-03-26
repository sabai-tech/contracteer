package tech.sabai.contracteer.verifier

import tech.sabai.contracteer.core.operation.*
import tech.sabai.contracteer.core.operation.ParameterElement.PathParam
import tech.sabai.contracteer.core.codec.SimpleStyleCodec
import tech.sabai.contracteer.core.serde.JsonSerde
import tech.sabai.contracteer.verifier.TestFixture.integerDataType
import tech.sabai.contracteer.verifier.TestFixture.objectDataType
import tech.sabai.contracteer.verifier.TestFixture.stringDataType
import kotlin.test.Test

class VerificationCaseTest {

  @Test
  fun `display name for scenario based with request and response bodies`() {
    // Given
    val case = VerificationCase.ScenarioBased(
      Scenario(
        path = "/users/{id}",
        method = "GET",
        key = "validUser",
        statusCode = 200,
        request = ScenarioRequest(
          parameterValues = mapOf(PathParam("id") to 123),
          body = ScenarioBody(
            contentType = ContentType("application/json"),
            value = mapOf("name" to "John")
          )
        ),
        response = ScenarioResponse(
          headers = emptyMap(),
          body = ScenarioBody(
            contentType = ContentType("application/json"),
            value = mapOf("id" to 123, "name" to "John")
          )
        )
      ), RequestSchema(
        parameters = listOf(
          ParameterSchema(
            element = PathParam("id"),
            dataType = integerDataType(),
            isRequired = true,
            codec = SimpleStyleCodec("id", false)
          )
        ),
        bodies = listOf(
          BodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("name" to stringDataType())),
            isRequired = true,
            serde = JsonSerde
          )
        )
      ), ResponseSchema(
        headers = emptyList(),
        bodies = listOf(
          BodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("id" to integerDataType(), "name" to stringDataType())),
            isRequired = true,
            serde = JsonSerde
          )
        )
      ))

    // When
    val displayName = case.displayName

    // Then
    assert(displayName == "GET /users/{id} (application/json) -> 200 (application/json) with scenario 'validUser'")
  }

  @Test
  fun `display name for scenario based without request body`() {
    // Given
    val case = VerificationCase.ScenarioBased(
      Scenario(
        path = "/users/{id}",
        method = "GET",
        key = "notFound",
        statusCode = 404,
        request = ScenarioRequest(
          parameterValues = mapOf(PathParam("id") to 999),
          body = null
        ),
        response = ScenarioResponse(
          headers = emptyMap(),
          body = null
        )
      ),
      RequestSchema(
        parameters = listOf(
          ParameterSchema(
            element = PathParam("id"),
            dataType = integerDataType(),
            isRequired = true,
            codec = SimpleStyleCodec("id", false)
          )
        ),
        bodies = emptyList()
      ),
      ResponseSchema(
        headers = emptyList(),
        bodies = emptyList()
      ))

    // When
    val displayName = case.displayName

    // Then
    assert(displayName == "GET /users/{id} -> 404 with scenario 'notFound'")
  }

  @Test
  fun `display name for scenario based with POST method`() {
    // Given

    val case = VerificationCase.ScenarioBased(
      Scenario(
        path = "/orders",
        method = "POST",
        key = "success",
        statusCode = 201,
        request = ScenarioRequest(
          parameterValues = emptyMap(),
          body = ScenarioBody(
            contentType = ContentType("application/json"),
            value = mapOf("product" to "laptop", "quantity" to 1)
          )
        ),
        response = ScenarioResponse(
          headers = emptyMap(),
          body = ScenarioBody(
            contentType = ContentType("application/json"),
            value = mapOf("orderId" to "12345")
          )
        )
      ),
      RequestSchema(
        parameters = emptyList(),
        bodies = listOf(
          BodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("product" to stringDataType(),
                                                         "quantity" to integerDataType())),
            isRequired = true,
            serde = JsonSerde
          )
        )
      ),
      ResponseSchema(
        headers = emptyList(),
        bodies = listOf(
          BodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("orderId" to stringDataType())),
            isRequired = true,
            serde = JsonSerde
          )
        )
      ))

    // When
    val displayName = case.displayName

    // Then
    assert(displayName == "POST /orders (application/json) -> 201 (application/json) with scenario 'success'")
  }

  @Test
  fun `display name for schema only with request and response bodies`() {
    // Given

    val case = VerificationCase.SchemaBased(
      path = "/products",
      method = "GET",
      statusCode = 200,
      requestContentType = ContentType("application/json"),
      responseContentType = ContentType("application/json"),
      requestSchema = RequestSchema(
        parameters = listOf(
          ParameterSchema(
            element = PathParam("id"),
            dataType = integerDataType(),
            isRequired = true,
            codec = SimpleStyleCodec("id", false)
          )
        ),
        bodies = listOf(
          BodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("name" to stringDataType())),
            isRequired = true,
            serde = JsonSerde
          )
        )
      ),
      responseSchema = ResponseSchema(
        headers = emptyList(),
        bodies = listOf(
          BodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("id" to integerDataType(), "name" to stringDataType())),
            isRequired = true,
            serde = JsonSerde
          )
        )
      )
    )

    // When
    val displayName = case.displayName

    // Then
    assert(displayName == "GET /products (application/json) -> 200 (application/json) (generated)")
  }

  @Test
  fun `display name for schema only without request body`() {
    // Given

    val case = VerificationCase.SchemaBased(
      path = "/products",
      method = "GET",
      statusCode = 200,
      requestContentType = null,
      responseContentType = ContentType("application/json"),
      requestSchema = RequestSchema(
        parameters = emptyList(),
        bodies = emptyList()
      ),
      responseSchema = ResponseSchema(
        headers = emptyList(),
        bodies = listOf(
          BodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("items" to stringDataType())),
            isRequired = true,
            serde = JsonSerde
          )
        )
      )
    )

    // When
    val displayName = case.displayName

    // Then
    assert(displayName == "GET /products -> 200 (application/json) (generated)")
  }

  @Test
  fun `display name for schema only with XML content types`() {
    // Given

    val case = VerificationCase.SchemaBased(
      path = "/legacy/endpoint",
      method = "POST",
      statusCode = 201,
      requestContentType = ContentType("application/xml"),
      responseContentType = ContentType("application/xml"),
      requestSchema = RequestSchema(
        parameters = emptyList(),
        bodies = listOf(
          BodySchema(
            contentType = ContentType("application/xml"),
            dataType = objectDataType(properties = mapOf("name" to stringDataType())),
            isRequired = true,
            serde = JsonSerde
          )
        )
      ),
      responseSchema = ResponseSchema(
        headers = emptyList(),
        bodies = listOf(
          BodySchema(
            contentType = ContentType("application/xml"),
            dataType = objectDataType(properties = mapOf("result" to stringDataType())),
            isRequired = true,
            serde = JsonSerde
          )
        )
      )
    )

    // When
    val displayName = case.displayName

    // Then
    assert(displayName == "POST /legacy/endpoint (application/xml) -> 201 (application/xml) (generated)")
  }

  @Test
  fun `display name for schema only without any bodies`() {
    // Given

    val case = VerificationCase.SchemaBased(
      path = "/health",
      method = "GET",
      statusCode = 204,
      requestContentType = null,
      responseContentType = null,
      requestSchema = RequestSchema(
        parameters = emptyList(),
        bodies = emptyList()
      ),
      responseSchema = ResponseSchema(
        headers = emptyList(),
        bodies = emptyList()
      )
    )

    // When
    val displayName = case.displayName

    // Then
    assert(displayName == "GET /health -> 204 (generated)")
  }
}
