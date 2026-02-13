package tech.sabai.contracteer.verifier

import io.github.oshai.kotlinlogging.KotlinLogging
import org.http4k.client.JavaHttpClient
import org.http4k.core.*
import org.http4k.core.cookie.cookie
import org.http4k.filter.DebuggingFilters
import tech.sabai.contracteer.core.operation.ContentType
import tech.sabai.contracteer.core.operation.BodySchema
import tech.sabai.contracteer.core.operation.ParameterElement
import tech.sabai.contracteer.core.operation.ParameterElement.*
import tech.sabai.contracteer.core.operation.RequestSchema
import tech.sabai.contracteer.core.operation.ScenarioBody
import tech.sabai.contracteer.verifier.VerificationCase.ScenarioBased
import tech.sabai.contracteer.verifier.VerificationCase.SchemaBased
import tech.sabai.contracteer.verifier.VerificationCase.TypeMismatchCase

internal class VerificationHttpClient(private val serverUrl: String) {
  private val logger = KotlinLogging.logger {}
  private val baseClient = JavaHttpClient()

  fun sendRequest(case: VerificationCase): Response {
    val client = createClient()

    return when (case) {
      is ScenarioBased    -> sendScenarioRequest(client, case)
      is SchemaBased      -> sendSchemaRequest(client, case)
      is TypeMismatchCase -> sendTypeMismatchRequest(client, case)
    }
  }

  private fun createClient() =
    if (logger.isDebugEnabled())
      DebuggingFilters.PrintRequestAndResponse().then(baseClient)
    else
      baseClient

  private fun sendScenarioRequest(client: (Request) -> Response, case: ScenarioBased): Response {
    val scenario = case.scenario
    validateScenarioParameterElements(scenario.request.parameterValues, case.requestSchema)

    val pathParams = buildPathParameters(scenario.request.parameterValues, case.requestSchema)
    val request = Request(
      method = Method.valueOf(scenario.method.uppercase()),
      uri = UriTemplate.from("$serverUrl${scenario.path}").generate(pathParams))
      .withScenarioParameters(scenario.request.parameterValues, case.requestSchema)
      .withScenarioBody(scenario.request.body)
      .withAcceptHeader(scenario.response.body?.contentType)

    return client(request)
  }

  private fun sendSchemaRequest(client: (Request) -> Response, case: SchemaBased): Response {
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

    return client(request)
  }

  private fun sendTypeMismatchRequest(client: (Request) -> Response, case: TypeMismatchCase): Response {
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

    return client(request)
  }

  private fun Request.withTypeMismatchParameters(case: TypeMismatchCase): Request {
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

  private fun Request.withTypeMismatchBody(case: TypeMismatchCase): Request {
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
