package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success

class AnyOfDataType private constructor(name: String,
                                        private val subTypes: List<StructuredObjectDataType>,
                                        val discriminator: Discriminator?,
                                        isNullable: Boolean,
                                        allowedValues: AllowedValues? = null):
    StructuredObjectDataType(name, "anyOf", isNullable, allowedValues) {

  override fun doValidate(value: Map<String, Any?>): Result<Map<String, Any?>> =
    discriminator?.let { validateWithDiscriminator(value) } ?: validateWithoutDiscriminator(value)

  override fun doRandomValue(): Map<String, Any?> {
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
    val discriminatingValue = value[discriminator!!.propertyName]
                              ?: return failure("discriminator property '${discriminator.propertyName}' is required")
    val dataType: StructuredObjectDataType = getDataTypeFrom(discriminatingValue)
                                             ?: return failure("No schema found for discriminator '${discriminator.propertyName}' with value: $discriminatingValue")


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
      else                      -> success(value)
    }
  }

  private fun buildNoMatchError(dataTypeErrors: Map<StructuredObjectDataType, Result<Any?>>): Result<Map<String, Any?>> =
    failure(
      dataTypeErrors.map {
        "Schema '${it.key.name}'" +
        it.value.errors().joinToString(
          prefix = "${System.lineSeparator()}     - ",
          separator = "${System.lineSeparator()}     - ")
      }.joinToString(separator = System.lineSeparator()))

  companion object {
    fun create(name: String = "Inline 'allOf' Schema",
               subTypes: List<StructuredObjectDataType>,
               discriminator: Discriminator? = null,
               isNullable: Boolean = false,
               enum: List<Any?>) =
      AnyOfDataType(name, subTypes, discriminator, isNullable).let { dataType ->
        if (enum.isEmpty()) success(dataType)
        else AllowedValues.create(enum, dataType).map { AnyOfDataType(name, subTypes, discriminator, isNullable, it) }
      }
  }
}