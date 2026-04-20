package tech.sabai.contracteer.core.dsl

import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.operation.ApiOperation
import tech.sabai.contracteer.core.operation.BodySchema
import tech.sabai.contracteer.core.operation.ContentType
import tech.sabai.contracteer.core.operation.ParameterElement
import tech.sabai.contracteer.core.operation.ParameterSchema
import tech.sabai.contracteer.core.operation.RequestSchema
import tech.sabai.contracteer.core.operation.ResponseSchema
import tech.sabai.contracteer.core.operation.ResponseSchemas
import tech.sabai.contracteer.core.operation.Scenario
import tech.sabai.contracteer.core.serde.JsonSerde
import tech.sabai.contracteer.core.serde.PlainTextSerde
import tech.sabai.contracteer.core.serde.Serde

fun apiOperation(
  method: String,
  path: String,
  block: ApiOperationBuilder.() -> Unit = {}
): ApiOperation = ApiOperationBuilder(method, path).apply(block).build()

@TestBuilder
class ApiOperationBuilder internal constructor(val method: String, val path: String) {
  private var requestBuilder: RequestBuilder? = null
  private val responses = mutableMapOf<Int, ResponseSchema>()
  private val scenarios = mutableListOf<Scenario>()

  fun request(block: RequestBuilder.() -> Unit) {
    requestBuilder = RequestBuilder().apply(block)
  }

  fun response(statusCode: Int, block: ResponseBuilder.() -> Unit = {}) {
    responses[statusCode] = ResponseBuilder().apply(block).build()
  }

  fun scenario(key: String, status: Int, block: ScenarioBuilder.() -> Unit = {}) {
    scenarios += ScenarioBuilder(path, method, key, status).apply(block).build()
  }

  internal fun build(): ApiOperation = ApiOperation(
    path = path,
    method = method,
    requestSchema = (requestBuilder ?: RequestBuilder()).build(),
    responseSchemas = ResponseSchemas(byStatusCode = responses.toMap()),
    scenarios = scenarios.toList()
  )
}

@TestBuilder
class RequestBuilder internal constructor() {
  private val parameters = mutableListOf<ParameterSchema>()
  private val bodies = mutableListOf<BodySchema>()

  fun pathParam(name: String, dataType: DataType<out Any>, isRequired: Boolean = true, codec: CodecFactory = simple()) {
    parameters += ParameterSchema(ParameterElement.PathParam(name), dataType, isRequired, codec(name))
  }

  fun queryParam(name: String, dataType: DataType<out Any>, isRequired: Boolean = false, allowReserved: Boolean = false, codec: CodecFactory = form()) {
    parameters += ParameterSchema(ParameterElement.QueryParam(name, allowReserved), dataType, isRequired, codec(name))
  }

  fun header(name: String, dataType: DataType<out Any>, isRequired: Boolean = false, codec: CodecFactory = simple()) {
    parameters += ParameterSchema(ParameterElement.Header(name), dataType, isRequired, codec(name))
  }

  fun cookie(name: String, dataType: DataType<out Any>, isRequired: Boolean = false, codec: CodecFactory = form(explode = false)) {
    parameters += ParameterSchema(ParameterElement.Cookie(name), dataType, isRequired, codec(name))
  }

  fun jsonBody(dataType: DataType<out Any>, isRequired: Boolean = true) {
    bodies += BodySchema(ContentType("application/json"), dataType, isRequired, JsonSerde)
  }

  fun plainTextBody(dataType: DataType<out Any>, isRequired: Boolean = true) {
    bodies += BodySchema(ContentType("text/plain"), dataType, isRequired, PlainTextSerde)
  }

  fun body(contentType: String, dataType: DataType<out Any>, serde: Serde, isRequired: Boolean = true) {
    bodies += BodySchema(ContentType(contentType), dataType, isRequired, serde)
  }

  internal fun build(): RequestSchema = RequestSchema(parameters.toList(), bodies.toList())
}

@TestBuilder
class ResponseBuilder internal constructor() {
  private val headers = mutableListOf<ParameterSchema>()
  private val bodies = mutableListOf<BodySchema>()

  fun header(name: String, dataType: DataType<out Any>, isRequired: Boolean = false, codec: CodecFactory = simple()) {
    headers += ParameterSchema(ParameterElement.Header(name), dataType, isRequired, codec(name))
  }

  fun jsonBody(dataType: DataType<out Any>, isRequired: Boolean = true) {
    bodies += BodySchema(ContentType("application/json"), dataType, isRequired, JsonSerde)
  }

  fun plainTextBody(dataType: DataType<out Any>, isRequired: Boolean = true) {
    bodies += BodySchema(ContentType("text/plain"), dataType, isRequired, PlainTextSerde)
  }

  fun body(contentType: String, dataType: DataType<out Any>, serde: Serde, isRequired: Boolean = true) {
    bodies += BodySchema(ContentType(contentType), dataType, isRequired, serde)
  }

  internal fun build(): ResponseSchema = ResponseSchema(headers.toList(), bodies.toList())
}
