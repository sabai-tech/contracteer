package dev.blitzcraft.contracts.core

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.math.BigDecimal
import java.math.BigInteger

fun Any?.convert(): Any? = when {
  this is Short                     -> BigInteger.valueOf(toLong())
  this is Int                       -> BigInteger.valueOf(toLong())
  this is Long                      -> BigInteger.valueOf(this)
  this is Float                     -> BigDecimal.valueOf(toDouble())
  this is Double                    -> BigDecimal.valueOf(this)
  this is BigDecimal && isInteger() -> this.toBigInteger() // swagger parser convert Long and BigInteger to BigDecimal
  this is Map<*, *>                 -> this.mapValues { it.value.convert() }
  this is Array<*>                  -> this.map { it.convert() }.toTypedArray<Any?>()
  this is ObjectNode                -> Mappers.jsonMapper.convertValue(this, Map::class.java)
  this is ArrayNode                 -> Mappers.jsonMapper.convertValue(this, Array::class.java)
  else                              -> this
}

fun BigDecimal.isInteger() = stripTrailingZeros().scale() <= 0