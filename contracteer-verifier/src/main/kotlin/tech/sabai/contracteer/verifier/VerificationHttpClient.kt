package tech.sabai.contracteer.verifier

import org.http4k.client.JavaHttpClient
import org.http4k.core.*
import org.http4k.core.cookie.cookie
import tech.sabai.contracteer.core.operation.*
import tech.sabai.contracteer.core.operation.ContentType
import tech.sabai.contracteer.core.operation.ParameterElement.*
import tech.sabai.contracteer.verifier.VerificationCase.*

internal class VerificationHttpClient(private val serverUrl: String) {
  private val baseClient = JavaHttpClient()

  fun execute(case: VerificationCase): Pair<Request, Response> {
    return when (case) {
      is ScenarioBased -> executeScenario(case)
      is SchemaBased   -> executeSchema(case)
      is TypeMismatch  -> executeTypeMismatch(case)
    }
  }

  private fun executeScenario(case: ScenarioBased): Pair<Request, Response> {
    val scenario = case.scenario
    validateScenarioParameterElements(scenario.request.parameterValues, case.requestSchema)

    val pathParams = buildPathParameters(scenario.request.parameterValues, case.requestSchema)
    val request = Request(
      method = Method.valueOf(scenario.method.uppercase()),
      uri = UriTemplate.from("$serverUrl${scenario.path}").generate(pathParams))
      .withScenarioParameters(scenario.request.parameterValues, case.requestSchema)
      .withScenarioBody(scenario.request.body)
      .withAcceptHeader(scenario.response.body?.contentType)

    return request to baseClient(request)
  }

  private fun executeSchema(case: SchemaBased): Pair<Request, Response> {
    val pathParams = case.requestSchema.pathParameters.associate {
      it.element.name to it.serde.serialize(it.dataType.randomValue())
    }

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
        else                                                                   -> param.serde.serialize(param.dataType.randomValue())
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
      val isMutated = mutatedElement is MutatedElement.Parameter && mutatedElement.element == param.element
      val value = if (isMutated) case.mutatedValue else param.serde.serialize(param.dataType.randomValue())
      when (val element = param.element) {
        is QueryParam -> req.query(element.name, value)
        is Header     -> req.header(element.name, value)
        is Cookie     -> req.cookie(element.name, value)
        is PathParam  -> req // Already handled in URI template
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
    return requestSchema.pathParameters.associate { param ->
      val value = parameterValues.getOrGenerate(param.element) { param.dataType.randomValue() }
      param.element.name to param.serde.serialize(value)
    }
  }

  private fun Request.withScenarioParameters(
    parameterValues: Map<ParameterElement, Any?>,
    requestSchema: RequestSchema
  ): Request {
    return requestSchema.parameters.fold(this) { req, param ->
      val value = parameterValues.getOrGenerate(param.element) { param.dataType.randomValue() }
      when (val element = param.element) {
        is QueryParam -> req.query(element.name, param.serde.serialize(value))
        is Header     -> req.header(element.name, param.serde.serialize(value))
        is Cookie     -> req.cookie(element.name, param.serde.serialize(value))
        is PathParam  -> req // Already handled in URI template
      }
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

  private fun Request.withScenarioBody(body: ScenarioBody?): Request {
    return body?.let { header("Content-Type", it.contentType.value).body(it.contentType.serde.serialize(it.value)) }
           ?: this
  }

  private fun Request.withGeneratedParameters(requestSchema: RequestSchema): Request {
    return requestSchema.parameters.fold(this) { req, param ->
      when (val element = param.element) {
        is QueryParam -> req.query(element.name, param.serde.serialize(param.dataType.randomValue()))
        is Header     -> req.header(element.name, param.serde.serialize(param.dataType.randomValue()))
        is Cookie     -> req.cookie(element.name, param.serde.serialize(param.dataType.randomValue()))
        is PathParam  -> req // Already handled in URI template
      }
    }
  }

  private fun Request.withGeneratedBody(bodySchema: BodySchema?): Request {
    return bodySchema?.let {
      header("Content-Type", it.contentType.value).body(it.contentType.serde.serialize(it.dataType.randomValue()))
    } ?: this
  }

  private fun Request.withAcceptHeader(contentType: ContentType?): Request {
    return contentType?.let { header("Accept", it.value) } ?: this
  }

  private fun <V> Map<ParameterElement, V>.getOrGenerate(key: ParameterElement, generator: () -> V): V {
    return if (containsKey(key)) getValue(key) else generator()
  }
}
