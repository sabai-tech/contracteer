package dev.blitzcraft.contracts.mockserver

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import dev.blitzcraft.contracts.core.Contract
import dev.blitzcraft.contracts.core.JsonPathMatcher
import dev.blitzcraft.contracts.core.Property
import dev.blitzcraft.contracts.core.ResponseContract


class MockServer(port: Int = 8080, private val contracts: Set<Contract>) {

  private var wireMockServer: WireMockServer

  init {
    wireMockServer = WireMockServer(options().port(port).notifier(ConsoleNotifier(true)))
  }

  fun start() {
    wireMockServer.start()
    println("Mock Server started at port: ${wireMockServer.port()}")
    addStubs(contracts)
  }

  private fun addStubs(contracts: Set<Contract>) {
    contracts.forEach {
      wireMockServer.stubFor(it.asMappingBuilder().willReturn(it.response.asResponseDefinitionBuilder()))
    }
  }

  fun stop() {
    wireMockServer.stop()
  }

  private fun ResponseContract.asResponseDefinitionBuilder(): ResponseDefinitionBuilder {
    val response = aResponse().withStatus(statusCode)
    headers.forEach { response.withHeader(it.key, it.value.value().toString()) }
    body?.let {
      response.withHeader("Content-Type", it.contentType)
      response.withBody(it.asString())
    }
    return response
  }

  private fun Contract.asMappingBuilder(): MappingBuilder {
    val mappingBuilder = when (request.method.uppercase()) {
      "GET"     -> get(urlPathTemplate(request.path))
      "POST"    -> post(urlPathTemplate(request.path))
      "PUT"     -> put(urlPathTemplate(request.path))
      "PATCH"   -> patch(urlPathTemplate(request.path))
      "DELETE"  -> delete(urlPathTemplate(request.path))
      "HEAD"    -> head(urlPathTemplate(request.path))
      "OPTIONS" -> options(urlPathTemplate(request.path))
      "TRACE"   -> trace(urlPathTemplate(request.path))
      else      -> throw IllegalArgumentException("Invalid HTTP method: ${request.method}")
    }
    response.body?.let { mappingBuilder.withHeader("Accept", matching("${it.contentType}.*")) }
    request.body?.let { body ->
      mappingBuilder.withHeader("Content-Type", matching("${body.contentType}.*"))
      val bodyMatchers = body.example?.let { JsonPathMatcher.exampleMatchers(it.value) }
                         ?: JsonPathMatcher.regexMatchers(body.dataType)
      bodyMatchers.forEach { mappingBuilder.withRequestBody(matchingJsonPath(it)) }
    }
    request.pathParameters.forEach { mappingBuilder.withPathParam(it.key, it.value.asStringValuePattern()) }
    request.queryParameters.forEach { mappingBuilder.withQueryParam(it.key, it.value.asStringValuePattern()) }
    request.cookies.forEach { mappingBuilder.withCookie(it.key, it.value.asStringValuePattern()) }
    return mappingBuilder
  }

  private fun Property.asStringValuePattern() =
    if (example != null) {
      equalTo(example!!.value.toString())
    } else {
      if (required) matching(dataType.regexPattern())
      else matching("(${dataType.regexPattern()})?")
    }
}
