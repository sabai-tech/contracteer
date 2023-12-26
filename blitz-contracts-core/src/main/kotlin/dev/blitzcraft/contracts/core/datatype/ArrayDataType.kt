package dev.blitzcraft.contracts.core.datatype

import com.fasterxml.jackson.databind.node.ArrayNode
import dev.blitzcraft.contracts.core.CompositeValidationResult
import dev.blitzcraft.contracts.core.SimpleValidationResult
import io.swagger.v3.oas.models.media.ArraySchema

data class ArrayDataType(val itemDataType: DataType<Any>): DataType<Array<Any>> {

  constructor(schema: ArraySchema): this(DataType.from(schema.items))

  override fun nextValue(): Array<Any> = Array((1..5).random()) { itemDataType.nextValue() }

  override fun validateValue(value: Any) = when (value) {
    is Array<*>  -> CompositeValidationResult(value.filterNotNull().validateElements())
    is ArrayNode -> CompositeValidationResult(value.filterNotNull().validateElements())
    else         -> SimpleValidationResult("The value has wrong type. Expected type: Array")
  }

  override fun parseAndValidate(stringValue: String) = SimpleValidationResult("Parsing an array is not yet supported")

  override fun regexPattern() = throw UnsupportedOperationException("String Pattern for Array is not defined")

  private fun <T> Iterable<T>.validateElements() =
    mapIndexed { index, element -> itemDataType.validateValue(element!!).forProperty("[$index]") }
}
