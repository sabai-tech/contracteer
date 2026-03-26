package tech.sabai.contracteer.mockserver

import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.datatype.IntegerDataType
import tech.sabai.contracteer.core.datatype.ObjectDataType
import tech.sabai.contracteer.core.datatype.StringDataType
import tech.sabai.contracteer.core.operation.*
import tech.sabai.contracteer.core.codec.SimpleStyleCodec
import tech.sabai.contracteer.core.serde.JsonSerde
import tech.sabai.contracteer.core.serde.PlainTextSerde
import java.math.BigDecimal

object TestFixture {

  fun integerDataType(isNullable: Boolean = false, enum: List<BigDecimal?> = emptyList()) =
    IntegerDataType.create("integer", isNullable, enum).value!!

  fun stringDataType(
    isNullable: Boolean = false,
    enum: List<String?> = emptyList(),
    minLength: Int? = null,
    maxLength: Int? = null
  ) = StringDataType.create("string", "string", isNullable, enum, minLength, maxLength).value!!

  fun objectDataType(
    properties: Map<String, DataType<out Any>>,
    requiredProperties: Set<String> = emptySet(),
    allowAdditionalProperties: Boolean = true,
    additionalPropertiesDataType: DataType<out Any>? = null,
    isNullable: Boolean = false,
    enum: List<Any?> = emptyList()
  ) = ObjectDataType.create(
    name = "object",
    properties = properties,
    requiredProperties = requiredProperties,
    allowAdditionalProperties = allowAdditionalProperties,
    additionalPropertiesDataType = additionalPropertiesDataType,
    isNullable = isNullable,
    enum = enum
  ).value!!

  fun parameterSchema(
    element: ParameterElement,
    dataType: DataType<out Any>,
    isRequired: Boolean = true
  ) = ParameterSchema(element, dataType, isRequired, SimpleStyleCodec(element.name, false))

  fun bodySchema(
    contentType: ContentType = ContentType("application/json"),
    dataType: DataType<out Any>,
    isRequired: Boolean = false
  ) = BodySchema(contentType, dataType, isRequired, if (contentType.isJson()) JsonSerde else PlainTextSerde)

  fun requestSchema(
    parameters: List<ParameterSchema> = emptyList(),
    bodies: List<BodySchema> = emptyList()
  ) = RequestSchema(parameters, bodies)

  fun responseSchema(
    headers: List<ParameterSchema> = emptyList(),
    bodies: List<BodySchema> = emptyList()
  ) = ResponseSchema(headers, bodies)

  fun scenario(
    path: String,
    method: String,
    key: String,
    statusCode: Int,
    requestParameterValues: Map<ParameterElement, Any?> = emptyMap(),
    requestBody: ScenarioBody? = null,
    responseHeaders: Map<ParameterElement.Header, Any?> = emptyMap(),
    responseBody: ScenarioBody? = null
  ) = Scenario(
    path = path,
    method = method,
    key = key,
    statusCode = statusCode,
    request = ScenarioRequest(requestParameterValues, requestBody),
    response = ScenarioResponse(responseHeaders, responseBody)
  )

  fun apiOperation(
    path: String,
    method: String,
    requestSchema: RequestSchema = requestSchema(),
    responses: Map<Int, ResponseSchema> = emptyMap(),
    scenarios: List<Scenario> = emptyList()
  ) = ApiOperation(path = path, method = method, requestSchema = requestSchema, responses = responses, scenarios = scenarios)
}
