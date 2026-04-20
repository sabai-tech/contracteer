package tech.sabai.contracteer.core.dsl

import tech.sabai.contracteer.core.operation.ContentType
import tech.sabai.contracteer.core.operation.ParameterElement
import tech.sabai.contracteer.core.operation.Scenario
import tech.sabai.contracteer.core.operation.ScenarioBody
import tech.sabai.contracteer.core.operation.ScenarioRequest
import tech.sabai.contracteer.core.operation.ScenarioResponse

@TestBuilder
class ScenarioBuilder internal constructor(
  private val path: String,
  private val method: String,
  private val key: String,
  private val status: Int
) {
  private var request: ScenarioRequest = ScenarioRequest(emptyMap(), null)
  private var response: ScenarioResponse = ScenarioResponse(emptyMap(), null)

  fun request(block: ScenarioRequestBuilder.() -> Unit = {}) {
    request = ScenarioRequestBuilder().apply(block).build()
  }

  fun response(block: ScenarioResponseBuilder.() -> Unit = {}) {
    response = ScenarioResponseBuilder().apply(block).build()
  }

  internal fun build(): Scenario = Scenario(path, method, key, status, request, response)
}

@TestBuilder
class ScenarioRequestBuilder internal constructor() {
  val pathParam = ParamValues(ParameterElement::PathParam)
  val queryParam = ParamValues { ParameterElement.QueryParam(it) }
  val header = ParamValues(ParameterElement::Header)
  val cookie = ParamValues(ParameterElement::Cookie)
  private var body: ScenarioBody? = null

  fun jsonBody(value: Any?) {
    body = ScenarioBody(ContentType("application/json"), value)
  }

  fun jsonBody(block: JsonObjectBuilder.() -> Unit) {
    jsonBody(json(block))
  }

  fun plainTextBody(value: Any?) {
    body = ScenarioBody(ContentType("text/plain"), value)
  }

  fun body(contentType: String, value: Any?) {
    body = ScenarioBody(ContentType(contentType), value)
  }

  internal fun build(): ScenarioRequest {
    val merged = pathParam.entries + queryParam.entries + header.entries + cookie.entries
    return ScenarioRequest(merged, body)
  }
}

@TestBuilder
class ScenarioResponseBuilder internal constructor() {
  val header = HeaderValues()
  private var body: ScenarioBody? = null

  fun jsonBody(value: Any?) {
    body = ScenarioBody(ContentType("application/json"), value)
  }

  fun jsonBody(block: JsonObjectBuilder.() -> Unit) {
    jsonBody(json(block))
  }

  fun plainTextBody(value: Any?) {
    body = ScenarioBody(ContentType("text/plain"), value)
  }

  fun body(contentType: String, value: Any?) {
    body = ScenarioBody(ContentType(contentType), value)
  }

  internal fun build(): ScenarioResponse = ScenarioResponse(header.entries, body)
}

class ParamValues internal constructor(private val elementOf: (String) -> ParameterElement) {
  internal val entries = mutableMapOf<ParameterElement, Any?>()
  operator fun set(name: String, value: Any?) {
    entries[elementOf(name)] = value
  }
}

class HeaderValues internal constructor() {
  internal val entries = mutableMapOf<ParameterElement.Header, Any?>()
  operator fun set(name: String, value: Any?) {
    entries[ParameterElement.Header(name)] = value
  }
}

class JsonObjectBuilder internal constructor() {
  private val entries = mutableMapOf<String, Any?>()
  infix fun String.to(value: Any?) {
    entries[this] = value
  }

  internal fun build(): Map<String, Any?> = entries.toMap()
}

fun json(block: JsonObjectBuilder.() -> Unit): Map<String, Any?> =
  JsonObjectBuilder().apply(block).build()
