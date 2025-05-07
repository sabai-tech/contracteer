package tech.sabai.contracteer.core.contract

import io.github.oshai.kotlinlogging.KotlinLogging
import tech.sabai.contracteer.core.Mappers.jsonMapper
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.datatype.AnyDataType
import tech.sabai.contracteer.core.datatype.ArrayDataType
import tech.sabai.contracteer.core.datatype.Base64DataType
import tech.sabai.contracteer.core.datatype.BinaryDataType
import tech.sabai.contracteer.core.datatype.BooleanDataType
import tech.sabai.contracteer.core.datatype.CompositeDataType
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.datatype.DateDataType
import tech.sabai.contracteer.core.datatype.DateTimeDataType
import tech.sabai.contracteer.core.datatype.EmailDataType
import tech.sabai.contracteer.core.datatype.IntegerDataType
import tech.sabai.contracteer.core.datatype.NumberDataType
import tech.sabai.contracteer.core.datatype.ObjectDataType
import tech.sabai.contracteer.core.datatype.StringDataType
import tech.sabai.contracteer.core.datatype.UuidDataType

private val logger = KotlinLogging.logger {}

data class ContentType(val value: String) {

  fun isJson() = "json" in value.lowercase()

  fun validate(contentType: String) =
    when {
      value.trim() == "*/*"                                     -> success(value)
      contentType.trim().startsWith(value.substringBefore("*")) -> success(value)
      else                                                      -> failure("'Content-type' does not match: Expected: $value, actual: $contentType")
    }

  fun validate(dataType: DataType<out Any>) =
    when {
      isJson() && !dataType.isFullyStructured() && dataType !is ArrayDataType -> failure("Content type $value supports only 'object', 'anyOf', 'oneOf', 'allOf' or 'array' schema")
      else -> success(dataType)
    }

  fun serialize(value: Any?): String =
    when {
      isJson() -> jsonMapper.writeValueAsString(value)
      else     -> value.toString()
    }

  fun parseValue(value: String?, dataType: DataType<out Any>): Result<Any?> =
    when {
      value == "null" || value.isNullOrEmpty() -> success()
      isJson()                                 -> parseJson(value, dataType)
      else                                     -> parse(value, dataType)
    }

  private fun parseJson(stringValue: String, dataType: DataType<out Any>) =
    try {
      success(jsonMapper.readValue(stringValue, dataType.dataTypeClass))
    } catch (e: Exception) {
      logger.debug { e }
      failure("Content type $value supports only schema 'object', 'anyOf', 'oneOf', 'allOf' or 'array' schema")
    }

  private fun parse(value: String, dataType: DataType<out Any>) =
    when (dataType) {
      is CompositeDataType,
      is ObjectDataType, is ArrayDataType   -> failure(dataType.name, "'object' and 'array' are not supported yet")
      is BooleanDataType                    -> value.asBoolean()
      is NumberDataType, is IntegerDataType -> value.asBigDecimal()
      is AnyDataType, is StringDataType,
      is UuidDataType, is Base64DataType,
      is BinaryDataType, is EmailDataType,
      is DateTimeDataType, is DateDataType  -> success(value)
    }
}

private fun String.asBoolean() =
  toBooleanStrictOrNull()?.let { success(it) } ?: failure("Wrong type. Expected type: 'boolean'")

private fun String.asBigDecimal() =
  toBigDecimalOrNull()?.let { success(it) } ?: failure("Wrong type. Expected type: 'number' or 'integer'")
