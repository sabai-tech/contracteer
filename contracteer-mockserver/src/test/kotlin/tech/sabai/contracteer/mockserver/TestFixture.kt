package tech.sabai.contracteer.mockserver

import tech.sabai.contracteer.core.codec.SimpleParameterCodec
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.operation.*
import tech.sabai.contracteer.core.serde.JsonSerde
import tech.sabai.contracteer.core.serde.PlainTextSerde

object TestFixture {

  fun parameterSchema(element: ParameterElement, dataType: DataType<out Any>, isRequired: Boolean = true) =
    ParameterSchema(element, dataType, isRequired, SimpleParameterCodec(element.name, false))

  fun bodySchema(contentType: ContentType, dataType: DataType<out Any>, isRequired: Boolean = false) =
    BodySchema(contentType, dataType, isRequired, if (contentType.isJson()) JsonSerde else PlainTextSerde)

  fun requestSchema(parameters: List<ParameterSchema> = emptyList(), bodies: List<BodySchema> = emptyList()) =
    RequestSchema(parameters, bodies)

  fun responseSchema(headers: List<ParameterSchema> = emptyList(), bodies: List<BodySchema> = emptyList()) =
    ResponseSchema(headers, bodies)

  fun scenario(path: String,
               method: String,
               key: String,
               statusCode: Int,
               requestParameterValues: Map<ParameterElement, Any?> = emptyMap(),
               requestBody: ScenarioBody? = null,
               responseHeaders: Map<ParameterElement.Header, Any?> = emptyMap(),
               responseBody: ScenarioBody? = null) =
    Scenario(
      path = path,
      method = method,
      key = key,
      statusCode = statusCode,
      request = ScenarioRequest(requestParameterValues, requestBody),
      response = ScenarioResponse(responseHeaders, responseBody)
    )

  fun apiOperation(path: String,
                   method: String,
                   requestSchema: RequestSchema = requestSchema(),
                   responses: Map<Int, ResponseSchema> = emptyMap(),
                   scenarios: List<Scenario> = emptyList()) =
    ApiOperation(path = path,
                 method = method,
                 requestSchema = requestSchema,
                 responseSchemas = ResponseSchemas(byStatusCode = responses, byClass = emptyMap(), defaultResponse = null),
                 scenarios = scenarios)
}
