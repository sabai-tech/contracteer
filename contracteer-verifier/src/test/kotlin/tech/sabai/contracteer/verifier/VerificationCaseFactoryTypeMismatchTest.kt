package tech.sabai.contracteer.verifier

import tech.sabai.contracteer.core.operation.*
import tech.sabai.contracteer.core.operation.ParameterElement.*
import tech.sabai.contracteer.core.codec.DeepObjectParameterCodec
import tech.sabai.contracteer.core.codec.FormParameterCodec
import tech.sabai.contracteer.core.codec.SimpleParameterCodec
import tech.sabai.contracteer.core.serde.JsonSerde
import tech.sabai.contracteer.core.TestFixture.integerDataType
import tech.sabai.contracteer.core.TestFixture.objectDataType
import tech.sabai.contracteer.core.TestFixture.stringDataType
import tech.sabai.contracteer.verifier.VerificationCase.ScenarioBased
import tech.sabai.contracteer.verifier.VerificationCase.TypeMismatch
import kotlin.test.Test

class VerificationCaseFactoryTypeMismatchTest {

  @Test
  fun `generates type mismatch case for path parameter`() {
    // Given
    val apiOperation = apiOperationWith400(
      parameters = listOf(
        ParameterSchema(element = PathParam("id"),
                        dataType = integerDataType(),
                        isRequired = true,
                        codec = SimpleParameterCodec("id", false))
      )
    )

    // When
    val cases = VerificationCaseFactory.create(apiOperation)
    val typeMismatchCases = cases.filterIsInstance<TypeMismatch>()

    // Then
    val pathCase = typeMismatchCases.find { it.mutatedElement == MutatedElement.Parameter(PathParam("id")) }
    assert(pathCase != null)
    assert(pathCase!!.mutatedValue == "<<not a integer>>")
  }

  @Test
  fun `generates type mismatch case for query parameter`() {
    // Given
    val apiOperation = apiOperationWith400(
      parameters = listOf(
        ParameterSchema(element = QueryParam("page"),
                        dataType = integerDataType(),
                        isRequired = false,
                        codec = FormParameterCodec("page", true))
      )
    )

    // When
    val cases = VerificationCaseFactory.create(apiOperation)
    val typeMismatchCases = cases.filterIsInstance<TypeMismatch>()

    // Then
    val queryCase = typeMismatchCases.find { it.mutatedElement == MutatedElement.Parameter(QueryParam("page")) }
    assert(queryCase != null)
    assert(queryCase!!.mutatedValue == "<<not a integer>>")
  }

  @Test
  fun `generates type mismatch case for header parameter`() {
    // Given
    val apiOperation = apiOperationWith400(
      parameters = listOf(
        ParameterSchema(element = Header("X-Request-Id"),
                        dataType = integerDataType(),
                        isRequired = true,
                        codec = SimpleParameterCodec("X-Request-Id", false))
      )
    )

    // When
    val cases = VerificationCaseFactory.create(apiOperation)
    val typeMismatchCases = cases.filterIsInstance<TypeMismatch>()

    // Then
    val headerCase = typeMismatchCases.find { it.mutatedElement == MutatedElement.Parameter(Header("X-Request-Id")) }
    assert(headerCase != null)
    assert(headerCase!!.mutatedValue == "<<not a integer>>")
  }

  @Test
  fun `generates type mismatch case for cookie parameter`() {
    // Given
    val apiOperation = apiOperationWith400(
      parameters = listOf(
        ParameterSchema(element = Cookie("session_ttl"),
                        dataType = integerDataType(),
                        isRequired = false,
                        codec = FormParameterCodec("session_ttl", true))
      )
    )

    // When
    val cases = VerificationCaseFactory.create(apiOperation)
    val typeMismatchCases = cases.filterIsInstance<TypeMismatch>()

    // Then
    val cookieCase = typeMismatchCases.find { it.mutatedElement == MutatedElement.Parameter(Cookie("session_ttl")) }
    assert(cookieCase != null)
    assert(cookieCase!!.mutatedValue == "<<not a integer>>")
  }

  @Test
  fun `generates one type mismatch case per element category`() {
    // Given
    val apiOperation = apiOperationWith400(
      parameters = listOf(
        ParameterSchema(element = PathParam("id"),
                        dataType = integerDataType(),
                        isRequired = true,
                        codec = SimpleParameterCodec("id", false)),
        ParameterSchema(element = QueryParam("page"),
                        dataType = integerDataType(),
                        isRequired = false,
                        codec = FormParameterCodec("page", true)),
        ParameterSchema(element = Header("X-Correlation-Id"),
                        dataType = integerDataType(),
                        isRequired = true,
                        codec = SimpleParameterCodec("X-Correlation-Id", false)),
        ParameterSchema(element = Cookie("token"),
                        dataType = integerDataType(),
                        isRequired = false,
                        codec = FormParameterCodec("token", true))
      ),
      bodies = listOf(
        BodySchema(contentType = ContentType("application/json"),
                   dataType = objectDataType(properties = mapOf("name" to stringDataType())),
                   isRequired = true,
                   serde = JsonSerde)
      )
    )

    // When
    val cases = VerificationCaseFactory.create(apiOperation)
    val typeMismatchCases = cases.filterIsInstance<TypeMismatch>()

    // Then
    assert(typeMismatchCases.size == 5)
    assert(typeMismatchCases.count { it.mutatedElement is MutatedElement.Parameter && it.mutatedElement.element is PathParam } == 1)
    assert(typeMismatchCases.count { it.mutatedElement is MutatedElement.Parameter && it.mutatedElement.element is QueryParam } == 1)
    assert(typeMismatchCases.count { it.mutatedElement is MutatedElement.Parameter && it.mutatedElement.element is Header } == 1)
    assert(typeMismatchCases.count { it.mutatedElement is MutatedElement.Parameter && it.mutatedElement.element is Cookie } == 1)
    assert(typeMismatchCases.count { it.mutatedElement == MutatedElement.Body } == 1)
  }

  @Test
  fun `picks first mutable parameter when category has multiple parameters`() {
    // Given
    val apiOperation = apiOperationWith400(
      parameters = listOf(
        ParameterSchema(element = QueryParam("name"),
                        dataType = stringDataType(),
                        isRequired = false,
                        codec = FormParameterCodec("name", true)),
        ParameterSchema(element = QueryParam("page"),
                        dataType = integerDataType(),
                        isRequired = false,
                        codec = FormParameterCodec("page", true)),
        ParameterSchema(element = QueryParam("limit"),
                        dataType = integerDataType(),
                        isRequired = false,
                        codec = FormParameterCodec("limit", true))
      )
    )

    // When
    val cases = VerificationCaseFactory.create(apiOperation)
    val typeMismatchCases = cases.filterIsInstance<TypeMismatch>()

    // Then
    assert(typeMismatchCases.size == 1)
    assert(typeMismatchCases[0].mutatedElement == MutatedElement.Parameter(QueryParam("page")))
  }

  @Test
  fun `does not generate parameter type mismatch case when all parameters in category are non-mutable`() {
    // Given
    val apiOperation = apiOperationWith400(
      parameters = listOf(
        ParameterSchema(element = QueryParam("name"),
                        dataType = stringDataType(),
                        isRequired = false,
                        codec = FormParameterCodec("name", true)),
        ParameterSchema(element = QueryParam("filter"),
                        dataType = stringDataType(),
                        isRequired = false,
                        codec = FormParameterCodec("filter", true))
      )
    )

    // When
    val cases = VerificationCaseFactory.create(apiOperation)

    // Then
    assert(cases.none { it is TypeMismatch })
  }

  @Test
  fun `generates type mismatch case for body when operation has 400 response and mutable request body`() {
    // Given
    val apiOperation = ApiOperation(
      path = "/users",
      method = "POST",
      requestSchema = RequestSchema(
        parameters = emptyList(),
        bodies = listOf(
          BodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("name" to stringDataType())),
            isRequired = true,
            serde = JsonSerde
          )
        )
      ),
      responseSchemas = ResponseSchemas(byStatusCode = mapOf(
        200 to ResponseSchema(headers = emptyList(), bodies = listOf(
          BodySchema(contentType = ContentType("application/json"),
                     dataType = objectDataType(properties = mapOf("id" to integerDataType())),
                     isRequired = true,
                     serde = JsonSerde)
        )),
        400 to ResponseSchema(headers = emptyList(), bodies = listOf(
          BodySchema(contentType = ContentType("application/json"),
                     dataType = objectDataType(properties = mapOf("error" to stringDataType())),
                     isRequired = true,
                     serde = JsonSerde)
        ))
      )),
      scenarios = emptyList()
    )

    // When
    val cases = VerificationCaseFactory.create(apiOperation)
    val typeMismatchCases = cases.filterIsInstance<TypeMismatch>()

    // Then
    assert(typeMismatchCases.size == 1)
    assert(typeMismatchCases[0].mutatedElement == MutatedElement.Body)
    assert(typeMismatchCases[0].mutatedValue == "<<not a object>>")
    assert(typeMismatchCases[0].requestContentType == ContentType("application/json"))
    assert(typeMismatchCases[0].responseSchema == apiOperation.responseSchemas.badRequestResponse())
    assert(typeMismatchCases[0].path == "/users")
    assert(typeMismatchCases[0].method == "POST")
  }

  @Test
  fun `does not generate type mismatch case when operation has no 400 response`() {
    // Given
    val apiOperation = ApiOperation(
      path = "/users",
      method = "POST",
      requestSchema = RequestSchema(
        parameters = emptyList(),
        bodies = listOf(
          BodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("name" to stringDataType())),
            isRequired = true,
            serde = JsonSerde
          )
        )
      ),
      responseSchemas = ResponseSchemas(byStatusCode = mapOf(
        200 to ResponseSchema(headers = emptyList(), bodies = listOf(
          BodySchema(contentType = ContentType("application/json"),
                     dataType = objectDataType(properties = mapOf("id" to integerDataType())),
                     isRequired = true,
                     serde = JsonSerde)
        ))
      )),
      scenarios = emptyList()
    )

    // When
    val cases = VerificationCaseFactory.create(apiOperation)

    // Then
    assert(cases.none { it is TypeMismatch })
  }

  @Test
  fun `does not generate type mismatch case for body when all request body data types are non-mutable`() {
    // Given
    val apiOperation = ApiOperation(
      path = "/users",
      method = "POST",
      requestSchema = RequestSchema(
        parameters = emptyList(),
        bodies = listOf(
          BodySchema(
            contentType = ContentType("text/plain"),
            dataType = stringDataType(),
            isRequired = true,
            serde = JsonSerde
          )
        )
      ),
      responseSchemas = ResponseSchemas(byStatusCode = mapOf(
        200 to ResponseSchema(headers = emptyList(), bodies = emptyList()),
        400 to ResponseSchema(headers = emptyList(), bodies = listOf(
          BodySchema(contentType = ContentType("application/json"),
                     dataType = objectDataType(properties = mapOf("error" to stringDataType())),
                     isRequired = true,
                     serde = JsonSerde)
        ))
      )),
      scenarios = emptyList()
    )

    // When
    val cases = VerificationCaseFactory.create(apiOperation)

    // Then
    assert(cases.none { it is TypeMismatch })
  }

  @Test
  fun `does not generate type mismatch case when operation has no request body and no mutable parameters`() {
    // Given
    val apiOperation = ApiOperation(
      path = "/users",
      method = "GET",
      requestSchema = RequestSchema(
        parameters = emptyList(),
        bodies = emptyList()
      ),
      responseSchemas = ResponseSchemas(byStatusCode = mapOf(
        200 to ResponseSchema(headers = emptyList(), bodies = listOf(
          BodySchema(contentType = ContentType("application/json"),
                     dataType = objectDataType(properties = mapOf("id" to integerDataType())),
                     isRequired = true,
                     serde = JsonSerde)
        )),
        400 to ResponseSchema(headers = emptyList(), bodies = listOf(
          BodySchema(contentType = ContentType("application/json"),
                     dataType = objectDataType(properties = mapOf("error" to stringDataType())),
                     isRequired = true,
                     serde = JsonSerde)
        ))
      )),
      scenarios = emptyList()
    )

    // When
    val cases = VerificationCaseFactory.create(apiOperation)

    // Then
    assert(cases.none { it is TypeMismatch })
  }

  @Test
  fun `generates type mismatch case for first mutable body when multiple content types exist`() {
    // Given
    val apiOperation = ApiOperation(
      path = "/data",
      method = "POST",
      requestSchema = RequestSchema(
        parameters = emptyList(),
        bodies = listOf(
          BodySchema(
            contentType = ContentType("text/plain"),
            dataType = stringDataType(),
            isRequired = true,
            serde = JsonSerde
          ),
          BodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("value" to stringDataType())),
            isRequired = true,
            serde = JsonSerde
          )
        )
      ),
      responseSchemas = ResponseSchemas(byStatusCode = mapOf(
        200 to ResponseSchema(headers = emptyList(), bodies = emptyList()),
        400 to ResponseSchema(headers = emptyList(), bodies = listOf(
          BodySchema(contentType = ContentType("application/json"),
                     dataType = objectDataType(properties = mapOf("error" to stringDataType())),
                     isRequired = true,
                     serde = JsonSerde)
        ))
      )),
      scenarios = emptyList()
    )

    // When
    val cases = VerificationCaseFactory.create(apiOperation)
    val typeMismatchCases = cases.filterIsInstance<TypeMismatch>()

    // Then
    assert(typeMismatchCases.size == 1)
    assert(typeMismatchCases[0].requestContentType == ContentType("application/json"))
    assert(typeMismatchCases[0].mutatedValue == "<<not a object>>")
  }

  @Test
  fun `generates type mismatch case alongside explicit 400 scenarios`() {
    // Given
    val apiOperation = ApiOperation(
      path = "/users",
      method = "POST",
      requestSchema = RequestSchema(
        parameters = emptyList(),
        bodies = listOf(
          BodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("name" to stringDataType())),
            isRequired = true,
            serde = JsonSerde
          )
        )
      ),
      responseSchemas = ResponseSchemas(byStatusCode = mapOf(
        201 to ResponseSchema(headers = emptyList(), bodies = listOf(
          BodySchema(contentType = ContentType("application/json"),
                     dataType = objectDataType(properties = mapOf("id" to integerDataType())),
                     isRequired = true,
                     serde = JsonSerde)
        )),
        400 to ResponseSchema(headers = emptyList(), bodies = listOf(
          BodySchema(contentType = ContentType("application/json"),
                     dataType = objectDataType(properties = mapOf("error" to stringDataType())),
                     isRequired = true,
                     serde = JsonSerde)
        ))
      )),
      scenarios = listOf(
        Scenario(
          path = "/users",
          method = "POST",
          key = "invalidUser",
          statusCode = 400,
          request = ScenarioRequest(
            parameterValues = emptyMap(),
            body = ScenarioBody(contentType = ContentType("application/json"), value = mapOf("name" to ""))
          ),
          response = ScenarioResponse(
            headers = emptyMap(),
            body = ScenarioBody(contentType = ContentType("application/json"),
                                value = mapOf("error" to "name is required"))
          )
        )
      )
    )

    // When
    val cases = VerificationCaseFactory.create(apiOperation)

    // Then
    val scenarioCases = cases.filterIsInstance<ScenarioBased>()
    val typeMismatchCases = cases.filterIsInstance<TypeMismatch>()
    assert(scenarioCases.size == 1)
    assert(typeMismatchCases.size == 1)
    assert(scenarioCases[0].displayName.contains("invalidUser"))
    assert(typeMismatchCases[0].mutatedElement == MutatedElement.Body)
  }


  @Test
  fun `does not generate body type mismatch for form-urlencoded with all optional properties`() {
    // Given
    val apiOperation = apiOperationWith400(
      bodies = listOf(
        BodySchema(
          contentType = ContentType("application/x-www-form-urlencoded"),
          dataType = objectDataType(properties = mapOf("name" to stringDataType())),
          isRequired = true,
          serde = JsonSerde
        )
      )
    )

    // When
    val cases = VerificationCaseFactory.create(apiOperation)

    // Then
    assert(cases.none { it is TypeMismatch })
  }

  @Test
  fun `generates body type mismatch for form-urlencoded with required properties`() {
    // Given
    val apiOperation = apiOperationWith400(
      bodies = listOf(
        BodySchema(
          contentType = ContentType("application/x-www-form-urlencoded"),
          dataType = objectDataType(
            properties = mapOf("name" to stringDataType()),
            requiredProperties = setOf("name")
          ),
          isRequired = true,
          serde = JsonSerde
        )
      )
    )

    // When
    val cases = VerificationCaseFactory.create(apiOperation)
    val typeMismatchCases = cases.filterIsInstance<TypeMismatch>()

    // Then
    assert(typeMismatchCases.size == 1)
    assert(typeMismatchCases[0].mutatedElement == MutatedElement.Body)
  }

  @Test
  fun `generates body type mismatch for form-urlencoded with additionalProperties false`() {
    // Given
    val apiOperation = apiOperationWith400(
      bodies = listOf(
        BodySchema(
          contentType = ContentType("application/x-www-form-urlencoded"),
          dataType = objectDataType(
            properties = mapOf("name" to stringDataType()),
            allowAdditionalProperties = false
          ),
          isRequired = true,
          serde = JsonSerde
        )
      )
    )

    // When
    val cases = VerificationCaseFactory.create(apiOperation)
    val typeMismatchCases = cases.filterIsInstance<TypeMismatch>()

    // Then
    assert(typeMismatchCases.size == 1)
    assert(typeMismatchCases[0].mutatedElement == MutatedElement.Body)
  }

  @Test
  fun `does not generate parameter type mismatch for deepObject with all optional properties`() {
    // Given
    val apiOperation = apiOperationWith400(
      parameters = listOf(
        ParameterSchema(element = QueryParam("filter"),
                        dataType = objectDataType(properties = mapOf("name" to stringDataType())),
                        isRequired = false,
                        codec = DeepObjectParameterCodec("filter"))
      )
    )

    // When
    val cases = VerificationCaseFactory.create(apiOperation)

    // Then
    assert(cases.none { it is TypeMismatch })
  }

  private fun apiOperationWith400(
    path: String = "/users",
    method: String = "POST",
    parameters: List<ParameterSchema> = emptyList(),
    bodies: List<BodySchema> = emptyList()
  ) = ApiOperation(
    path = path,
    method = method,
    requestSchema = RequestSchema(parameters = parameters, bodies = bodies),
    responseSchemas = ResponseSchemas(byStatusCode = mapOf(
      200 to ResponseSchema(headers = emptyList(), bodies = emptyList()),
      400 to ResponseSchema(headers = emptyList(), bodies = listOf(
        BodySchema(contentType = ContentType("application/json"),
                   dataType = objectDataType(properties = mapOf("error" to stringDataType())),
                   isRequired = true,
                   serde = JsonSerde)
      ))
    )),
    scenarios = emptyList()
  )
}
