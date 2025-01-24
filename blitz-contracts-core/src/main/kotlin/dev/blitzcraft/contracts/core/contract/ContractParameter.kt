package dev.blitzcraft.contracts.core.contract

import dev.blitzcraft.contracts.core.datatype.*
import dev.blitzcraft.contracts.core.Result.Companion.failure
import dev.blitzcraft.contracts.core.Result.Companion.success

open class ContractParameter(
  val name: String,
  val dataType: DataType<*>,
  val isRequired: Boolean = false,
  val example: Example? = null) {

  fun hasExample() = example != null

  fun value(): Any? = if (hasExample()) example!!.normalizedValue else dataType.randomValue()

  fun stringValue(): String {
    return when (dataType) {
      is ObjectDataType -> TODO("Not yet implemented")
      is ArrayDataType  -> TODO("Not yet implemented")
      else                                -> value().toString()
    }
  }

  internal fun parseOrNull(value: String) =
    when (dataType) {
      is BooleanDataType          -> value.toBooleanStrictOrNull()
      is IntegerDataType          -> value.toBigDecimalOrNull()
      is NumberDataType           -> value.toBigDecimalOrNull()
      is StringDataType           -> value
      is UuidDataType             -> value
      is Base64DataType           -> value
      is BinaryDataType           -> value
      is EmailDataType            -> value
      is DateTimeDataType         -> value
      is DateDataType             -> value
      is StructuredObjectDataType -> TODO("Not yet implemented")
      is ArrayDataType            -> TODO("Not yet implemented")
    }
}

class PathParameter(
  name: String,
  dataType: DataType<*>,
  example: Example? = null): ContractParameter(name, dataType, true, example)

fun String?.matches(parameter: ContractParameter) = when {
  this == null && parameter.dataType.isNullable -> success()
  this == null                                  -> failure(parameter.name, "Cannot be null")
  else                                          -> {
    val value = parameter.parseOrNull(this)
    if (value != null) parameter.dataType.validate(value)
    else failure(parameter.name, "Wrong type. Expected type: ${parameter.dataType.openApiType}")
  }
}

fun String?.matchesExample(parameter: ContractParameter) = when {
  parameter.example == null                                 -> failure(parameter.name, "Example is not defined")
  this == null && !parameter.dataType.isNullable            -> failure(parameter.name, "Cannot be null")
  this == null && parameter.example.normalizedValue == null -> success()
  else                                                      -> validateEqualsExampleValue(parameter, this!!)
}

private fun validateEqualsExampleValue(parameter: ContractParameter, value: String) =
  when (val parsedValue = parameter.parseOrNull(value)) {
    null -> failure(parameter.name, "Wrong type. Expected type: ${parameter.dataType.openApiType}")
    else -> parameter.example!!.matches(parsedValue).forProperty(parameter.name)
  }