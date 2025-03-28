package tech.sabai.contracteer.core.swagger.converter.parameter

import io.swagger.v3.oas.models.parameters.Parameter
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.contract.ContractParameter
import tech.sabai.contracteer.core.swagger.converter.schema.SchemaConverter
import tech.sabai.contracteer.core.swagger.converter.example.ExampleConverter
import tech.sabai.contracteer.core.swagger.safeIsRequired

object ParameterConverter {
  private const val COMPONENTS_PARAMETER_BASE_REF = "#/components/parameters/"
  private const val MAX_RECURSIVE_DEPTH = 10
  lateinit var sharedParameters: Map<String, Parameter>

  fun convert(parameter: Parameter,
              exampleKey: String?,
              maxRecursiveDepth: Int = MAX_RECURSIVE_DEPTH): Result<ContractParameter> {
    val ref = parameter.shortRef()
    return when {
      maxRecursiveDepth < 0                 -> failure("Maximum recursive depth reached while converting Parameter")
      ref == null                           -> convertParameter(parameter, exampleKey)
      sharedParameters[ref]?.`$ref` != null -> convert(sharedParameters[ref]!!, exampleKey, maxRecursiveDepth - 1)
      sharedParameters[ref] != null         -> convertParameter(sharedParameters[ref]!!, exampleKey)
      else                                  -> failure("Parameter' ${parameter.`$ref`}' not found in 'components/parameters' section")
    }
  }

  private fun convertParameter(parameter: Parameter, exampleKey: String?) =
    when {
      exampleKey == null                                  -> success()
      !parameter.safeExamples().keys.contains(exampleKey) -> success()
      else                                                -> ExampleConverter.convert(parameter.safeExamples()[exampleKey]!!)
    }.flatMap { resolvedExample ->
      SchemaConverter
        .convertToDataType(parameter.schema, "")
        .flatMap { ContractParameter.create(parameter.name, it!!, parameter.safeIsRequired(), resolvedExample) }
    }

  private fun Parameter.shortRef() =
    this.`$ref`?.replace(COMPONENTS_PARAMETER_BASE_REF, "")

  private fun Parameter.safeExamples() =
    examples ?: emptyMap()
}