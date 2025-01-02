package dev.blitzcraft.contracts.core

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.math.BigDecimal
import java.math.BigInteger

fun Any?.normalize(): Any? = when (this) {
  is Short      -> BigDecimal.valueOf(toLong())
  is Int        -> BigDecimal.valueOf(toLong())
  is Long       -> BigDecimal.valueOf(this)
  is Byte       -> BigDecimal.valueOf(toLong())
  is BigInteger -> toBigDecimal()
  is Float      -> BigDecimal.valueOf(toDouble())
  is Double     -> BigDecimal.valueOf(this)
  is Map<*, *>  -> mapValues { it.value.normalize() }
  is Array<*>   -> map { it.normalize() }.toTypedArray<Any?>()
  is ObjectNode -> Mappers.jsonMapper.convertValue(this, Map::class.java)
  is ArrayNode  -> Mappers.jsonMapper.convertValue(this, Array::class.java)
  else          -> this
}