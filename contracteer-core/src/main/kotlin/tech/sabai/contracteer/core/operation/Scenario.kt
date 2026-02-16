package tech.sabai.contracteer.core.operation

/**
 * A named example-based pairing of request values and response values for a specific status code.
 *
 * Scenarios are derived from OpenAPI example keys. Each scenario contains only the values
 * explicitly provided in the specification; parameters without example values for a given
 * key are absent from the scenario.
 *
 * @param path the URL path pattern
 * @param method the HTTP method
 * @param key the example key identifying this scenario
 * @param statusCode the expected response status code
 * @param request the request values for this scenario
 * @param response the response values for this scenario
 */
data class Scenario(
  val path: String,
  val method: String,
  val key: String,
  val statusCode: Int,
  val request: ScenarioRequest,
  val response: ScenarioResponse
)

/**
 * The request side of a [Scenario]: parameter values and an optional body.
 *
 * @param parameterValues example values keyed by [ParameterElement], only for parameters
 *        that have an example for this scenario's key
 * @param body the request body value and content type, or null if not provided
 */
data class ScenarioRequest(
  val parameterValues: Map<ParameterElement, Any?>,
  val body: ScenarioBody?
)

/**
 * The response side of a [Scenario]: header values and an optional body.
 *
 * @param headers example values keyed by [ParameterElement.Header],
 *        only for headers that have an example for this scenario's key
 * @param body the response body value and content type, or null if not provided
 */
data class ScenarioResponse(
  val headers: Map<ParameterElement.Header, Any?>,
  val body: ScenarioBody?
)

/** A body value with its content type, used within a [Scenario]. */
data class ScenarioBody(
  val contentType: ContentType,
  val value: Any?
)
