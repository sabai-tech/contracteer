package tech.sabai.contracteer.verifier

import org.http4k.core.Headers
import org.http4k.core.Response
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.accumulate
import tech.sabai.contracteer.core.contract.ContractResponse
import tech.sabai.contracteer.core.contract.matches

internal class ResponseValidator(private val responseContract: ContractResponse) {

  fun validate(response: Response) =
    response.status.code.validate() andThen
        { response.headers.validate() } andThen
        { response.validateBody() }

  private fun Int.validate(): Result<Any?> =
    if (this == responseContract.statusCode) success(this)
    else failure("Status code does not match. Expected: ${responseContract.statusCode}, Actual: $this")

  private fun Headers.validate() =
    responseContract.headers.accumulate {
      when {
        it.isRequired.not() && hasHeader(it.name).not() -> success()
        it.isRequired && hasHeader(it.name).not()       -> failure("Response header '${it.name}' is missing")
        else                                            -> headerValue(it.name).matches(it)
      }
    }

  private fun Response.validateBody() =
    when {
      responseContract.body == null && contentType().isNullOrEmpty()   -> success()
      responseContract.body == null && !contentType().isNullOrEmpty()  -> failure("Expected no Content-Type but found: '${contentType()}'")
      responseContract.body != null && contentType().isNullOrEmpty()   -> failure("Content-Type is missing, expected '${responseContract.body!!.contentType}'")
      !contentType()!!.startsWith(responseContract.body!!.contentType) -> failure("Wrong Content-Type. Expected: ${responseContract.body!!.contentType}, Actual: '${contentType()}'")
      "json" !in contentType()!!.lowercase()                           -> failure("Content-Type'${contentType()}' is not managed")
      else                                                             -> bodyString().matches(responseContract.body!!)
    }

  private fun Headers.hasHeader(name: String) = any { it.first.lowercase() == name.lowercase() }
  private fun Headers.headerValue(name: String) = find { it.first.lowercase() == name.lowercase() }?.second

  private fun Response.contentType(): String? = header("Content-Type")
}

