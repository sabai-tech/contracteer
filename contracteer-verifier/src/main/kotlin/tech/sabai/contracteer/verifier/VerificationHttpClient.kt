package tech.sabai.contracteer.verifier

import org.http4k.client.JavaHttpClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.UriTemplate
import org.http4k.core.cookie.cookie
import tech.sabai.contracteer.core.operation.*
import tech.sabai.contracteer.core.serde.Serde
import tech.sabai.contracteer.core.operation.ParameterElement.*
import tech.sabai.contracteer.verifier.VerificationCase.*

internal class VerificationHttpClient(private val serverUrl: String) {
  private val baseClient = JavaHttpClient()

  fun execute(case: VerificationCase): Pair<Request, Response> {
    return when (case) {
      is ScenarioBased -> executeScenario(case)
      is SchemaBased   -> executeSchemaBased(case)
      is TypeMismatch  -> executeTypeMismatch(case)
    }
  }

  private fun executeScenario(case: ScenarioBased): Pair<Request, Response> {
    val scenario = case.scenario
    validateScenarioParameterElements(scenario.request.parameterValues, case.requestSchema)

    val pathParams = buildPathParameters(scenario.request.parameterValues, case.requestSchema)
    val bodySerde = scenario.request.body?.let { body -> case.requestSchema.bodies.serdeFor(body.contentType) }
    val request = Request(
      method = Method.valueOf(scenario.method.uppercase()),
      uri = UriTemplate.from("$serverUrl${scenario.path}").generate(pathParams))
      .withScenarioParameters(scenario.request.parameterValues, case.requestSchema)
      .withScenarioBody(scenario.request.body, bodySerde)
      .withAcceptHeader(scenario.response.body?.contentType)

    return request to baseClient(request)
  }

  private fun executeSchemaBased(case: SchemaBased): Pair<Request, Response> {
    val pathParams = case.requestSchema.pathParameters
      .flatMap { it.codec.encode(it.dataType.randomValue()) }
      .toMap()

    val request = Request(
      method = Method.valueOf(case.method.uppercase()),
      uri = UriTemplate.from("$serverUrl${case.path}").generate(pathParams))
      .withGeneratedParameters(case.requestSchema)
      .withGeneratedBody(
        case.requestSchema.bodies.find { it.contentType == case.requestContentType })
      .withAcceptHeader(case.responseContentType)

    return request to baseClient(request)
  }

  private fun executeTypeMismatch(case: TypeMismatch): Pair<Request, Response> {
    val mutatedElement = case.mutatedElement

    val pathParams = case.requestSchema.pathParameters.associate { param ->
      val value = when (mutatedElement) {
        is MutatedElement.Parameter if mutatedElement.element == param.element -> case.mutatedValue
        else                                                                   ->
          param.codec
            .encode(param.dataType.randomValue())
            .single().second
      }
      param.element.name to value
    }

    val request = Request(
      method = Method.valueOf(case.method.uppercase()),
      uri = UriTemplate.from("$serverUrl${case.path}").generate(pathParams))
      .withTypeMismatchParameters(case)
      .withTypeMismatchBody(case)
      .withAcceptHeader(case.responseContentType)

    return request to baseClient(request)
  }

  private fun Request.withTypeMismatchParameters(case: TypeMismatch): Request {
    val mutatedElement = case.mutatedElement
    return case.requestSchema.parameters.fold(this) { req, param ->
      when {
        param.element is PathParam                                                            -> req
        mutatedElement is MutatedElement.Parameter && mutatedElement.element == param.element ->
          req.placeRawValue(param.element, case.mutatedValue)
        else                                                                                  ->
          req.placeEncodedEntries(param.element, param.codec.encode(param.dataType.randomValue()))
      }
    }
  }

  private fun Request.withTypeMismatchBody(case: TypeMismatch): Request {
    return if (case.mutatedElement is MutatedElement.Body) {
      case.requestContentType?.let { header("Content-Type", it.value).body(case.mutatedValue) } ?: this
    } else {
      withGeneratedBody(case.requestSchema.bodies.find { it.contentType == case.requestContentType })
    }
  }

  private fun buildPathParameters(
    parameterValues: Map<ParameterElement, Any?>,
    requestSchema: RequestSchema
  ): Map<String, String> {
    return requestSchema.pathParameters.flatMap { param ->
      val value = parameterValues.getOrGenerate(param.element) { param.dataType.randomValue() }
      param.codec.encode(value)
    }.toMap()
  }

  private fun Request.withScenarioParameters(
    parameterValues: Map<ParameterElement, Any?>,
    requestSchema: RequestSchema
  ): Request = withParameters(requestSchema) { parameterValues.getOrGenerate(it.element) { it.dataType.randomValue() } }

  private fun Request.withGeneratedParameters(requestSchema: RequestSchema): Request =
    withParameters(requestSchema) { it.dataType.randomValue() }

  private fun Request.withParameters(
    requestSchema: RequestSchema,
    valueProvider: (ParameterSchema) -> Any?
  ): Request = requestSchema.parameters.fold(this) { req, param ->
    when (param.element) {
      is PathParam -> req
      else         -> req.placeEncodedEntries(param.element, param.codec.encode(valueProvider(param)))
    }
  }

  private fun validateScenarioParameterElements(
    parameterValues: Map<ParameterElement, Any?>,
    requestSchema: RequestSchema
  ) {
    val allowedElements = requestSchema.parameters.map { it.element }.toSet()
    val unknownElements = parameterValues.keys.filterNot { it in allowedElements }
    if (unknownElements.isNotEmpty()) {
      error("No parameter schema found for elements: ${unknownElements.joinToString(", ")}")
    }
  }

  private fun Request.withScenarioBody(body: ScenarioBody?, serde: Serde?): Request {
    if (body == null || serde == null) return this
    return header("Content-Type", body.contentType.value).body(serde.serialize(body.value))
  }

  private fun Request.placeEncodedEntries(element: ParameterElement, entries: List<Pair<String, String>>): Request =
    entries.fold(this) { request, (key, value) ->
      when (element) {
        is QueryParam -> request.query(key, value)
        is Header     -> request.header(key, value)
        is Cookie     -> request.cookie(key, value)
        else          -> request
      }
    }

  private fun Request.placeRawValue(element: ParameterElement, value: String): Request =
    when (element) {
      is QueryParam -> query(element.name, value)
      is Header     -> header(element.name, value)
      is Cookie     -> cookie(element.name, value)
      else          -> this
    }

  private fun Request.withGeneratedBody(bodySchema: BodySchema?): Request {
    return bodySchema?.let {
      header("Content-Type", it.contentType.value).body(it.serde.serialize(it.dataType.randomValue()))
    } ?: this
  }

  private fun Request.withAcceptHeader(contentType: ContentType?): Request {
    return contentType?.let { header("Accept", it.value) } ?: this
  }

  private fun <V> Map<ParameterElement, V>.getOrGenerate(key: ParameterElement, generator: () -> V): V {
    return if (containsKey(key)) getValue(key) else generator()
  }
}

private fun List<BodySchema>.serdeFor(contentType: ContentType): Serde? =
  find { it.contentType == contentType }?.serde
