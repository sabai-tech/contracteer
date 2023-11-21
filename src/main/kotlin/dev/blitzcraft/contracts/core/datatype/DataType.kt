package dev.blitzcraft.contracts.core.datatype

import io.swagger.v3.oas.models.media.*

interface DataType<out T> {

  fun regexPattern(): String
  fun nextValue(): T

  companion object {
    fun from(schema: Schema<*>) = when (schema) {
      is BooleanSchema -> BooleanDataType()
      is IntegerSchema -> IntegerDataType()
      is NumberSchema  -> DecimalDataType()
      is StringSchema  -> StringDataType()
      is ObjectSchema  -> ObjectDataType(schema)
      is ArraySchema   -> ArrayDataType(schema)
      else             -> throw UnsupportedSchemeException(schema)
    }
  }
}

class UnsupportedSchemeException(schema: Schema<*>): Exception("Schema ${schema::class.java} is not supported")

