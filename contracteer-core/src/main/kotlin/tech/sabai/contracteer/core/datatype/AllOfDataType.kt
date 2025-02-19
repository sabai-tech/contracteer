package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success

// TODO validate that sub schema does not have duplicated properties but with different type
class AllOfDataType private constructor(name: String,
                                        private val subTypes: List<StructuredObjectDataType>,
                                        isNullable: Boolean,
                                        allowedValues: AllowedValues? = null):
    StructuredObjectDataType(name, "allOf", isNullable, allowedValues) {

  override fun doValidate(value: Map<String, Any?>): Result<Map<String, Any?>> {
    val validationResults = subTypes.associateWith { it.validate(value) }
    val dataTypeErrors = validationResults.filterValues { it.isFailure() }
    return when {
      dataTypeErrors.isEmpty().not() -> buildNoMatchError(dataTypeErrors)
      else                           -> success(value)
    }
  }

  override fun doRandomValue() = subTypes.map { it.randomValue() }.reduce { acc, properties -> acc + properties }

  private fun buildNoMatchError(dataTypeErrors: Map<StructuredObjectDataType, Result<Any?>>): Result<Map<String, Any?>> =
    failure(
      "No schema match: ${System.lineSeparator()}" +
      dataTypeErrors.map {
        "   * ${it.key.name}" +
        it.value.errors().joinToString(
          prefix = "${System.lineSeparator()}     - ",
          separator = "${System.lineSeparator()}     - ")
      }.joinToString(separator = System.lineSeparator()))

  companion object {
    fun create(name: String = "Inline 'allOf' Schema",
               subTypes: List<StructuredObjectDataType>,
               isNullable: Boolean = false,
               enum: List<Any?>) =
      AllOfDataType(name, subTypes, isNullable).let { dataType ->
        if (enum.isEmpty()) success(dataType)
        else AllowedValues.create(enum, dataType).map { AllOfDataType(name, subTypes, isNullable, it) }
      }
  }
}