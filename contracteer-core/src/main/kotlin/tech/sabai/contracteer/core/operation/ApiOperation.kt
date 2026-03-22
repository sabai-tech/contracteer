package tech.sabai.contracteer.core.operation

import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.serde.Serde

/**
 * Represents a single API operation (path + HTTP method) extracted from an OpenAPI specification.
 *
 * Contains the structural schemas describing what the operation accepts and returns,
 * and the scenarios providing named example-based request/response pairings.
 *
 * @param path the URL path pattern (e.g. `/products/{id}`)
 * @param method the HTTP method (e.g. `get`, `post`)
 * @param requestSchema the structural definition of what this operation accepts
 * @param responses a map of HTTP status codes to their response schemas
 * @param scenarios named example-based pairings of request and response values
 */
data class ApiOperation(
  val path: String,
  val method: String,
  val requestSchema: RequestSchema,
  internal val responses: Map<Int, ResponseSchema>,
  internal val defaultResponse: ResponseSchema? = null,
  val scenarios: List<Scenario>
) {
  /** Returns the response schema for the given [statusCode], falling back to [defaultResponse] if not explicitly defined. */
  fun responseFor(statusCode: Int): ResponseSchema? = responses[statusCode] ?: defaultResponse

  /** Returns all 2xx response schemas, keyed by status code. */
  fun successResponses(): Map<Int, ResponseSchema> = responses.filterKeys { it in 200..299 }

  /** Returns the 400 Bad Request response schema, falling back to [defaultResponse] if not explicitly defined. */
  fun badRequestResponse(): ResponseSchema? = responses[400] ?: defaultResponse

  /** Returns true if at least one response schema is defined. */
  fun hasResponses(): Boolean = responses.isNotEmpty()
}

/**
 * Structural definition of what an API operation accepts: parameters and request bodies.
 */
data class RequestSchema(
  val parameters: List<ParameterSchema>,
  val bodies: List<BodySchema>
) {
  val pathParameters get() = parameters.filter { it.element is ParameterElement.PathParam }
  val queryParameters get() = parameters.filter { it.element is ParameterElement.QueryParam }
  val headers get() = parameters.filter { it.element is ParameterElement.Header }
  val cookies get() = parameters.filter { it.element is ParameterElement.Cookie }
}

/**
 * Structural definition of what an API operation returns for a given status code:
 * response headers and response bodies.
 */
data class ResponseSchema(
  val headers: List<ParameterSchema>,
  val bodies: List<BodySchema>
)

/**
 * Schema for a single request parameter or response header.
 *
 * @param element identifies the parameter location and name
 * @param dataType the expected data type and constraints
 * @param isRequired whether the parameter must be present
 * @param serde the serializer/deserializer for this parameter's wire format
 */
data class ParameterSchema(
  val element: ParameterElement,
  val dataType: DataType<out Any>,
  val isRequired: Boolean,
  val serde: Serde
)

/**
 * Schema for a request or response body.
 *
 * @param contentType the media type (e.g. `application/json`)
 * @param dataType the expected data type and constraints
 * @param isRequired whether the body must be present
 */
data class BodySchema(
  val contentType: ContentType,
  val dataType: DataType<out Any>,
  val isRequired: Boolean
)
