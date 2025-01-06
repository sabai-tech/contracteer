package dev.blitzcraft.contracts.core.datatype

import dev.blitzcraft.contracts.core.validation.ValidationResult
import dev.blitzcraft.contracts.core.validation.ValidationResult.Companion.error
import dev.blitzcraft.contracts.core.validation.ValidationResult.Companion.success

class OneOfDataType(name: String = "Inline Schema",
                    val subTypes: List<StructuredObjectDataType>,
                    val discriminator: Discriminator? = null,
                    isNullable: Boolean = false): StructuredObjectDataType(name, "oneOf", isNullable) {

  override fun doValidate(value: Map<String, Any?>) =
    discriminator?.let { validateWithDiscriminator(value) } ?: validateWithoutDiscriminator(value)

  override fun randomValue(): Map<String, Any?> {
    val chosenType = subTypes.random()
    if (discriminator != null) {
      val discriminatingValue = discriminator.mapping
                                  .filterValues { it == chosenType }
                                  .keys
                                  .firstOrNull()
                                ?: chosenType.name
      return chosenType.randomValue() + (discriminator.propertyName to discriminatingValue)
    }
    return chosenType.randomValue()
  }

  private fun validateWithDiscriminator(value: Map<String, Any?>): ValidationResult {
    val discriminatorValue = value[discriminator!!.propertyName]
                             ?: return error("discriminator property '${discriminator.propertyName}' is required")
    val dataType = getDataTypeFrom(discriminatorValue)
                   ?: return error("No schema found for discriminator '${discriminator.propertyName}' with value: $discriminatorValue")

    return dataType.validate(value)
  }

  private fun getDataTypeFrom(discriminatorValue: Any?) =
    discriminator?.let { discriminator ->
      subTypes.firstOrNull { it.name == discriminator.propertyName } ?: discriminator.mapping[discriminatorValue]
    }

  private fun validateWithoutDiscriminator(value: Map<String, Any?>): ValidationResult {
    val validationResults = subTypes.associateWith { it.validate(value) }
    val dataTypeErrors = validationResults.filterValues { it.isSuccess().not() }
    val dataTypeSuccess = validationResults.filterValues { it.isSuccess() }

    return when {
      dataTypeSuccess.isEmpty() -> buildNoMatchError(dataTypeErrors)
      dataTypeSuccess.size > 1  -> buildMultipleMatchError(dataTypeSuccess)
      else                      -> validateDiscriminatorProperty(value, dataTypeSuccess.entries.first().key)
    }
  }

  private fun validateDiscriminatorProperty(value: Map<String, Any?>, dataType: StructuredObjectDataType) =
    when {
      discriminator == null                              -> success()
      value[discriminator.propertyName] == dataType.name -> success()

      else                                               -> error("the discriminator property $discriminator is wrong. Expected: ${dataType.name}, actual: ${value[discriminator.propertyName]}")
    }

  private fun buildNoMatchError(dataTypeErrors: Map<StructuredObjectDataType, ValidationResult>) =
    error(
      "No schema match: ${System.lineSeparator()}" +
      dataTypeErrors.map {
        "   * ${it.key.name}" +
        it.value.errors().joinToString(
          prefix = "${System.lineSeparator()}     - ",
          separator = "${System.lineSeparator()}     - ")
      }.joinToString(separator = System.lineSeparator()))


  private fun buildMultipleMatchError(dataTypeSuccess: Map<StructuredObjectDataType, ValidationResult>) =
    error("Multiple Schema match: " + dataTypeSuccess.map { it.key.name }.joinToString())
}