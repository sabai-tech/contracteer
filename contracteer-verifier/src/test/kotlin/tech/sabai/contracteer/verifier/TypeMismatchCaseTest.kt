package tech.sabai.contracteer.verifier

import tech.sabai.contracteer.core.dsl.apiOperation
import tech.sabai.contracteer.core.dsl.integerType
import tech.sabai.contracteer.core.dsl.objectType
import tech.sabai.contracteer.core.dsl.stringType
import tech.sabai.contracteer.core.operation.ContentType
import tech.sabai.contracteer.core.operation.ParameterElement.Cookie
import tech.sabai.contracteer.core.operation.ParameterElement.Header
import tech.sabai.contracteer.core.operation.ParameterElement.PathParam
import tech.sabai.contracteer.core.operation.ParameterElement.QueryParam
import kotlin.test.Test

class TypeMismatchCaseTest {

  @Test
  fun `display name for body type mismatch`() {
    // Given
    val case = typeMismatchCase(
      path = "/users",
      method = "POST",
      mutatedElement = MutatedElement.Body,
      mutatedValue = "<<not a object>>"
    )

    // When
    val displayName = case.displayName

    // Then
    assert(displayName == "POST /users -> 400 (auto: body type mismatch)")
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

    // When
    val displayName = case.displayName

    // Then
    assert(displayName == "GET /users/{id} -> 400 (auto: path 'id' type mismatch)")
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

    // When
    val displayName = case.displayName

    // Then
    assert(displayName == "GET /users -> 400 (auto: query 'page' type mismatch)")
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

    // When
    val displayName = case.displayName

    // Then
    assert(displayName == "GET /users -> 400 (auto: header 'X-Request-Id' type mismatch)")
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

    // When
    val displayName = case.displayName

    // Then
    assert(displayName == "GET /users -> 400 (auto: cookie 'session_ttl' type mismatch)")
  }

  private fun typeMismatchCase(
    path: String = "/users",
    method: String = "POST",
    mutatedElement: MutatedElement,
    mutatedValue: String
  ): VerificationCase.TypeMismatch {
    val op = apiOperation(method, path) {
      request {
        pathParam("id", integerType())
        jsonBody(objectType { properties { "name" to stringType() } })
      }
      response(400) {
        jsonBody(objectType { properties { "error" to stringType() } })
      }
    }
    return VerificationCase.TypeMismatch(
      path = path,
      method = method,
      requestContentType = ContentType("application/json"),
      responseContentType = ContentType("application/json"),
      requestSchema = op.requestSchema,
      responseSchema = op.responseSchemas.responseFor(400)!!,
      mutatedElement = mutatedElement,
      mutatedValue = mutatedValue
    )
  }
}
