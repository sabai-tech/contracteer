package tech.sabai.contracteer.core

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
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

fun Collection<*>.joinWithQuotes(): String =
  joinToString(separator = "', '", prefix = "'", postfix = "'")
