package tech.sabai.contracteer.mockserver

import org.http4k.core.Request
import org.http4k.core.cookie.cookie
import org.http4k.routing.path
import tech.sabai.contracteer.core.operation.*
import tech.sabai.contracteer.core.operation.ParameterElement.*

internal object ScenarioMatcher {

  fun match(request: Request, scenarios: List<Scenario>, requestSchema: RequestSchema): ScenarioMatchResult {
    val matches = scenarios.filter { it.matches(request, requestSchema) }
    return when {
      matches.size == 1 -> ScenarioMatchResult.SingleMatch(matches.first())
      matches.size > 1  -> ScenarioMatchResult.Ambiguous(matches)
      else              -> ScenarioMatchResult.NoMatch
    }
  }

  private fun Scenario.matches(request: Request, requestSchema: RequestSchema): Boolean =
    matchesParameters(request, this.request.parameterValues, requestSchema) &&
    matchesBody(request, this.request.body, requestSchema)

  private fun matchesParameters(
    request: Request,
    parameterValues: Map<ParameterElement, Any?>,
    requestSchema: RequestSchema
  ): Boolean =
    parameterValues.all { (element, expectedValue) -> requestHasExpectedValue(request, element, expectedValue, requestSchema) }

  private fun requestHasExpectedValue(
    request: Request,
    element: ParameterElement,
    expectedValue: Any?,
    requestSchema: RequestSchema
  ): Boolean {
    val rawValue = extractRawValue(request, element) ?: return false
    val paramSchema = requestSchema.parameters.find { it.element == element } ?: return false
    val deserializeResult = paramSchema.serde.deserialize(rawValue, paramSchema.dataType)
    return deserializeResult.isSuccess() && deserializeResult.value == expectedValue
  }

  private fun matchesBody(request: Request, scenarioBody: ScenarioBody?, requestSchema: RequestSchema): Boolean {
    if (scenarioBody == null) return true

    val requestContentType = request.header("Content-Type") ?: return false
    if (scenarioBody.contentType.validate(requestContentType).isFailure()) return false

    val bodySchema = requestSchema.bodies.find { it.contentType == scenarioBody.contentType } ?: return false
    val deserializeResult = bodySchema.contentType.serde.deserialize(request.bodyString(), bodySchema.dataType)

    return deserializeResult.isSuccess() && deserializeResult.value == scenarioBody.value
  }

  private fun extractRawValue(request: Request, element: ParameterElement): String? =
    when (element) {
      is PathParam  -> request.path(element.name)
      is QueryParam -> request.query(element.name)
      is Header     -> request.header(element.name)
      is Cookie     -> request.cookie(element.name)?.value
    }
}
