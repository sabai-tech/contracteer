package tech.sabai.contracteer.verifier

import tech.sabai.contracteer.core.operation.*
import tech.sabai.contracteer.core.operation.ParameterElement.*
import tech.sabai.contracteer.core.serde.PlainTextSerde
import tech.sabai.contracteer.verifier.TestFixture.integerDataType
import tech.sabai.contracteer.verifier.TestFixture.objectDataType
import tech.sabai.contracteer.verifier.TestFixture.stringDataType
import kotlin.test.Test

class TypeMismatchTest {

  @Test
  fun `display name for body type mismatch`() {
    // Given
    val case = typeMismatchCase(
      path = "/users",
      method = "POST",
      mutatedElement = MutatedElement.Body,
      mutatedValue = "<<not a object>>"
    )

    // Then
    assert(case.displayName == "POST /users -> 400 (auto: body type mismatch)")
  }

  @Test
  fun `display name for path parameter type mismatch`() {
    // Given
    val case = typeMismatchCase(
      path = "/users/{id}",
      method = "GET",
      mutatedElement = MutatedElement.Parameter(PathParam("id")),
      mutatedValue = "<<not a integer>>"
    )

    // Then
    assert(case.displayName == "GET /users/{id} -> 400 (auto: path 'id' type mismatch)")
  }

  @Test
  fun `display name for query parameter type mismatch`() {
    // Given
    val case = typeMismatchCase(
      path = "/users",
      method = "GET",
      mutatedElement = MutatedElement.Parameter(QueryParam("page")),
      mutatedValue = "<<not a integer>>"
    )

    // Then
    assert(case.displayName == "GET /users -> 400 (auto: query 'page' type mismatch)")
  }

  @Test
  fun `display name for header parameter type mismatch`() {
    // Given
    val case = typeMismatchCase(
      path = "/users",
      method = "GET",
      mutatedElement = MutatedElement.Parameter(Header("X-Request-Id")),
      mutatedValue = "<<not a string/uuid>>"
    )

    // Then
    assert(case.displayName == "GET /users -> 400 (auto: header 'X-Request-Id' type mismatch)")
  }

  @Test
  fun `display name for cookie parameter type mismatch`() {
    // Given
    val case = typeMismatchCase(
      path = "/users",
      method = "GET",
      mutatedElement = MutatedElement.Parameter(Cookie("session_ttl")),
      mutatedValue = "<<not a integer>>"
    )

    // Then
    assert(case.displayName == "GET /users -> 400 (auto: cookie 'session_ttl' type mismatch)")
  }

  private fun typeMismatchCase(
    path: String = "/users",
    method: String = "POST",
    mutatedElement: MutatedElement,
    mutatedValue: String
  ) = VerificationCase.TypeMismatch(
    path = path,
    method = method,
    requestContentType = ContentType("application/json"),
    responseContentType = ContentType("application/json"),
    requestSchema = RequestSchema(
      parameters = listOf(
        ParameterSchema(
          element = PathParam("id"),
          dataType = integerDataType(),
          isRequired = true,
          serde = PlainTextSerde
        )
      ),
      bodies = listOf(
        BodySchema(
          contentType = ContentType("application/json"),
          dataType = objectDataType(properties = mapOf("name" to stringDataType())),
          isRequired = true
        )
      )
    ),
    responseSchema = ResponseSchema(
      headers = emptyList(),
      bodies = listOf(
        BodySchema(
          contentType = ContentType("application/json"),
          dataType = objectDataType(properties = mapOf("error" to stringDataType())),
          isRequired = true
        )
      )
    ),
    mutatedElement = mutatedElement,
    mutatedValue = mutatedValue
  )
}
