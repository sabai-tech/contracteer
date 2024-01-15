package dev.blitzcraft.contracts.core

import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS
import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS
import com.fasterxml.jackson.databind.ObjectMapper

object Mappers {
  val jsonMapper = ObjectMapper()
  init {
    jsonMapper.enable(USE_BIG_DECIMAL_FOR_FLOATS, USE_BIG_INTEGER_FOR_INTS)
  }
}