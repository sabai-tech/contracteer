package dev.blitzcraft.contracts.core.datatype

import dev.blitzcraft.contracts.core.validation.ValidationResult
import dev.blitzcraft.contracts.core.validation.ValidationResult.Companion.error
import dev.blitzcraft.contracts.core.validation.ValidationResult.Companion.success

// TODO validate that sub schema does not have duplicated properties but with different type
class AllOfDataType(name: String = "Inline Schema",
                    private val objectDataTypes: List<ObjectDataType>,
                    isNullable: Boolean = false):
    StructuredObjectDataType(name, "anyOf", isNullable) {

  override fun doValidate(value: Map<String, Any?>): ValidationResult {
    val validationResults = objectDataTypes.associateWith { it.validate(value) }
    val dataTypeErrors = validationResults.filterValues { it.isSuccess().not() }
    return when {
      dataTypeErrors.isEmpty().not() -> buildNoMatchError(dataTypeErrors)
      else                           -> success()
    }
  }

  override fun randomValue() = objectDataTypes.map { it.randomValue() }.reduce { acc, properties -> acc + properties }

  private fun buildNoMatchError(dataTypeErrors: Map<ObjectDataType, ValidationResult>) =
    error(
      "No schema match: ${System.lineSeparator()}" +
      dataTypeErrors.map {
        "   * ${it.key.name}" +
        it.value.errors().joinToString(
          prefix = "${System.lineSeparator()}     - ",
          separator = "${System.lineSeparator()}     - ")
      }.joinToString(separator = System.lineSeparator()))
}