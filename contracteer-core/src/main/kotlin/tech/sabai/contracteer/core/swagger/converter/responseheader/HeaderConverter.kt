package tech.sabai.contracteer.core.swagger.converter.responseheader

import io.swagger.v3.oas.models.headers.Header
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.contract.ContractParameter
import tech.sabai.contracteer.core.swagger.converter.example.ExampleConverter
import tech.sabai.contracteer.core.swagger.converter.schema.SchemaConverter
import tech.sabai.contracteer.core.swagger.safeExamples
import tech.sabai.contracteer.core.swagger.safeIsRequired

object HeaderConverter {
  private const val COMPONENTS_PARAMETER_BASE_REF = "#/components/headers/"
  private const val MAX_RECURSIVE_DEPTH = 10
  lateinit var sharedHeaders: Map<String, Header>

  fun convert(name: String,
              header: Header,
              exampleKey: String?,
              maxRecursiveDepth: Int = MAX_RECURSIVE_DEPTH): Result<ContractParameter> {
    val ref = header.shortRef()
    return when {
      maxRecursiveDepth < 0              -> failure("Max recursive depth reached for Response Header")
      ref == null                        -> convertHeader(name, header, exampleKey)
      sharedHeaders[ref]?.`$ref` != null -> convert(name, sharedHeaders[ref]!!, exampleKey, maxRecursiveDepth - 1)
      sharedHeaders[ref] != null         -> convertHeader(name, sharedHeaders[ref]!!, exampleKey)
      else                               -> failure("Parameter' ${header.`$ref`}' not found in 'components/parameters' section")
    }
  }

  private fun convertHeader(name: String, header: Header, exampleKey: String?) =
    when {
      exampleKey == null                               -> success()
      !header.safeExamples().keys.contains(exampleKey) -> success()
      else                                             -> ExampleConverter.convert(header.safeExamples()[exampleKey]!!)
    }.flatMap { resolvedExample ->
      SchemaConverter.convertToDataType(header.schema, "")
        .flatMap { ContractParameter.create(name, it!!, header.safeIsRequired(), resolvedExample) }
    }

  private fun Header.shortRef() =
    this.`$ref`?.replace(COMPONENTS_PARAMETER_BASE_REF, "")
}