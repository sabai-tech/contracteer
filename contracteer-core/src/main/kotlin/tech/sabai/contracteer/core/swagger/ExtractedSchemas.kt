package tech.sabai.contracteer.core.swagger

import tech.sabai.contracteer.core.operation.*
import tech.sabai.contracteer.core.operation.ParameterElement.Header

internal class ExtractedParameterSchema(val schema: ParameterSchema,
                                        val examples: Map<String, Any?>)

internal class ExtractedBodySchema(val schema: BodySchema,
                                   val examples: Map<String, Any?>)

internal class ExtractedRequestSchema(val parameters: List<ExtractedParameterSchema>,
                                      val bodies: List<ExtractedBodySchema>) {

  fun toRequestSchema() = RequestSchema(parameters.map { it.schema }, bodies.map { it.schema })

  fun exampleKeys(): Set<String> =
    parameters.flatMap { it.examples.keys }.toSet() +
    bodies.flatMap { it.examples.keys }.toSet()

  fun parameterExamplesFor(key: String): Map<ParameterElement, Any?> =
    parameters
      .filter { key in it.examples }
      .associate { it.schema.element to it.examples[key] }

  fun bodyExamplesFor(key: String): List<ScenarioBody> =
    bodies
      .filter { key in it.examples }
      .map { ScenarioBody(it.schema.contentType, it.examples[key]) }
}

internal class ExtractedResponseSchema(
  val headers: List<ExtractedParameterSchema>,
  val bodies: List<ExtractedBodySchema>
) {
  fun toResponseSchema() = ResponseSchema(headers.map { it.schema }, bodies.map { it.schema })

  fun exampleKeys(): Set<String> =
    headers.flatMap { it.examples.keys }.toSet() +
    bodies.flatMap { it.examples.keys }.toSet()

  fun headerExamplesFor(key: String): Map<Header, Any?> =
    headers
      .filter { key in it.examples }
      .associate {
        val element = it.schema.element
        check(element is Header) { "Response header must have Header element type, got: $element" }
        element to it.examples[key]
      }

  fun bodyExamplesFor(key: String): List<ScenarioBody> =
    bodies
      .filter { key in it.examples }
      .map { ScenarioBody(it.schema.contentType, it.examples[key]) }
}
