package tech.sabai.contracteer.mockserver

import org.http4k.core.Request
import tech.sabai.contracteer.core.Result.Success
import tech.sabai.contracteer.core.operation.ParameterElement
import tech.sabai.contracteer.core.operation.RequestSchema
import tech.sabai.contracteer.core.operation.Scenario
import tech.sabai.contracteer.core.operation.ScenarioBody
import tech.sabai.contracteer.mockserver.ScenarioMatchResult.*

internal object ScenarioMatcher {

  fun match(request: Request, scenarios: List<Scenario>, requestSchema: RequestSchema): ScenarioMatchResult {
    val matches = scenarios.filter { it.matches(request, requestSchema) }
    val disambiguated = if (matches.size > 1) disambiguateByAcceptHeader(matches, request.header("Accept")) else matches
    return when {
      disambiguated.size == 1 -> SingleMatch(disambiguated.first())
      disambiguated.size > 1  -> Ambiguous(disambiguated)
      else                    -> NoMatch
    }
  }

  private fun disambiguateByAcceptHeader(matches: List<Scenario>, acceptHeader: String?): List<Scenario> {
    val accept = AcceptHeader.parse(acceptHeader)
    return when {
      accept.acceptsAny() -> matches
      else                -> matches.filter { accept.matchesResponseContentType(it) }.ifEmpty { matches }
    }
  }

  private fun AcceptHeader.matchesResponseContentType(scenario: Scenario): Boolean {
    val responseContentType = scenario.response.body?.contentType ?: return true
    return bestMatch(listOf(responseContentType)) != null
  }

  private fun Scenario.matches(request: Request, requestSchema: RequestSchema): Boolean =
    matchesParameters(request, this.request.parameterValues, requestSchema) &&
    matchesBody(request, this.request.body, requestSchema)

  private fun matchesParameters(
    request: Request,
    parameterValues: Map<ParameterElement, Any?>,
    requestSchema: RequestSchema
  ): Boolean =
    parameterValues.all { (element, expectedValue) ->
      requestHasExpectedValue(request,
                              element,
                              expectedValue,
                              requestSchema)
    }

  private fun requestHasExpectedValue(
    request: Request,
    element: ParameterElement,
    expectedValue: Any?,
    requestSchema: RequestSchema
  ): Boolean {
    val paramSchema = requestSchema.parameters.find { it.element == element } ?: return false
    val valueExtractor = request.valueExtractorFor(element)
    val result = paramSchema.codec.decode(valueExtractor, paramSchema.dataType)
    return result is Success && result.value == expectedValue
  }

  private fun matchesBody(request: Request, scenarioBody: ScenarioBody?, requestSchema: RequestSchema): Boolean {
    if (scenarioBody == null) return true

    val requestContentType = request.header("Content-Type") ?: return false
    if (scenarioBody.contentType.validate(requestContentType).isFailure()) return false

    val bodySchema = requestSchema.bodies.find { it.contentType == scenarioBody.contentType } ?: return false
    val deserializeResult = bodySchema.serde.deserialize(request.bodyString(), bodySchema.dataType)

    return deserializeResult is Success && deserializeResult.value == scenarioBody.value
  }
}
