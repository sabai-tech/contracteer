package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.combineResults
import java.lang.System.*

@Suppress("UNCHECKED_CAST")
class AllOfDataType private constructor(name: String,
                                        val subTypes: List<CompositeDataType<Map<String, Any?>>>,
                                        isNullable: Boolean,
                                        val discriminator: Discriminator?,
                                        allowedValues: AllowedValues? = null):
    CompositeDataType<Map<String, Any?>>(name,
                                         "allOf",
                                         isNullable,
                                         Map::class.java as Class<Map<String, Any?>>,
                                         allowedValues) {

  override fun doValidate(value: Map<String, Any?>) =
    validateWithDiscriminator(value)
      .flatMap {
        val dataTypeErrors = subTypes.associateWith { it.validate(value) }.filterValues { it.isFailure() }
        if (dataTypeErrors.isNotEmpty())
          buildNoMatchError(dataTypeErrors)
        else
          success(value)
      }

  override fun doRandomValue(): Map<String, Any?> {
    val randomValue = subTypes
      .map { it.randomValue() }
      .reduce { acc, properties -> acc + properties }

    return discriminator?.let { randomValue + (it.propertyName to it.getMappingName(name)) } ?: randomValue
  }

  override fun isStructured() = true

  override fun hasDiscriminatorProperty(name: String) =
    if (subTypes.map { it.hasDiscriminatorProperty(name) }.any { it.isSuccess() }) success(name)
    else failure("Discriminator property '$name' is missing or is not a 'string'")

  private fun validateWithDiscriminator(value: Map<String, Any?>) =
    when {
      discriminator == null                                                                 -> success()
      value[discriminator.propertyName] == null                                             -> failure("Discriminator property '${discriminator.propertyName}' is required")
      value[discriminator.propertyName] !is String                                          -> failure("Discriminator property '${discriminator.propertyName}' must be of type 'string'")
      discriminator.getDataTypeNameFor(value[discriminator.propertyName] as String) != name -> failure(
        "Invalid value for discriminator property '${discriminator.propertyName}'. Expected: '${
          discriminator.getMappingName(name)
        }', but found: '${value[discriminator.propertyName]}'.")
      else                                                                                  -> success(value)
    }

  private fun buildNoMatchError(dataTypeErrors: Map<CompositeDataType<Map<String, Any?>>, Result<Map<String, Any?>>>): Result<Map<String, Any?>> {
    val errorMessages = dataTypeErrors.map { (dataType, result) ->
      "Schema '${dataType.name}':${
        result
          .errors()
          .joinToString(lineSeparator() + "     - ", lineSeparator() + "     - ")
      }"
    }.joinToString(lineSeparator())
    return failure(errorMessages)
  }

  companion object {
    fun create(name: String = "Inline 'allOf' Schema",
               subTypes: List<CompositeDataType<Map<String, Any?>>>,
               isNullable: Boolean = false,
               discriminator: Discriminator? = null,
               enum: List<Any?> = emptyList()) =
      subTypes.validate(discriminator)
        .flatMap {
          AllOfDataType(name, subTypes, isNullable, discriminator).let { dataType ->
            if (enum.isEmpty()) success(dataType)
            else AllowedValues
              .create(enum, dataType)
              .map { AllOfDataType(name, subTypes, isNullable, discriminator, allowedValues = it) }
          }
        }

    private fun List<CompositeDataType<*>>.validate(discriminator: Discriminator?) =
      discriminator?.let {
        val discriminatorResults = map { it.hasDiscriminatorProperty(discriminator.propertyName) }
        val successes = discriminatorResults.count { it.isSuccess() }
        when {
          successes == 1 -> success(discriminator)
          successes > 1  -> failure("Discriminator property '${discriminator.propertyName}' is defined in multiple 'allOf' sub schemas")
          else           -> discriminatorResults.combineResults()
        }
      } ?: success()
  }
}