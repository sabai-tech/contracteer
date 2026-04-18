package tech.sabai.contracteer.verifier

import tech.sabai.contracteer.core.operation.*
import tech.sabai.contracteer.core.codec.SimpleParameterCodec
import tech.sabai.contracteer.core.serde.JsonSerde
import tech.sabai.contracteer.core.TestFixture.integerDataType
import tech.sabai.contracteer.core.TestFixture.objectDataType
import tech.sabai.contracteer.core.TestFixture.stringDataType
import tech.sabai.contracteer.verifier.VerificationCase.ScenarioBased
import tech.sabai.contracteer.verifier.VerificationCase.SchemaBased
import kotlin.test.Test

class VerificationCaseFactoryTest {

  @Test
  fun `creates scenario based cases from operation scenarios`() {
    // Given
    val apiOperation = ApiOperation(
      path = "/users/{id}",
      method = "GET",
      requestSchema = RequestSchema(
        parameters = listOf(
          ParameterSchema(
            element = ParameterElement.PathParam("id"),
            dataType = integerDataType(),
            isRequired = true,
            codec = SimpleParameterCodec("id", false)
          )
        ),
        bodies = emptyList()
      ),
      responseSchemas = ResponseSchemas(byStatusCode = mapOf(
        200 to ResponseSchema(
          headers = emptyList(),
          bodies = listOf(
            BodySchema(
              contentType = ContentType("application/json"),
              dataType = objectDataType(properties = mapOf("id" to integerDataType(), "name" to stringDataType())),
              isRequired = true,
              serde = JsonSerde
            )
          )
        ),
        404 to ResponseSchema(
          headers = emptyList(),
          bodies = emptyList()
        )
      )),
      scenarios = listOf(
        Scenario(
          path = "/users/{id}",
          method = "GET",
          key = "validUser",
          statusCode = 200,
          request = ScenarioRequest(
            parameterValues = mapOf(ParameterElement.PathParam("id") to 1),
            body = null
          ),
          response = ScenarioResponse(
            headers = emptyMap(),
            body = ScenarioBody(
              contentType = ContentType("application/json"),
              value = mapOf("id" to 1, "name" to "John")
            )
          )
        ),
        Scenario(
          path = "/users/{id}",
          method = "GET",
          key = "notFound",
          statusCode = 404,
          request = ScenarioRequest(
            parameterValues = mapOf(ParameterElement.PathParam("id") to 999),
            body = null
          ),
          response = ScenarioResponse(
            headers = emptyMap(),
            body = null
          )
        )
      )
    )

    // When
    val cases = VerificationCaseFactory.create(apiOperation)

    // Then
    assert(cases.size == 2)
    assert(cases.all { it is ScenarioBased })
    assert(cases[0].displayName.contains("validUser"))
    assert(cases[1].displayName.contains("notFound"))
  }

  @Test
  fun `generates schema based case when no 2xx scenario exists`() {
    // Given
    val apiOperation = ApiOperation(
      path = "/products",
      method = "GET",
      requestSchema = RequestSchema(
        parameters = emptyList(),
        bodies = emptyList()
      ),
      responseSchemas = ResponseSchemas(byStatusCode = mapOf(
        200 to ResponseSchema(
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
      )),
      scenarios = emptyList()
    )

    // When
    val cases = VerificationCaseFactory.create(apiOperation)

    // Then
    assert(cases.size == 1)
    assert(cases[0] is SchemaBased)
    assert(cases[0].displayName.contains("(generated)"))
  }

  @Test
  fun `creates cartesian product for schema based cases with multiple content types`() {
    // Given
    val apiOperation = ApiOperation(
      path = "/data",
      method = "POST",
      requestSchema = RequestSchema(
        parameters = emptyList(),
        bodies = listOf(
          BodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("value" to stringDataType())),
            isRequired = true,
            serde = JsonSerde
          ),
          BodySchema(
            contentType = ContentType("application/xml"),
            dataType = stringDataType(),
            isRequired = true,
            serde = JsonSerde
          )
        )
      ),
      responseSchemas = ResponseSchemas(byStatusCode = mapOf(
        201 to ResponseSchema(
          headers = emptyList(),
          bodies = listOf(
            BodySchema(
              contentType = ContentType("application/json"),
              dataType = objectDataType(properties = mapOf("result" to stringDataType())),
              isRequired = true,
              serde = JsonSerde
            ),
            BodySchema(
              contentType = ContentType("application/xml"),
              dataType = stringDataType(),
              isRequired = true,
              serde = JsonSerde
            )
          )
        )
      )),
      scenarios = emptyList()
    )

    // When
    val cases = VerificationCaseFactory.create(apiOperation)

    // Then
    assert(cases.size == 4)
    assert(cases.all { it is SchemaBased })
    assert(cases.all { it.displayName.contains("(generated)") })
  }

  @Test
  fun `does not generate schema based case when 2xx scenario exists`() {
    // Given
    val apiOperation = ApiOperation(
      path = "/orders",
      method = "POST",
      requestSchema = RequestSchema(
        parameters = emptyList(),
        bodies = listOf(
          BodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("product" to stringDataType())),
            isRequired = true,
            serde = JsonSerde
          )
        )
      ),
      responseSchemas = ResponseSchemas(byStatusCode = mapOf(
        201 to ResponseSchema(
          headers = emptyList(),
          bodies = listOf(
            BodySchema(
              contentType = ContentType("application/json"),
              dataType = objectDataType(properties = mapOf("orderId" to stringDataType())),
              isRequired = true,
              serde = JsonSerde
            )
          )
        )
      )),
      scenarios = listOf(
        Scenario(
          path = "/orders",
          method = "POST",
          key = "success",
          statusCode = 201,
          request = ScenarioRequest(
            parameterValues = emptyMap(),
            body = ScenarioBody(
              contentType = ContentType("application/json"),
              value = mapOf("product" to "laptop")
            )
          ),
          response = ScenarioResponse(
            headers = emptyMap(),
            body = ScenarioBody(
              contentType = ContentType("application/json"),
              value = mapOf("orderId" to "12345")
            )
          )
        )
      )
    )

    // When
    val cases = VerificationCaseFactory.create(apiOperation)

    // Then
    assert(cases.size == 1)
    assert(cases[0] is ScenarioBased)
    assert(cases[0].displayName.contains("success"))
    assert(!cases[0].displayName.contains("(generated)"))
  }

  @Test
  fun `fans out scenario based cases per required content type when scenario has no body example`() {
    // Given a scenario with no body example and a required request body with two content types
    val apiOperation = ApiOperation(
      path = "/upload",
      method = "POST",
      requestSchema = RequestSchema(
        parameters = emptyList(),
        bodies = listOf(
          BodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("name" to stringDataType())),
            isRequired = true,
            serde = JsonSerde
          ),
          BodySchema(
            contentType = ContentType("application/xml"),
            dataType = objectDataType(properties = mapOf("name" to stringDataType())),
            isRequired = true,
            serde = JsonSerde
          )
        )
      ),
      responseSchemas = ResponseSchemas(byStatusCode = mapOf(
        200 to ResponseSchema(headers = emptyList(), bodies = emptyList())
      )),
      scenarios = listOf(
        Scenario(
          path = "/upload",
          method = "POST",
          key = "K1",
          statusCode = 200,
          request = ScenarioRequest(parameterValues = emptyMap(), body = null),
          response = ScenarioResponse(headers = emptyMap(), body = null)
        )
      )
    )

    // When
    val cases = VerificationCaseFactory.create(apiOperation)

    // Then
    assert(cases.size == 2)
    assert(cases.all { it is ScenarioBased })
    val names = cases.map { it.displayName }
    assert(names.any { it.contains("application/json") && it.contains("K1") })
    assert(names.any { it.contains("application/xml") && it.contains("K1") })
  }

  @Test
  fun `marks request content type when scenario has no body example and single required content type`() {
    // Given a scenario with no body example and a required request body with a single content type
    val apiOperation = ApiOperation(
      path = "/upload",
      method = "POST",
      requestSchema = RequestSchema(
        parameters = emptyList(),
        bodies = listOf(
          BodySchema(
            contentType = ContentType("multipart/form-data"),
            dataType = objectDataType(properties = mapOf("file" to stringDataType())),
            isRequired = true,
            serde = JsonSerde
          )
        )
      ),
      responseSchemas = ResponseSchemas(byStatusCode = mapOf(
        200 to ResponseSchema(headers = emptyList(), bodies = emptyList())
      )),
      scenarios = listOf(
        Scenario(
          path = "/upload",
          method = "POST",
          key = "K1",
          statusCode = 200,
          request = ScenarioRequest(parameterValues = emptyMap(), body = null),
          response = ScenarioResponse(headers = emptyMap(), body = null)
        )
      )
    )

    // When
    val cases = VerificationCaseFactory.create(apiOperation)

    // Then
    assert(cases.size == 1)
    assert(cases[0] is ScenarioBased)
    assert(cases[0].displayName.contains("multipart/form-data"))
    assert(cases[0].displayName.contains("K1"))
  }

  @Test
  fun `does not fan out scenario based cases when request body is optional`() {
    // Given a scenario with no body example and an optional request body
    val apiOperation = ApiOperation(
      path = "/search",
      method = "POST",
      requestSchema = RequestSchema(
        parameters = emptyList(),
        bodies = listOf(
          BodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("q" to stringDataType())),
            isRequired = false,
            serde = JsonSerde
          ),
          BodySchema(
            contentType = ContentType("application/xml"),
            dataType = objectDataType(properties = mapOf("q" to stringDataType())),
            isRequired = false,
            serde = JsonSerde
          )
        )
      ),
      responseSchemas = ResponseSchemas(byStatusCode = mapOf(
        200 to ResponseSchema(headers = emptyList(), bodies = emptyList())
      )),
      scenarios = listOf(
        Scenario(
          path = "/search",
          method = "POST",
          key = "K1",
          statusCode = 200,
          request = ScenarioRequest(parameterValues = emptyMap(), body = null),
          response = ScenarioResponse(headers = emptyMap(), body = null)
        )
      )
    )

    // When
    val cases = VerificationCaseFactory.create(apiOperation)

    // Then
    assert(cases.size == 1)
    assert(cases[0] is ScenarioBased)
    assert(!cases[0].displayName.contains("application/json"))
    assert(!cases[0].displayName.contains("application/xml"))
  }

  @Test
  fun `does not generate schema based case when multiple 2xx responses exist without scenarios`() {
    // Given
    val apiOperation = ApiOperation(
      path = "/resources",
      method = "GET",
      requestSchema = RequestSchema(
        parameters = emptyList(),
        bodies = emptyList()
      ),
      responseSchemas = ResponseSchemas(byStatusCode = mapOf(
        200 to ResponseSchema(
          headers = emptyList(),
          bodies = listOf(
            BodySchema(
              contentType = ContentType("application/json"),
              dataType = objectDataType(properties = mapOf("data" to stringDataType())),
              isRequired = true,
              serde = JsonSerde
            )
          )
        ),
        202 to ResponseSchema(
          headers = emptyList(),
          bodies = listOf(
            BodySchema(
              contentType = ContentType("application/json"),
              dataType = objectDataType(properties = mapOf("status" to stringDataType())),
              isRequired = true,
              serde = JsonSerde
            )
          )
        )
      )),
      scenarios = emptyList()
    )

    // When
    val cases = VerificationCaseFactory.create(apiOperation)

    // Then
    assert(cases.isEmpty())
  }
}
