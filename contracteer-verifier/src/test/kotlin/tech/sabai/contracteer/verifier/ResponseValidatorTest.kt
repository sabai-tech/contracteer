package tech.sabai.contracteer.verifier

import io.mockk.every
import io.mockk.mockk
import org.http4k.core.Response
import org.http4k.core.Status
import tech.sabai.contracteer.core.operation.*
import tech.sabai.contracteer.core.serde.PlainTextSerde
import tech.sabai.contracteer.verifier.TestFixture.integerDataType
import tech.sabai.contracteer.verifier.TestFixture.objectDataType
import tech.sabai.contracteer.verifier.TestFixture.stringDataType
import kotlin.test.Test

class ResponseValidatorTest {

  @Test
  fun `validates successfully when status codes match`() {
    // Given
    val target = VerificationCase.SchemaBased(
      path = "/users",
      method = "GET",
      statusCode = 201,
      requestContentType = null,
      responseContentType = null,
      requestSchema = RequestSchema(parameters = emptyList(), bodies = emptyList()),
      responseSchema = ResponseSchema(headers = emptyList(), bodies = emptyList())
    )

    val response = mockk<Response>()
    every { response.status } returns Status.CREATED
    every { response.headers } returns emptyList()
    every { response.header("Content-Type") } returns null

    // When
    val result = ResponseValidator.validate(target, response)

    // Then
    assert(result.isSuccess())
  }

  @Test
  fun `fails when status code does not match`() {
    // Given
    val target = VerificationCase.SchemaBased(
      path = "/users",
      method = "GET",
      statusCode = 200,
      requestContentType = null,
      responseContentType = null,
      requestSchema = RequestSchema(parameters = emptyList(), bodies = emptyList()),
      responseSchema = ResponseSchema(headers = emptyList(), bodies = emptyList())
    )

    val response = mockk<Response>()
    every { response.status } returns Status.CREATED
    every { response.headers } returns emptyList()
    every { response.header("Content-Type") } returns null

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
    val responseSchema = ResponseSchema(
      headers = listOf(
        ParameterSchema(
          element = ParameterElement.Header("X-Count"),
          dataType = integerDataType(),
          isRequired = true,
          serde = PlainTextSerde
        ),
        ParameterSchema(
          element = ParameterElement.Header("X-Name"),
          dataType = stringDataType(),
          isRequired = true,
          serde = PlainTextSerde
        )
      ),
      bodies = emptyList()
    )

    val target = VerificationCase.SchemaBased(
      path = "/users",
      method = "GET",
      statusCode = 200,
      requestContentType = null,
      responseContentType = null,
      requestSchema = RequestSchema(parameters = emptyList(), bodies = emptyList()),
      responseSchema = responseSchema
    )

    val response = mockk<Response>()
    every { response.status } returns Status.OK
    every { response.headers } returns listOf("X-Count" to "42", "X-Name" to "test")
    every { response.header("Content-Type") } returns null

    // When
    val result = ResponseValidator.validate(target, response)

    // Then
    assert(result.isSuccess())
  }

  @Test
  fun `validates successfully with optional headers missing`() {
    // Given
    val responseSchema = ResponseSchema(
      headers = listOf(
        ParameterSchema(
          element = ParameterElement.Header("X-Optional"),
          dataType = stringDataType(),
          isRequired = false,
          serde = PlainTextSerde
        )
      ),
      bodies = emptyList()
    )

    val target = VerificationCase.SchemaBased(
      path = "/users",
      method = "GET",
      statusCode = 200,
      requestContentType = null,
      responseContentType = null,
      requestSchema = RequestSchema(parameters = emptyList(), bodies = emptyList()),
      responseSchema = responseSchema
    )

    val response = mockk<Response>()
    every { response.status } returns Status.OK
    every { response.headers } returns emptyList()
    every { response.header("Content-Type") } returns null

    // When
    val result = ResponseValidator.validate(target, response)

    // Then
    assert(result.isSuccess())
  }

  @Test
  fun `fails when required header is missing`() {
    // Given
    val responseSchema = ResponseSchema(
      headers = listOf(
        ParameterSchema(
          element = ParameterElement.Header("X-Required"),
          dataType = stringDataType(),
          isRequired = true,
          serde = PlainTextSerde
        )
      ),
      bodies = emptyList()
    )

    val target = VerificationCase.SchemaBased(
      path = "/users",
      method = "GET",
      statusCode = 200,
      requestContentType = null,
      responseContentType = null,
      requestSchema = RequestSchema(parameters = emptyList(), bodies = emptyList()),
      responseSchema = responseSchema
    )

    val response = mockk<Response>()
    every { response.status } returns Status.OK
    every { response.headers } returns emptyList()
    every { response.header("Content-Type") } returns null

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
    val responseSchema = ResponseSchema(
      headers = listOf(
        ParameterSchema(
          element = ParameterElement.Header("X-Count"),
          dataType = integerDataType(),
          isRequired = true,
          serde = PlainTextSerde
        )
      ),
      bodies = emptyList()
    )

    val target = VerificationCase.SchemaBased(
      path = "/users",
      method = "GET",
      statusCode = 200,
      requestContentType = null,
      responseContentType = null,
      requestSchema = RequestSchema(parameters = emptyList(), bodies = emptyList()),
      responseSchema = responseSchema
    )

    val response = mockk<Response>()
    every { response.status } returns Status.OK
    every { response.headers } returns listOf("X-Count" to "not-a-number")
    every { response.header("Content-Type") } returns null

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
    val responseSchema = ResponseSchema(
      headers = emptyList(),
      bodies = listOf(
        BodySchema(
          contentType = ContentType("application/json"),
          dataType = objectDataType(
            properties = mapOf(
              "id" to integerDataType(),
              "name" to stringDataType()
            )
          ),
          isRequired = true
        )
      )
    )

    val target = VerificationCase.SchemaBased(
      path = "/users",
      method = "GET",
      statusCode = 200,
      requestContentType = null,
      responseContentType = ContentType("application/json"),
      requestSchema = RequestSchema(parameters = emptyList(), bodies = emptyList()),
      responseSchema = responseSchema
    )

    val response = mockk<Response>()
    every { response.status } returns Status.OK
    every { response.headers } returns listOf("Content-Type" to "application/json")
    every { response.header("Content-Type") } returns "application/json"
    every { response.bodyString() } returns """{"id": 123, "name": "John"}"""

    // When
    val result = ResponseValidator.validate(target, response)

    // Then
    assert(result.isSuccess())
  }

  @Test
  fun `validates successfully when no body expected and no body received`() {
    // Given
    val responseSchema = ResponseSchema(
      headers = emptyList(),
      bodies = emptyList()
    )

    val target = VerificationCase.SchemaBased(
      path = "/users",
      method = "DELETE",
      statusCode = 204,
      requestContentType = null,
      responseContentType = null,
      requestSchema = RequestSchema(parameters = emptyList(), bodies = emptyList()),
      responseSchema = responseSchema
    )

    val response = mockk<Response>()
    every { response.status } returns Status.NO_CONTENT
    every { response.headers } returns emptyList()
    every { response.header("Content-Type") } returns null

    // When
    val result = ResponseValidator.validate(target, response)

    // Then
    assert(result.isSuccess())
  }

  @Test
  fun `fails when content-type does not match expected`() {
    // Given
    val responseSchema = ResponseSchema(
      headers = emptyList(),
      bodies = listOf(
        BodySchema(
          contentType = ContentType("application/json"),
          dataType = objectDataType(properties = mapOf("name" to stringDataType())),
          isRequired = true
        )
      )
    )

    val target = VerificationCase.SchemaBased(
      path = "/users",
      method = "GET",
      statusCode = 200,
      requestContentType = null,
      responseContentType = ContentType("application/json"),
      requestSchema = RequestSchema(parameters = emptyList(), bodies = emptyList()),
      responseSchema = responseSchema
    )

    val response = mockk<Response>()
    every { response.status } returns Status.OK
    every { response.headers } returns listOf("Content-Type" to "text/plain")
    every { response.header("Content-Type") } returns "text/plain"
    every { response.bodyString() } returns "plain text"

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
    val responseSchema = ResponseSchema(
      headers = emptyList(),
      bodies = listOf(
        BodySchema(
          contentType = ContentType("application/json"),
          dataType = objectDataType(properties = mapOf("name" to stringDataType())),
          isRequired = true
        )
      )
    )

    val target = VerificationCase.SchemaBased(
      path = "/users",
      method = "GET",
      statusCode = 200,
      requestContentType = null,
      responseContentType = ContentType("application/json"),
      requestSchema = RequestSchema(parameters = emptyList(), bodies = emptyList()),
      responseSchema = responseSchema
    )

    val response = mockk<Response>()
    every { response.status } returns Status.OK
    every { response.headers } returns emptyList()
    every { response.header("Content-Type") } returns null
    every { response.bodyString() } returns ""

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
    val responseSchema = ResponseSchema(
      headers = emptyList(),
      bodies = emptyList()
    )

    val target = VerificationCase.SchemaBased(
      path = "/users",
      method = "DELETE",
      statusCode = 204,
      requestContentType = null,
      responseContentType = null,
      requestSchema = RequestSchema(parameters = emptyList(), bodies = emptyList()),
      responseSchema = responseSchema
    )

    val response = mockk<Response>()
    every { response.status } returns Status.NO_CONTENT
    every { response.headers } returns listOf("Content-Type" to "application/json")
    every { response.header("Content-Type") } returns "application/json"
    every { response.bodyString() } returns "{}"

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
    val responseSchema = ResponseSchema(
      headers = emptyList(),
      bodies = listOf(
        BodySchema(
          contentType = ContentType("application/json"),
          dataType = objectDataType(properties = mapOf("name" to stringDataType())),
          isRequired = true
        ),
        BodySchema(
          contentType = ContentType("application/xml"),
          dataType = stringDataType(),
          isRequired = true
        )
      )
    )

    val target = VerificationCase.SchemaBased(
      path = "/users",
      method = "GET",
      statusCode = 200,
      requestContentType = null,
      responseContentType = ContentType("application/xml"),
      requestSchema = RequestSchema(parameters = emptyList(), bodies = emptyList()),
      responseSchema = responseSchema
    )

    val response = mockk<Response>()
    every { response.status } returns Status.OK
    every { response.headers } returns listOf("Content-Type" to "application/xml")
    every { response.header("Content-Type") } returns "application/xml"
    every { response.bodyString() } returns "<user><name>John</name></user>"

    // When
    val result = ResponseValidator.validate(target, response)

    // Then
    assert(result.isSuccess())
  }

  @Test
  fun `full validation with status code headers and body`() {
    // Given
    val responseSchema = ResponseSchema(
      headers = listOf(
        ParameterSchema(
          element = ParameterElement.Header("X-Total-Count"),
          dataType = integerDataType(),
          isRequired = true,
          serde = PlainTextSerde
        )
      ),
      bodies = listOf(
        BodySchema(
          contentType = ContentType("application/json"),
          dataType = objectDataType(
            properties = mapOf(
              "id" to integerDataType(),
              "email" to stringDataType()
            ),
            requiredProperties = setOf("id", "email")
          ),
          isRequired = true
        )
      )
    )

    val target = VerificationCase.SchemaBased(
      path = "/users",
      method = "POST",
      statusCode = 201,
      requestContentType = ContentType("application/json"),
      responseContentType = ContentType("application/json"),
      requestSchema = RequestSchema(
        parameters = emptyList(),
        bodies = listOf(
          BodySchema(
            contentType = ContentType("application/json"),
            dataType = objectDataType(properties = mapOf("email" to stringDataType())),
            isRequired = true
          )
        )
      ),
      responseSchema = responseSchema
    )

    val response = mockk<Response>()
    every { response.status } returns Status.CREATED
    every { response.headers } returns listOf(
      "X-Total-Count" to "42",
      "Content-Type" to "application/json"
    )
    every { response.header("Content-Type") } returns "application/json"
    every { response.bodyString() } returns """{"id": 999, "email": "test@example.com"}"""

    // When
    val result = ResponseValidator.validate(target, response)

    // Then
    assert(result.isSuccess())
  }
}
