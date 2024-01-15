package dev.blitzcraft.contracts.verifier

import dev.blitzcraft.contracts.core.contract.ContractResponse
import dev.blitzcraft.contracts.core.contract.matches
import dev.blitzcraft.contracts.core.validation.ValidationResult
import dev.blitzcraft.contracts.core.validation.ValidationResult.Companion.success
import dev.blitzcraft.contracts.core.validation.ValidationResult.Companion.error
import dev.blitzcraft.contracts.core.validation.validate
import org.http4k.core.Headers
import org.http4k.core.Response
import org.http4k.core.findSingle

internal class ResponseValidator(private val responseContract: ContractResponse) {

  fun validate(response: Response) =
    response.status.code.validate() and
        response.headers.validate() and
        response.validateBody()

  private fun Int.validate() =
    if (this == responseContract.statusCode) success()
    else error("Status code does not match. Expected: ${responseContract.statusCode}, Actual: $this")

  private fun Headers.validate() =
    responseContract.headers.validate {
      when {
        it.isRequired.not() && hasHeader(it.name).not() -> success()
        it.isRequired && hasHeader(it.name).not()       -> error(it.name, "Is Missing")
        else                                            -> findSingle(it.name).matches(it)
      }
    }

  private fun Response.validateBody(): ValidationResult {
    return when {
      responseContract.body == null && contentType().isNullOrEmpty()   -> success()
      responseContract.body == null && !contentType().isNullOrEmpty()  -> error("Expected no Content-Type but found: '${contentType()}'")
      responseContract.body != null && contentType().isNullOrEmpty()   -> error("Content-Type is missing, expected '${responseContract.body!!.contentType}'")
      !contentType()!!.startsWith(responseContract.body!!.contentType) -> error("Wrong Content-Type. Expected: ${responseContract.body!!.contentType}, Actual: '${contentType()}")
      "json" !in contentType()!!.lowercase()                           -> error("Content-Type'${contentType()}' is not managed")
      else                                                             -> bodyString().matches(responseContract.body!!)
    }
  }

  private fun Headers.hasHeader(name: String) = any { it.first == name }
  private fun Response.contentType(): String? = header("Content-Type")
}

