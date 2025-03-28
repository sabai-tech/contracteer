package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.accumulate
import tech.sabai.contracteer.core.joinWithQuotes
import java.lang.System.lineSeparator

class AnyOfDataType private constructor(name: String,
                                        subTypes: List<DataType<out Any>>,
                                        val discriminator: Discriminator? = null,
                                        isNullable: Boolean = false,
                                        allowedValues: AllowedValues? = null):
    CompositeDataType<Any>(name, "anyOf", isNullable, subTypes, Object::class.java, allowedValues) {

  override fun isFullyStructured() =
    subTypes.all { it.isFullyStructured() }

  override fun doValidate(value: Any): Result<Any> =
    if (discriminator != null)
      validateWithDiscriminator(value)
    else
      validateWithoutDiscriminator(value)

  @Suppress("UNCHECKED_CAST")
  override fun doRandomValue(): Any {
    val chosenType = subTypes.random()
    return if (discriminator == null) {
      chosenType.randomValue()
    } else {
      (chosenType as DataType<Map<String, Any?>>).randomValue() +
      (discriminator.propertyName to (discriminator.getMappingName(chosenType.name)))
    }
  }

  private fun validateWithDiscriminator(value: Any) =
    when {
      value !is Map<*, *>                          -> failure("Wrong type, expected 'object' type")
      value[discriminator!!.propertyName] == null  -> failure("discriminator property '${discriminator.propertyName}' is required")
      value[discriminator.propertyName] !is String -> failure("discriminator property '${discriminator.propertyName}' must be of type 'string'")
      else                                         ->
        dataTypeFrom(value[discriminator.propertyName] as String).flatMap { it!!.validate(value) }
    }

  private fun dataTypeFrom(discriminatorValue: String): Result<DataType<out Any>> =
    subTypes.firstOrNull { it.name == discriminator!!.getDataTypeNameFor(discriminatorValue) }
      ?.let { success(it) }
    ?: failure("No schema found for discriminator property '${discriminator!!.propertyName}' with value: $discriminatorValue")

  private fun validateWithoutDiscriminator(value: Any): Result<Any> {
    val validationResults = subTypes.associateWith { it.validate(value) }
    val dataTypeErrors = validationResults.filterValues { it.isFailure() }
    val dataTypeSuccess = validationResults.filterValues { it.isSuccess() }
    return when {
      dataTypeSuccess.isNotEmpty() -> success(value)
      else                         -> buildNoMatchError(dataTypeErrors)
    }
  }

  private fun buildNoMatchError(dataTypeErrors: Map<DataType<out Any>, Result<Any?>>): Result<Map<String, Any?>> {
    val schemaNames = dataTypeErrors.keys.map { it.name }.joinWithQuotes()
    val detailedErrors = dataTypeErrors.map { (dataType, result) ->
      "${lineSeparator()}  - Schema '${dataType.name}':" + result.errors().joinToString(
        prefix = "${lineSeparator()}      - ",
        separator = "${lineSeparator()}      - "
      )
    }.joinToString("")
    return failure("No matching schema. Value did not match candidate schemas ($schemaNames):${lineSeparator()}$detailedErrors")
  }

  companion object {
    fun create(name: String,
               subTypes: List<DataType<out Any>>,
               discriminator: Discriminator? = null,
               isNullable: Boolean = false,
               enum: List<Any?> = emptyList()) =
      subTypes.validate(discriminator)
        .flatMap {
          AnyOfDataType(name, subTypes, discriminator, isNullable).let { dataType ->
            if (enum.isEmpty()) success(dataType)
            else AllowedValues
              .create(enum, dataType)
              .map { AnyOfDataType(name, subTypes, discriminator, isNullable, it) }
          }
        }

    private fun List<DataType<out Any>>.validate(discriminator: Discriminator?) =
      when {
        discriminator == null                           -> success()
        namesNotContains(discriminator.dataTypeNames()) -> failure("Discriminator mapping error. The discriminator references schemas not present in 'anyOf'")
        else                                            ->
          accumulate { discriminator.validate(it).forProperty(it.name) }.map { discriminator }
      }

    private fun List<DataType<out Any>>.namesNotContains(names: Collection<String>) =
      !map { it.name }.containsAll(names)
  }
}
