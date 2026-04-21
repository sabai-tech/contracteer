package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.accumulate
import tech.sabai.contracteer.core.joinWithQuotes
import java.lang.System.lineSeparator

/** OpenAPI `oneOf` composition. The value must match exactly one sub-schema. */
class OneOfDataType private constructor(name: String,
                                        subTypes: List<DataType<out Any>>,
                                        val discriminator: Discriminator?,
                                        isNullable: Boolean,
                                        allowedValues: AllowedValues? = null):
    CompositeDataType<Any>(name, "oneOf", isNullable, subTypes, Any::class.java, allowedValues) {

  override fun isFullyStructured() =
    subTypes.all { it.isFullyStructured() }

  override fun asRequestType(): DataType<Any> =
    subTypes
      .map { it.asRequestType() }
      .let { transformed ->
        if (transformed.zip(subTypes).all { (a, b) -> a === b }) this
        else OneOfDataType(name, transformed, discriminator, isNullable, allowedValues)
      }

  override fun asResponseType(): DataType<Any> =
    subTypes
      .map { it.asResponseType() }
      .let { transformed ->
        if (transformed.zip(subTypes).all { (a, b) -> a === b }) this
        else OneOfDataType(name, transformed, discriminator, isNullable, allowedValues)
      }

  override fun doValidate(value: Any): Result<Any> =
    discriminator?.let { validateWithDiscriminator(value) } ?: validateWithoutDiscriminator(value)

  @Suppress("UNCHECKED_CAST")
  override fun doRandomValue(): Any? {
    val chosenType = subTypes.random()
    return if (discriminator == null) {
      chosenType.randomValue()
    } else {
      (chosenType as DataType<Map<String, Any?>>).randomValue()?.plus(
        discriminator.propertyName to discriminator.getMappingName(chosenType.name))
    }
  }

  private fun validateWithDiscriminator(value: Any): Result<Any> {
    val discriminatorValue = (value as? Map<*, *>)?.get(discriminator!!.propertyName)
    return if (discriminatorValue is String)
      dataTypeFrom(discriminatorValue).flatMap { it.validate(value) }.map { value }
    else
      validateWithoutDiscriminator(value)
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
      dataTypeSuccess.isEmpty() -> buildNoMatchError(dataTypeErrors)
      dataTypeSuccess.size > 1  -> buildMultipleMatchError(dataTypeSuccess)
      else                      -> success(value)
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

  private fun buildMultipleMatchError(dataTypeErrors: Map<DataType<out Any>, Result<Any?>>): Result<Map<String, Any?>> =
    failure(
      "Ambiguous match for 'oneOf'. The provided value matches multiple schemas (${
        dataTypeErrors
          .map { it.key.name }
          .joinWithQuotes()
      }). If multiple matches are acceptable, use 'anyOf'. Otherwise, differentiate the branches with 'pattern', 'enum', required properties, or a 'discriminator'.")

  companion object {
    @JvmStatic
    @JvmOverloads
    fun create(name: String,
               subTypes: List<DataType<out Any>>,
               discriminator: Discriminator? = null,
               isNullable: Boolean = false,
               enum: List<Any?> = emptyList()) =
      subTypes.validate(discriminator)
        .flatMap {
          OneOfDataType(name, subTypes, discriminator, isNullable).let { dataType ->
            if (enum.isEmpty()) success(dataType)
            else AllowedValues
              .create(enum, dataType)
              .map { OneOfDataType(name, subTypes, discriminator, isNullable, it) }
          }
        }

    private fun List<DataType<out Any>>.validate(discriminator: Discriminator?) =
      when {
        discriminator == null                           -> success(null)
        namesNotContains(discriminator.dataTypeNames()) -> failure("Discriminator mapping error. The discriminator references schemas not present in 'oneOf'")
        else                                            ->
          accumulate { discriminator.validate(it).forProperty(it.name) }.map { discriminator }
      }

    private fun List<DataType<out Any>>.namesNotContains(names: Collection<String>) =
      !map { it.name }.containsAll(names)
  }
}