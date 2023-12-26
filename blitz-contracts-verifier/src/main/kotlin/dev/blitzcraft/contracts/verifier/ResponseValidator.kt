package dev.blitzcraft.contracts.verifier

import com.fasterxml.jackson.databind.ObjectMapper
import dev.blitzcraft.contracts.core.*
import org.http4k.core.Headers
import org.http4k.core.Response
import org.http4k.core.findSingle

internal class ResponseValidator(private val responseContract: ResponseContract) {
  private val objectMapper = ObjectMapper()

  fun validate(response: Response) =
    CompositeValidationResult(listOf(validateStatusCode(response.status.code),
                                     validateHeaders(response.headers),
                                     validateBody(response)))

  private fun validateHeaders(headers: Headers) =
    CompositeValidationResult(responseContract.headers.map {
      if (it.required && headers.hasHeader(it.name).not()) SimpleValidationResult(it.name, "Missing property")
      else it.parseAndValidate(headers.findSingle(it.name))
    })

  private fun validateStatusCode(statusCode: Int) =
    if (statusCode == responseContract.statusCode) SimpleValidationResult()
    else SimpleValidationResult("Status code does not match. Expected: ${responseContract.statusCode}, Actual: $statusCode")

  private fun validateBody(response: Response): ValidationResult {
    val contentType = response.header("Content-Type")

    return when {
      responseContract.body == null && contentType.isNullOrEmpty()              -> SimpleValidationResult()
      responseContract.body == null && !contentType.isNullOrEmpty()             -> SimpleValidationResult("Expected no Content-Type but found: '$contentType'")
      responseContract.body != null && contentType.isNullOrEmpty()              -> SimpleValidationResult("Content-Type is missing, expected '${responseContract.body!!.contentType}'")
      !contentType!!.matches(Regex("${responseContract.body!!.contentType}.*")) -> SimpleValidationResult("Wrong Content-Type. Expected: ${responseContract.body!!.contentType}, Actual: '$contentType")
      "json" !in contentType.lowercase()                                        -> SimpleValidationResult("Content-Type'$contentType' is not managed")
      else                                                                      ->
        responseContract.body!!.dataType.validateValue(objectMapper.readTree(response.bodyString()))
    }
  }
}

private fun Headers.hasHeader(name: String) = any { it.first == name }

