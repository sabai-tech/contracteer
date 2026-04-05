package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.joinWithQuotes
import java.lang.System.lineSeparator

/** OpenAPI `allOf` composition. The value must match all sub-schemas simultaneously. */
class AllOfDataType private constructor(name: String,
                                        subTypes: List<DataType<out Any>>,
                                        isNullable: Boolean,
                                        val discriminator: Discriminator?,
                                        allowedValues: AllowedValues? = null):
    CompositeDataType<Any>(name,
                           "allOf",
                           isNullable,
                           subTypes,
                           Any::class.java,
                           allowedValues) {

  override fun isFullyStructured() = subTypes.all { it.isFullyStructured() }

  override fun asRequestType(): DataType<Any> =
    subTypes
      .map { it.asRequestType() }
      .let { transformed ->
        if (transformed.zip(subTypes).all { (a, b) -> a === b }) this
        else AllOfDataType(name, transformed, isNullable, discriminator, allowedValues)
      }

  override fun asResponseType(): DataType<Any> =
    subTypes
      .map { it.asResponseType() }
      .let { transformed ->
        if (transformed.zip(subTypes).all { (a, b) -> a === b }) this
        else AllOfDataType(name, transformed, isNullable, discriminator, allowedValues)
      }

  override fun doValidate(value: Any): Result<Any> {
    if (discriminator != null) {
      val discriminatorResult = validateDiscriminator(value)
      if (discriminatorResult.isFailure()) return discriminatorResult
    }
    val dataTypeErrors = subTypes
      .associateWith { it.validate(withoutSiblingProperties(value, it)) }
      .filterValues { it.isFailure() }

    return if (dataTypeErrors.isNotEmpty())
      buildNoMatchError(dataTypeErrors)
    else
      success(value)
  }

  @Suppress("UNCHECKED_CAST")
  private fun withoutSiblingProperties(value: Any, subType: DataType<out Any>): Any {
    if (value !is Map<*, *>) return value
    val siblingProperties = subTypes
      .filter { it !== subType }
      .flatMap { it.propertyNames() }
      .toSet()

    return if (siblingProperties.isEmpty())
      value
    else
      (value as Map<String, Any?>).filterKeys { it !in siblingProperties }
  }

  private fun DataType<*>.propertyNames(): Set<String> = when (this) {
    is ObjectDataType       -> properties.keys
    is CompositeDataType<*> -> subTypes.flatMapTo(mutableSetOf()) { it.propertyNames() }
    else                    -> emptySet()
  }

  @Suppress("UNCHECKED_CAST")
  override fun doRandomValue(): Any {
    val values = subTypes.map { it.randomValue() }
    if (values.any { it !is Map<*, *> }) return values.first()
    val randomValue = (values as List<Map<String, Any?>>).reduce { acc, properties -> acc + properties }
    return discriminator?.let { randomValue + (it.propertyName to it.getMappingName(name)) } ?: randomValue
  }

  private fun validateDiscriminator(value: Any): Result<Any> =
    when {
      value !is Map<*, *>                                                                   -> failure("Discriminator requires an object value")
      value[discriminator!!.propertyName] == null                                           -> failure("Discriminator property '${discriminator.propertyName}' is required")
      value[discriminator.propertyName] !is String                                          -> failure("Discriminator property '${discriminator.propertyName}' must be of type 'string'")
      discriminator.getDataTypeNameFor(value[discriminator.propertyName] as String) != name -> failure(
        "Invalid value for discriminator property '${discriminator.propertyName}'. " +
        "Expected '${discriminator.getMappingName(name)}', but found '${value[discriminator.propertyName]}'.")
      else                                                                                  -> success(value)
    }

  private fun buildNoMatchError(dataTypeErrors: Map<DataType<out Any>, Result<Any>>): Result<Any> {
    val schemaNames = dataTypeErrors.keys.map { it.name }.joinWithQuotes()
    val detailedErrors = dataTypeErrors.map { (dataType, result) ->
      "Schema '${dataType.name}':" + result.errors().joinToString(
        prefix = "${lineSeparator()}\t\t\t- ",
        separator = "${lineSeparator()}\t\t\t- "
      )
    }.joinToString(
      prefix = "\t\t- ",
      separator = "${lineSeparator()}\t\t- "
    )
    return failure(
      "No matching schema found. The provided value did not match any of the candidate schemas ($schemaNames):${lineSeparator()}$detailedErrors"
    )
  }

  companion object {
    @JvmStatic
    @JvmOverloads
    fun create(name: String,
               subTypes: List<DataType<out Any>>,
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

    private fun List<DataType<out Any>>.validate(discriminator: Discriminator?): Result<Discriminator?> {
      if (size > 1 && any { !it.isFullyStructured() }) return failure("Only structured schemas (object, allOf, anyOf, oneOf) are supported for multi-element 'allOf'.")
      if (discriminator == null) return success(null)

      val results = map { discriminator.validate(it).forProperty(it.name) }
      val successes = results.count { it.isSuccess() }
      return when {
        successes == 1 -> success(discriminator)
        successes > 1  -> failure("Ambiguous discriminator. The discriminator property '${discriminator.propertyName}' is present in multiple 'allOf' sub-schemas.")
        else           -> results.combineResults().retypeError()
      }
    }
  }
}
