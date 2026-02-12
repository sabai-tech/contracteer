package tech.sabai.contracteer.mockserver

import org.http4k.core.Response
import org.http4k.core.Status
import tech.sabai.contracteer.core.operation.BodySchema
import tech.sabai.contracteer.core.operation.Scenario

internal object ResponseGenerator {

  fun fromScenario(scenario: Scenario): Response {
    val response = Response(Status.fromCode(scenario.statusCode)!!)
    val scenarioBody = scenario.response.body ?: return response

    val serializedBody = scenarioBody.contentType.serde.serialize(scenarioBody.value)

    return response
      .header("Content-Type", scenarioBody.contentType.value)
      .body(serializedBody)
  }

  fun fromSchema(statusCode: Int, bodySchema: BodySchema?): Response {
    val response = Response(Status.fromCode(statusCode)!!)
    if (bodySchema == null) return response

    val randomValue = bodySchema.dataType.randomValue()
    val serializedBody = bodySchema.contentType.serde.serialize(randomValue)

    return response
      .header("Content-Type", bodySchema.contentType.value)
      .body(serializedBody)
  }
}
