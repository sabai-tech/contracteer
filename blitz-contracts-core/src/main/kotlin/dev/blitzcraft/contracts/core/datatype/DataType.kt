package dev.blitzcraft.contracts.core.datatype

import dev.blitzcraft.contracts.core.ValidationResult
import io.swagger.v3.oas.models.media.*

interface DataType<out T> {

  fun regexPattern(): String
  fun nextValue(): T
  fun validateValue(value: Any): ValidationResult
  fun parseAndValidate(stringValue: String):ValidationResult

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

