package tech.sabai.contracteer.verifier

import tech.sabai.contracteer.core.dsl.apiOperation
import tech.sabai.contracteer.core.dsl.integerType
import tech.sabai.contracteer.core.dsl.objectType
import tech.sabai.contracteer.core.dsl.stringType
import tech.sabai.contracteer.core.serde.JsonSerde
import tech.sabai.contracteer.core.serde.PlainTextSerde
import tech.sabai.contracteer.verifier.VerificationCase.ScenarioBased
import tech.sabai.contracteer.verifier.VerificationCase.SchemaBased
import kotlin.test.Test

class VerificationCaseFactoryTest {

  @Test
  fun `creates scenario based cases from operation scenarios`() {
    // Given
    val apiOperation = apiOperation("GET", "/users/{id}") {
      request {
        pathParam("id", integerType())
      }

      response(200) {
        jsonBody(objectType {
          properties {
            "id" to integerType()
            "name" to stringType()
          }
        })
      }

      response(404) {}

      scenario("validUser", status = 200) {
        request { pathParam["id"] = 1 }
        response { jsonBody { "id" to 1; "name" to "John" } }
      }
      scenario("notFound", status = 404) {
        request { pathParam["id"] = 999 }
      }
    }

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
    val apiOperation = apiOperation("GET", "/products") {
      response(200) {
        jsonBody(objectType {
          properties { "items" to stringType() }
        })
      }
    }

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
    val apiOperation = apiOperation("POST", "/data") {
      request {
        jsonBody(objectType {
          properties { "value" to stringType() }
        })
        body("application/xml", stringType(), JsonSerde)
      }

      response(201) {
        jsonBody(objectType {
          properties { "result" to stringType() }
        })
        body("application/xml", stringType(), JsonSerde)
      }
    }

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
    val apiOperation = apiOperation("POST", "/orders") {
      request {
        jsonBody(objectType {
          properties { "product" to stringType() }
        })
      }
      response(201) {
        jsonBody(objectType {
          properties { "orderId" to stringType() }
        })
      }
      scenario("success", status = 201) {
        request { jsonBody { "product" to "laptop" } }
        response { jsonBody { "orderId" to "12345" } }
      }
    }

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
    val apiOperation = apiOperation("POST", "/upload") {
      request {
        jsonBody(objectType {
          properties { "name" to stringType() }
        })
        body(
          "application/xml",
          objectType { properties { "name" to stringType() } },
          PlainTextSerde
        )
      }
      response(200) {}
      scenario("K1", status = 200) {}
    }

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
    val apiOperation = apiOperation("POST", "/upload") {
      request {
        body(
          "multipart/form-data",
          objectType { properties { "file" to stringType() } },
          JsonSerde
        )
      }
      response(200) {}
      scenario("K1", status = 200) {}
    }

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
    val apiOperation = apiOperation("POST", "/search") {
      request {
        jsonBody(objectType {
          properties { "q" to stringType() }
        },
                 isRequired = false
        )
        body(
          "application/xml",
          objectType {
            properties { "q" to stringType() }
          },
          JsonSerde,
          isRequired = false
        )
      }

      response(200) {}

      scenario("K1", status = 200) {}
    }

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
    val apiOperation = apiOperation("GET", "/resources") {
      response(200) {
        jsonBody(objectType {
          properties { "data" to stringType() }
        })
      }
      response(202) {
        jsonBody(objectType {
          properties { "status" to stringType() }
        })
      }
    }

    // When
    val cases = VerificationCaseFactory.create(apiOperation)

    // Then
    assert(cases.isEmpty())
  }
}
