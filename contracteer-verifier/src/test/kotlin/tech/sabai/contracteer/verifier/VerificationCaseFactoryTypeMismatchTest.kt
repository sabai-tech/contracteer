package tech.sabai.contracteer.verifier

import tech.sabai.contracteer.core.dsl.ApiOperationBuilder
import tech.sabai.contracteer.core.dsl.apiOperation
import tech.sabai.contracteer.core.dsl.deepObject
import tech.sabai.contracteer.core.dsl.integerType
import tech.sabai.contracteer.core.dsl.objectType
import tech.sabai.contracteer.core.dsl.stringType
import tech.sabai.contracteer.core.operation.ContentType
import tech.sabai.contracteer.core.operation.ParameterElement.Cookie
import tech.sabai.contracteer.core.operation.ParameterElement.Header
import tech.sabai.contracteer.core.operation.ParameterElement.PathParam
import tech.sabai.contracteer.core.operation.ParameterElement.QueryParam
import tech.sabai.contracteer.core.serde.JsonSerde
import tech.sabai.contracteer.verifier.VerificationCase.ScenarioBased
import tech.sabai.contracteer.verifier.VerificationCase.TypeMismatch
import kotlin.test.Test

class VerificationCaseFactoryTypeMismatchTest {

  @Test
  fun `generates type mismatch case for path parameter`() {
    // Given
    val apiOperation = apiOperationWith400 {
      request {
        pathParam("id", integerType())
      }
    }

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
    val apiOperation = apiOperationWith400 {
      request {
        queryParam("page", integerType())
      }
    }

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
    val apiOperation = apiOperationWith400 {
      request {
        header("X-Request-Id", integerType(), isRequired = true)
      }
    }

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
    val apiOperation = apiOperationWith400 {
      request {
        cookie("session_ttl", integerType())
      }
    }

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
    val apiOperation = apiOperationWith400 {
      request {
        pathParam("id", integerType())
        queryParam("page", integerType())
        header("X-Correlation-Id", integerType(), isRequired = true)
        cookie("token", integerType())
        jsonBody(objectType { properties { "name" to stringType() } })
      }
    }

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
    val apiOperation = apiOperationWith400 {
      request {
        queryParam("name", stringType())
        queryParam("page", integerType())
        queryParam("limit", integerType())
      }
    }

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
    val apiOperation = apiOperationWith400 {
      request {
        queryParam("name", stringType())
        queryParam("filter", stringType())
      }
    }

    // When
    val cases = VerificationCaseFactory.create(apiOperation)

    // Then
    assert(cases.none { it is TypeMismatch })
  }

  @Test
  fun `generates type mismatch case for body when operation has 400 response and mutable request body`() {
    // Given
    val apiOperation = apiOperation("POST", "/users") {
      request {
        jsonBody(objectType { properties { "name" to stringType() } })
      }
      response(200) {
        jsonBody(objectType { properties { "id" to integerType() } })
      }
      response(400) {
        jsonBody(objectType { properties { "error" to stringType() } })
      }
    }

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
    val apiOperation = apiOperation("POST", "/users") {
      request {
        jsonBody(objectType { properties { "name" to stringType() } })
      }
      response(200) {
        jsonBody(objectType { properties { "id" to integerType() } })
      }
    }

    // When
    val cases = VerificationCaseFactory.create(apiOperation)

    // Then
    assert(cases.none { it is TypeMismatch })
  }

  @Test
  fun `does not generate type mismatch case for body when all request body data types are non-mutable`() {
    // Given
    val apiOperation = apiOperation("POST", "/users") {
      request {
        body("text/plain", stringType(), JsonSerde)
      }
      response(200) {}
      response(400) {
        jsonBody(objectType { properties { "error" to stringType() } })
      }
    }

    // When
    val cases = VerificationCaseFactory.create(apiOperation)

    // Then
    assert(cases.none { it is TypeMismatch })
  }

  @Test
  fun `does not generate type mismatch case when operation has no request body and no mutable parameters`() {
    // Given
    val apiOperation = apiOperation("GET", "/users") {
      response(200) {
        jsonBody(objectType { properties { "id" to integerType() } })
      }
      response(400) {
        jsonBody(objectType { properties { "error" to stringType() } })
      }
    }

    // When
    val cases = VerificationCaseFactory.create(apiOperation)

    // Then
    assert(cases.none { it is TypeMismatch })
  }

  @Test
  fun `generates type mismatch case for first mutable body when multiple content types exist`() {
    // Given
    val apiOperation = apiOperation("POST", "/data") {
      request {
        body("text/plain", stringType(), JsonSerde)
        jsonBody(objectType { properties { "value" to stringType() } })
      }
      response(200) {}
      response(400) {
        jsonBody(objectType { properties { "error" to stringType() } })
      }
    }

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
    val apiOperation = apiOperation("POST", "/users") {
      request {
        jsonBody(objectType { properties { "name" to stringType() } })
      }
      response(201) {
        jsonBody(objectType { properties { "id" to integerType() } })
      }
      response(400) {
        jsonBody(objectType { properties { "error" to stringType() } })
      }
      scenario("invalidUser", status = 400) {
        request { jsonBody { "name" to "" } }
        response { jsonBody { "error" to "name is required" } }
      }
    }

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
    val apiOperation = apiOperationWith400 {
      request {
        // Intentional serde mismatch: application/x-www-form-urlencoded with JsonSerde (preserved from legacy).
        body(
          "application/x-www-form-urlencoded",
          objectType { properties { "name" to stringType() } },
          JsonSerde
        )
      }
    }

    // When
    val cases = VerificationCaseFactory.create(apiOperation)

    // Then
    assert(cases.none { it is TypeMismatch })
  }

  @Test
  fun `generates body type mismatch for form-urlencoded with required properties`() {
    // Given
    val apiOperation = apiOperationWith400 {
      request {
        body(
          "application/x-www-form-urlencoded",
          objectType {
            properties { "name" to stringType() }
            required("name")
          },
          JsonSerde
        )
      }
    }

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
    val apiOperation = apiOperationWith400 {
      request {
        body(
          "application/x-www-form-urlencoded",
          objectType(allowAdditionalProperties = false) {
            properties { "name" to stringType() }
          },
          JsonSerde
        )
      }
    }

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
    val apiOperation = apiOperationWith400 {
      request {
        queryParam(
          "filter",
          objectType { properties { "name" to stringType() } },
          codec = deepObject()
        )
      }
    }

    // When
    val cases = VerificationCaseFactory.create(apiOperation)

    // Then
    assert(cases.none { it is TypeMismatch })
  }

  private fun apiOperationWith400(
    path: String = "/users",
    method: String = "POST",
    block: ApiOperationBuilder.() -> Unit = {}
  ) = apiOperation(method, path) {
    block()
    response(200) {}
    response(400) {
      jsonBody(objectType { properties { "error" to stringType() } })
    }
  }
}
