package dev.blitzcraft.contracts.core

import com.fasterxml.jackson.databind.ObjectMapper
import dev.blitzcraft.contracts.core.datatype.ArrayDataType
import dev.blitzcraft.contracts.core.datatype.DataType
import dev.blitzcraft.contracts.core.datatype.ObjectDataType

data class Body(val contentType: String, val dataType: DataType<Any>, val example: Example? = null) {

  init {
    if ("json" in contentType) {
      require(dataType is ObjectDataType || dataType is ArrayDataType) { "Body with Content Type '$contentType' accepts only object type" }
      example?.let {
        require(it.value is Map<*, *> || it.value is Array<*>) { "Example value is not an object or an array" }
      }
    }
  }

  fun content(): Any? = if (example != null) example.value else dataType.nextValue()

  fun asString(): String =
    when {
      "json" in contentType -> ObjectMapper().writeValueAsString(content())
      else                  -> content().toString()
    }


}
