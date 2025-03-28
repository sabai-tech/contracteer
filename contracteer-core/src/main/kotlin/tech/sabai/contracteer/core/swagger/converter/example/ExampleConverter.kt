package tech.sabai.contracteer.core.swagger.converter.example

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.contract.Example

object ExampleConverter {
  private const val COMPONENTS_EXAMPLE_BASE_REF = "#/components/examples/"
  private const val MAX_RECURSIVE_DEPTH = 10

  lateinit var sharedExamples: Map<String, io.swagger.v3.oas.models.examples.Example>

  fun convert(example: io.swagger.v3.oas.models.examples.Example,
              maxRecursiveDepth: Int = MAX_RECURSIVE_DEPTH): Result<Example> {
    val ref = example.shortRef()
    return when {
      maxRecursiveDepth < 0               -> failure("Maximum recursive depth reached while converting Example.")
      ref == null                         -> success(Example(example.value))
      sharedExamples[ref]?.`$ref` != null -> convert(sharedExamples[ref]!!, maxRecursiveDepth - 1)
      sharedExamples[ref] != null         -> success(Example(sharedExamples[ref]!!.value))
      else                                -> failure("Example '${example.`$ref`}' not found in 'components/examples' section")
    }
  }

  private fun io.swagger.v3.oas.models.examples.Example.shortRef() =
    this.`$ref`?.replace(COMPONENTS_EXAMPLE_BASE_REF, "")
}