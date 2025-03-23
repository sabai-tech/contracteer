package tech.sabai.contracteer.core.swagger.converter.example

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.contract.Example

object ExampleConverter {
  const val COMPONENTS_EXAMPLE_BASE_REF = "#/components/examples/"
  private var exampleCache: Map<String, Example> = emptyMap()

  fun setSharedExamples(sharedExamples: Map<String, io.swagger.v3.oas.models.examples.Example>) {
    exampleCache = sharedExamples.mapValues { Example(it.value.value) }
  }

  fun convert(example: io.swagger.v3.oas.models.examples.Example): Result<Example> {
    val ref = example.shortRef()
    return when {
      ref == null                   -> success(Example(example.value))
      exampleCache.containsKey(ref) -> success(exampleCache[ref])
      else                          -> failure("Example '${example.`$ref`}' not found in 'components/examples' section")
    }
  }

  private fun io.swagger.v3.oas.models.examples.Example.shortRef() =
    this.`$ref`?.replace(COMPONENTS_EXAMPLE_BASE_REF, "")
}