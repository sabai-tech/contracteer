package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success

class OneOfDataType(name: String = "Inline 'oneOf' Schema",
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

  private fun validateWithDiscriminator(value: Map<String, Any?>): Result<Map<String, Any?>> {
    val discriminatorValue = value[discriminator!!.propertyName]
                             ?: return failure("discriminator property '${discriminator.propertyName}' is required")
    val dataType = getDataTypeFrom(discriminatorValue)
                   ?: return failure("No schema found for discriminator '${discriminator.propertyName}' with value: $discriminatorValue")

    return dataType.validate(value)
  }

  private fun getDataTypeFrom(discriminatorValue: Any?) =
    discriminator?.let { discriminator ->
      subTypes.firstOrNull { it.name == discriminator.propertyName } ?: discriminator.mapping[discriminatorValue]
    }

  private fun validateWithoutDiscriminator(value: Map<String, Any?>): Result<Map<String, Any?>> {
    val validationResults = subTypes.associateWith { it.validate(value) }
    val dataTypeErrors = validationResults.filterValues { it.isFailure() }
    val dataTypeSuccess = validationResults.filterValues { it.isSuccess() }

    return when {
      dataTypeSuccess.isEmpty() -> buildNoMatchError(dataTypeErrors)
      dataTypeSuccess.size > 1  -> buildMultipleMatchError(dataTypeSuccess)
      else                      -> validateDiscriminatorProperty(value, dataTypeSuccess.entries.first().key)
    }
  }

  private fun validateDiscriminatorProperty(value: Map<String, Any?>, dataType: StructuredObjectDataType) =
    when {
      discriminator == null                              -> success(value)
      value[discriminator.propertyName] == dataType.name -> success(value)

      else                                               -> failure("the discriminator property $discriminator is wrong. Expected: ${dataType.name}, actual: ${value[discriminator.propertyName]}")
    }

  private fun buildNoMatchError(dataTypeErrors: Map<StructuredObjectDataType, Result<Map<String, Any?>>>): Result<Map<String, Any?>> =
    failure(
      "No schema match: ${System.lineSeparator()}" +
      dataTypeErrors.map {
        "   * ${it.key.name}" +
        it.value.errors().joinToString(
          prefix = "${System.lineSeparator()}     - ",
          separator = "${System.lineSeparator()}     - ")
      }.joinToString(separator = System.lineSeparator()))


  private fun buildMultipleMatchError(dataTypeSuccess: Map<StructuredObjectDataType, Result<Map<String, Any?>>>): Result<Map<String, Any?>> =
    failure("Multiple Schema match: " + dataTypeSuccess.map { it.key.name }.joinToString())
}