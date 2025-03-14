package tech.sabai.contracteer.core

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
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
import java.math.BigDecimal
import java.math.BigInteger

fun Any?.normalize(): Any? =
  when (this) {
    is Short         -> BigDecimal.valueOf(toLong())
    is Int           -> BigDecimal.valueOf(toLong())
    is Long          -> BigDecimal.valueOf(this)
    is Byte          -> BigDecimal.valueOf(toLong())
    is BigInteger    -> toBigDecimal()
    is Float         -> BigDecimal.valueOf(toDouble())
    is Double        -> BigDecimal.valueOf(this)
    is Map<*, *>     -> mapValues { it.value.normalize() }
    is Collection<*> -> map { it.normalize() }
    is Array<*>      -> map { it.normalize() }.toList()
    is ObjectNode    -> Mappers.jsonMapper.convertValue(this, Map::class.java).mapValues { it.value.normalize() }
    is ArrayNode     -> Mappers.jsonMapper.convertValue(this, List::class.java).map { it.normalize() }
    else             -> this
  }


// TODO refactor it when supporting ObjectDataType and ArrayDataType for Parameter
fun DataType<out Any>.parse(value: String?) =
  if (value == null) success()
  else {
    when (this) {
      is CompositeDataType,
      is ObjectDataType, is ArrayDataType   -> failure(name, "'object' and 'array' are not supported yet")
      is BooleanDataType                    -> value.asBoolean()
      is NumberDataType, is IntegerDataType -> value.asBigDecimal()
      is StringDataType,
      is UuidDataType, is Base64DataType,
      is BinaryDataType, is EmailDataType,
      is DateTimeDataType, is DateDataType  -> success(value)
    }
  }

private fun String.asBoolean() =
  toBooleanStrictOrNull()?.let { success(it) } ?: failure("Wrong type. Expected type: 'boolean'")

private fun String.asBigDecimal() =
  toBigDecimalOrNull()?.let { success(it) } ?: failure("Wrong type. Expected type: 'number' or 'integer'")
