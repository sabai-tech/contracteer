package tech.sabai.contracteer.core.operation

data class Scenario(
  val path: String,
  val method: String,
  val key: String,
  val statusCode: Int,
  val request: ScenarioRequest,
  val response: ScenarioResponse
)

data class ScenarioRequest(
  val parameterValues: Map<ParameterElement, Any?>,
  val body: ScenarioBody?
)

data class ScenarioResponse(
  val parameterValues: Map<ParameterElement, Any?>,
  val body: ScenarioBody?
)

data class ScenarioBody(
  val contentType: ContentType,
  val value: Any?
)
