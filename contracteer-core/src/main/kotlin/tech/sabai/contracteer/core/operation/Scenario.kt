package tech.sabai.contracteer.core.operation

import tech.sabai.contracteer.core.normalize

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
 * Values are [normalized][normalize] on construction.
 */
class ScenarioRequest(parameterValues: Map<ParameterElement, Any?>, val body: ScenarioBody?) {
  val parameterValues: Map<ParameterElement, Any?> = parameterValues.mapValues { it.value?.normalize() }
}

/**
 * The response side of a [Scenario]: header values and an optional body.
 *
 * Values are [normalized][normalize] on construction.
 */
class ScenarioResponse(headers: Map<ParameterElement.Header, Any?>, val body: ScenarioBody?) {
  val headers: Map<ParameterElement.Header, Any?> = headers.mapValues { it.value?.normalize() }
}

/**
 * A body value with its content type, used within a [Scenario].
 *
 * The [value] is [normalized][normalize] on construction.
 */
class ScenarioBody(val contentType: ContentType, value: Any?) {
  val value: Any? = value?.normalize()
}
