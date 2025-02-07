package dev.blitzcraft.contracts.core.datatype

import dev.blitzcraft.contracts.core.Result
import dev.blitzcraft.contracts.core.Result.Companion.failure
import dev.blitzcraft.contracts.core.Result.Companion.success

// TODO validate that sub schema does not have duplicated properties but with different type
class AllOfDataType(name: String = "Inline 'allOf' Schema",
                    val subTypes: List<StructuredObjectDataType>,
                    isNullable: Boolean = false): StructuredObjectDataType(name, "allO", isNullable) {

  override fun doValidate(value: Map<String, Any?>): Result<Map<String, Any?>> {
    val validationResults = subTypes.associateWith { it.validate(value) }
    val dataTypeErrors = validationResults.filterValues { it.isFailure() }
    return when {
      dataTypeErrors.isEmpty().not() -> buildNoMatchError(dataTypeErrors)
      else                           -> success(value)
    }
  }

  override fun randomValue() = subTypes.map { it.randomValue() }.reduce { acc, properties -> acc + properties }

  private fun buildNoMatchError(dataTypeErrors: Map<StructuredObjectDataType, Result<Any?>>): Result<Map<String, Any?>> =
    failure(
      "No schema match: ${System.lineSeparator()}" +
      dataTypeErrors.map {
        "   * ${it.key.name}" +
        it.value.errors().joinToString(
          prefix = "${System.lineSeparator()}     - ",
          separator = "${System.lineSeparator()}     - ")
      }.joinToString(separator = System.lineSeparator()))
}