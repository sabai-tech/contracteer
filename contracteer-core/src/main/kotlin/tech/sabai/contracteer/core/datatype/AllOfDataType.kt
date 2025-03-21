package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.combineResults
import java.lang.System.lineSeparator

@Suppress("UNCHECKED_CAST")
class AllOfDataType private constructor(name: String,
                                        subTypes: List<DataType<Map<String, Any?>>>,
                                        isNullable: Boolean,
                                        val discriminator: Discriminator?,
                                        allowedValues: AllowedValues? = null):
    CompositeDataType<Map<String, Any?>>(name,
                                         "allOf",
                                         isNullable,
                                         subTypes,
                                         Map::class.java as Class<Map<String, Any?>>,
                                         allowedValues) {

  override fun isFullyStructured() = true

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

  private fun buildNoMatchError(dataTypeErrors: Map<DataType<out Map<String, Any?>>, Result<Map<String, Any?>>>): Result<Map<String, Any?>> {
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
    fun create(name: String,
               subTypes: List<DataType<Map<String, Any?>>>,
               isNullable: Boolean = false,
               discriminator: Discriminator? = null,
               enum: List<Any?> = emptyList()) =
      subTypes.validate(discriminator)
        .flatMap {
          val defaultDataType = AllOfDataType(name, subTypes, isNullable, discriminator)
          if (enum.isEmpty()) {
            success(defaultDataType)
          } else {
            AllowedValues
              .create(enum, defaultDataType)
              .map { AllOfDataType(name, subTypes, isNullable, discriminator, it) }
          }
        }

    private fun List<DataType<out Any>>.validate(discriminator: Discriminator?): Result<Discriminator> {
      if (discriminator == null) return success()
      val results = map { discriminator.validate(it).forProperty(it.name) }
      val successes = results.count { it.isSuccess() }
      return when {
        successes == 1 -> success(discriminator)
        successes > 1  -> failure("Discriminator property '${discriminator.propertyName}' is defined in multiple 'allOf' sub schemas")
        else           -> results.combineResults().retypeError()
      }
    }
  }
}