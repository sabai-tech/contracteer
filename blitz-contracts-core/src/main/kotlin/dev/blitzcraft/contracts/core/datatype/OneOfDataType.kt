package dev.blitzcraft.contracts.core.datatype

import dev.blitzcraft.contracts.core.validation.ValidationResult
import dev.blitzcraft.contracts.core.validation.ValidationResult.Companion.error
import dev.blitzcraft.contracts.core.validation.ValidationResult.Companion.success

class OneOfDataType(name: String = "Inline Schema",
                    private val objectDataTypes: List<ObjectDataType>,
                    isNullable: Boolean = false):
    StructuredObjectDataType(name, "oneOf", isNullable) {

  override fun doValidate(value: Map<String, Any?>): ValidationResult {
    val validationResults = objectDataTypes.associateWith { it.validate(value) }
    val dataTypeErrors = validationResults.filterValues { it.isSuccess().not() }
    val dataTypeSuccess = validationResults.filterValues { it.isSuccess() }
    return when {
      dataTypeSuccess.isEmpty() -> buildNoMatchError(dataTypeErrors)
      dataTypeSuccess.size > 1  -> buildMultipleMatchError(dataTypeSuccess)
      else                      -> success()
    }
  }

  override fun randomValue() = objectDataTypes.random().randomValue()

  private fun buildNoMatchError(dataTypeErrors: Map<ObjectDataType, ValidationResult>) =
    error(
      "No schema match: ${System.lineSeparator()}" +
      dataTypeErrors.map {
        "   * ${it.key.name}" +
        it.value.errors().joinToString(
          prefix = "${System.lineSeparator()}     - ",
          separator = "${System.lineSeparator()}     - ")
      }.joinToString(separator = System.lineSeparator()))


  private fun buildMultipleMatchError(dataTypeSuccess: Map<ObjectDataType, ValidationResult>) =
    error("Multiple Schema match: " + dataTypeSuccess.map { it.key.name }.joinToString())
}