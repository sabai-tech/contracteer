package tech.sabai.contracteer.mockserver

import org.http4k.core.Response
import org.http4k.core.Status
import tech.sabai.contracteer.core.operation.*

internal object ResponseGenerator {

  fun fromScenario(scenario: Scenario, responseSchema: ResponseSchema): Response {
    val response = Response(Status.fromCode(scenario.statusCode)!!)
      .withHeaders(responseSchema.headers, scenario.response.headers)

    val scenarioBody = scenario.response.body ?: return response
    val bodySchema = responseSchema.bodies.find { it.contentType == scenarioBody.contentType } ?: return response
    val serializedBody = bodySchema.serde.serialize(scenarioBody.value)

    return response
      .header("Content-Type", scenarioBody.contentType.value)
      .body(serializedBody)
  }

  fun fromSchema(statusCode: Int, headers: List<ParameterSchema>, bodySchema: BodySchema?): Response {
    val response = Response(Status.fromCode(statusCode)!!)
      .withHeaders(headers, emptyMap())

    if (bodySchema == null) return response

    val randomValue = bodySchema.dataType.randomValue()
    val serializedBody = bodySchema.serde.serialize(randomValue)

    return response
      .header("Content-Type", bodySchema.contentType.value)
      .body(serializedBody)
  }

  private fun Response.withHeaders(headerSchemas: List<ParameterSchema>,
                                   scenarioHeaders: Map<ParameterElement.Header, Any?>) =
    headerSchemas.fold(this) { response, schema ->
      val header = schema.element as ParameterElement.Header
      val value = scenarioHeaders[header] ?: schema.dataType.randomValue()
      schema.codec.encode(value).fold(response) { resp, (_, headerValue) -> resp.header(header.name, headerValue) }
    }
}
