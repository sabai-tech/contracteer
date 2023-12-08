package dev.blitzcraft.contracts.verifier

import com.jayway.jsonpath.JsonPath
import dev.blitzcraft.contracts.core.JsonPathMatcher
import dev.blitzcraft.contracts.core.ResponseContract
import io.restassured.http.Headers
import io.restassured.response.Response

internal class ResponseAsserter(private val responseContract: ResponseContract) {
  fun assert(response: Response) {
    validateStatusCode(response.statusCode)
    validateHeaders(response.headers)
    validateContentType(response.contentType)
    if (responseContract.body != null) {
      validateBody(response)
    }
  }

  private fun validateHeaders(headers: Headers) {
    responseContract.headers.forEach { (name, property) ->
      if (headers.hasHeaderWithName(name)) {
        require(property.dataType.regexPattern().toPattern().asMatchPredicate().test(headers[name]!!.value))
        { "Assertion failed on Header '$name'. It does not match ${property.dataType.regexPattern()}" }
      } else {
        require(property.required.not()) { "Assertion failed. Header '$name' is missing" }
      }
    }
  }

  private fun validateStatusCode(statusCode: Int) {
    require(statusCode == responseContract.statusCode) { "Assertion Failed on Response status code. Expected: ${responseContract.statusCode}, Actual: $statusCode" }
  }

  private fun validateContentType(contentType: String?) {
    if (responseContract.body == null) require(contentType.isNullOrEmpty()) { "Assertion Failed. Expected no Content-Type but found: '$contentType'" }
    if (responseContract.body != null) require(contentType != null) { "Assertion Failed. Content-Type is missing, expected '${responseContract.body!!.contentType}'" }
    if (!contentType.isNullOrEmpty()) require(contentType.matches(Regex("${responseContract.body!!.contentType}.*"))) { "Assertion Failed on Response Content-Type. Expected: ${responseContract.body!!.contentType}, Actual: '$contentType" }
  }

  private fun validateBody(response: Response) {
    response.contentType?.let { contentType ->
      if ("json" !in contentType) throw IllegalArgumentException("Content '${response.contentType}' is not managed")

      JsonPathMatcher.regexMatchers(responseContract.body!!.dataType).forEach {
        require((JsonPath.read(response.body.asString(), it) as Collection<*>).isNotEmpty()) { "$it does not match" }
      }
    }
  }
}