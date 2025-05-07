package tech.sabai.contracteer.verifier

import org.http4k.core.Headers
import org.http4k.core.Response
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.accumulate
import tech.sabai.contracteer.core.contract.ContractResponse

internal class ResponseValidator(private val contractResponse: ContractResponse) {

  fun validate(response: Response) =
      validateStatusCode(response.status.code) andThen
          { validateHeaders(response.headers) } andThen
          { validateBody(response) }

  private fun validateStatusCode(statusCode: Int) =
    if (this.contractResponse.statusCode == statusCode) success(statusCode)
    else failure("Status code does not match. Expected: ${this.contractResponse.statusCode}, Actual: $statusCode")


  private fun validateHeaders(headers: Headers) =
    contractResponse.headers.accumulate {
      when {
        !it.isRequired && !headers.hasHeader(it.name) ->
          success()

        it.isRequired && !headers.hasHeader(it.name)  ->
          failure("Response header '${it.name}' is missing")

        else                                          ->
          it.dataType.validate(headers.headerValue(it.name)).forProperty(it.name)
      }
    }

  private fun validateBody(response: Response) =
    when {
      contractResponse.body == null && response.contentType().isNullOrEmpty()  ->
        success()

      contractResponse.body == null && !response.contentType().isNullOrEmpty() ->
        failure("Expected no Content-Type but found: '${response.contentType()}'")

      contractResponse.body != null && response.contentType().isNullOrEmpty()  ->
        failure("Content-Type is missing, expected '${contractResponse.body!!.contentType}'")

      else                                                                     ->
        contractResponse.body!!.contentType
          .validate(response.contentType()!!)
          .andThen {
            contractResponse.body!!.contentType
              .parseValue(response.bodyString(), contractResponse.body!!.dataType)
              .flatMap { contractResponse.body!!.dataType.validate(it) }
          }
    }


  private fun Headers.hasHeader(name: String) = any { it.first.lowercase() == name.lowercase() }
  private fun Headers.headerValue(name: String) = find { it.first.lowercase() == name.lowercase() }?.second
  private fun Response.contentType(): String? = header("Content-Type")
}

