package tech.sabai.contracteer.verifier

import io.mockk.every
import io.mockk.mockk
import org.http4k.core.Response
import org.http4k.core.Status
import tech.sabai.contracteer.core.dsl.*
import tech.sabai.contracteer.core.operation.ContentType
import tech.sabai.contracteer.core.operation.ResponseSchema
import tech.sabai.contracteer.core.serde.PlainTextSerde
import kotlin.test.Test

class ResponseValidatorTest {

  @Test
  fun `validates successfully when status codes match`() {
    // Given
    val target = schemaBasedCase(statusCode = 201)
    val response = mockResponse(Status.CREATED)

    // When
    val result = ResponseValidator.validate(target, response)

    // Then
    assert(result.isSuccess())
  }

  @Test
  fun `fails when status code does not match`() {
    // Given
    val target = schemaBasedCase(statusCode = 200)
    val response = mockResponse(Status.CREATED)

    // When
    val result = ResponseValidator.validate(target, response)

    // Then
    assert(result.isFailure())
    assert(result.errors().size == 1)
    assert(result.errors().first().contains("200"))
    assert(result.errors().first().contains("201"))
  }

  @Test
  fun `validates successfully with required headers using serde deserialization`() {
    // Given
    val target = schemaBasedCase(statusCode = 200) {
      response(200) {
        header("X-Count", integerType(), isRequired = true)
        header("X-Name", stringType(), isRequired = true)
      }
    }
    val response = mockResponse(
      status = Status.OK,
      headers = listOf("X-Count" to "42", "X-Name" to "test")
    )

    // When
    val result = ResponseValidator.validate(target, response)

    // Then
    assert(result.isSuccess())
  }

  @Test
  fun `validates successfully with optional headers missing`() {
    // Given
    val target = schemaBasedCase(statusCode = 200) {
      response(200) {
        header("X-Optional", stringType(), isRequired = false)
      }
    }
    val response = mockResponse(Status.OK)

    // When
    val result = ResponseValidator.validate(target, response)

    // Then
    assert(result.isSuccess())
  }

  @Test
  fun `fails when required header is missing`() {
    // Given
    val target = schemaBasedCase(statusCode = 200) {
      response(200) {
        header("X-Required", stringType(), isRequired = true)
      }
    }
    val response = mockResponse(Status.OK)

    // When
    val result = ResponseValidator.validate(target, response)

    // Then
    assert(result.isFailure())
    assert(result.errors().size == 1)
    assert(result.errors().first() == "Response header 'X-Required' is missing")
  }

  @Test
  fun `fails when header value does not match datatype after deserialization`() {
    // Given
    val target = schemaBasedCase(statusCode = 200) {
      response(200) {
        header("X-Count", integerType(), isRequired = true)
      }
    }
    val response = mockResponse(
      status = Status.OK,
      headers = listOf("X-Count" to "not-a-number")
    )

    // When
    val result = ResponseValidator.validate(target, response)

    // Then
    assert(result.isFailure())
    assert(result.errors().size == 1)
    assert(result.errors().first().contains("X-Count"))
  }

  @Test
  fun `validates successfully with matching JSON body`() {
    // Given
    val target = schemaBasedCase(
      statusCode = 200,
      responseContentType = ContentType("application/json")
    ) {
      response(200) {
        jsonBody(objectType {
          properties {
            "id" to integerType()
            "name" to stringType()
          }
        })
      }
    }
    val response = mockResponse(
      status = Status.OK,
      headers = listOf("Content-Type" to "application/json"),
      contentType = "application/json",
      body = """{"id": 123, "name": "John"}"""
    )

    // When
    val result = ResponseValidator.validate(target, response)

    // Then
    assert(result.isSuccess())
  }

  @Test
  fun `validates successfully when no body expected and no body received`() {
    // Given
    val target = schemaBasedCase(method = "DELETE", statusCode = 204)
    val response = mockResponse(Status.NO_CONTENT)

    // When
    val result = ResponseValidator.validate(target, response)

    // Then
    assert(result.isSuccess())
  }

  @Test
  fun `fails when content-type does not match expected`() {
    // Given
    val target = schemaBasedCase(
      statusCode = 200,
      responseContentType = ContentType("application/json")
    ) {
      response(200) {
        jsonBody(objectType { properties { "name" to stringType() } })
      }
    }
    val response = mockResponse(
      status = Status.OK,
      headers = listOf("Content-Type" to "text/plain"),
      contentType = "text/plain",
      body = "plain text"
    )

    // When
    val result = ResponseValidator.validate(target, response)

    // Then
    assert(result.isFailure())
    assert(result.errors().size == 1)
    assert(result.errors().first().contains("text/plain"))
    assert(result.errors().first().contains("application/json"))
  }

  @Test
  fun `fails when body is expected but content-type is missing`() {
    // Given
    val target = schemaBasedCase(
      statusCode = 200,
      responseContentType = ContentType("application/json")
    ) {
      response(200) {
        jsonBody(objectType { properties { "name" to stringType() } })
      }
    }
    val response = mockResponse(
      status = Status.OK,
      body = ""
    )

    // When
    val result = ResponseValidator.validate(target, response)

    // Then
    assert(result.isFailure())
    assert(result.errors().size == 1)
    assert(result.errors().first().contains("Content-Type is missing"))
  }

  @Test
  fun `fails when no body expected but content-type is present`() {
    // Given
    val target = schemaBasedCase(method = "DELETE", statusCode = 204)
    val response = mockResponse(
      status = Status.NO_CONTENT,
      headers = listOf("Content-Type" to "application/json"),
      contentType = "application/json",
      body = "{}"
    )

    // When
    val result = ResponseValidator.validate(target, response)

    // Then
    assert(result.isFailure())
    assert(result.errors().size == 1)
    assert(result.errors().first().contains("Expected no Content-Type"))
  }

  @Test
  fun `validates with multiple content types and finds matching schema`() {
    // Given
    val target = schemaBasedCase(
      statusCode = 200,
      responseContentType = ContentType("application/xml")
    ) {
      response(200) {
        jsonBody(objectType { properties { "name" to stringType() } })
        body("application/xml", stringType(), PlainTextSerde)
      }
    }
    val response = mockResponse(
      status = Status.OK,
      headers = listOf("Content-Type" to "application/xml"),
      contentType = "application/xml",
      body = "<user><name>John</name></user>"
    )

    // When
    val result = ResponseValidator.validate(target, response)

    // Then
    assert(result.isSuccess())
  }

  @Test
  fun `full validation with status code headers and body`() {
    // Given
    val target = schemaBasedCase(
      method = "POST",
      statusCode = 201,
      requestContentType = ContentType("application/json"),
      responseContentType = ContentType("application/json")
    ) {
      request {
        jsonBody(objectType { properties { "email" to stringType() } })
      }
      response(201) {
        header("X-Total-Count", integerType(), isRequired = true)
        jsonBody(objectType {
          properties {
            "id" to integerType()
            "email" to stringType()
          }
          required("id", "email")
        })
      }
    }
    val response = mockResponse(
      status = Status.CREATED,
      headers = listOf(
        "X-Total-Count" to "42",
        "Content-Type" to "application/json"
      ),
      contentType = "application/json",
      body = """{"id": 999, "email": "test@example.com"}"""
    )

    // When
    val result = ResponseValidator.validate(target, response)

    // Then
    assert(result.isSuccess())
  }

  // --- helpers ---

  private fun schemaBasedCase(method: String = "GET",
                              path: String = "/users",
                              statusCode: Int = 200,
                              requestContentType: ContentType? = null,
                              responseContentType: ContentType? = null,
                              block: ApiOperationBuilder.() -> Unit = {}): VerificationCase.SchemaBased {
    val op = apiOperation(method, path, block)
    return VerificationCase.SchemaBased(
      path = path,
      method = method,
      statusCode = statusCode,
      requestContentType = requestContentType,
      responseContentType = responseContentType,
      requestSchema = op.requestSchema,
      responseSchema = op.responseSchemas.responseFor(statusCode)
                       ?: ResponseSchema(headers = emptyList(), bodies = emptyList())
    )
  }

  private fun mockResponse(status: Status,
                           headers: List<Pair<String, String>> = emptyList(),
                           contentType: String? = null,
                           body: String? = null): Response {
    val response = mockk<Response>()
    every { response.status } returns status
    every { response.headers } returns headers
    every { response.header("Content-Type") } returns contentType
    if (body != null) every { response.bodyString() } returns body
    return response
  }
}
