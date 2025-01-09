package dev.blitzcraft.contracts.core.contract

import dev.blitzcraft.contracts.core.Mappers.jsonMapper
import dev.blitzcraft.contracts.core.datatype.*
import dev.blitzcraft.contracts.core.Result.Companion.success
import dev.blitzcraft.contracts.core.Result.Companion.failure

data class Body(
  val contentType: String,
  val dataType: DataType<*>,
  val example: Example? = null) {

  init {
    if ("json" in contentType) {
      require(dataType is StructuredObjectDataType || dataType is ArrayDataType) { "Body with Content Type '$contentType' accepts only object type" }
      example?.normalizedValue?.let { require(it is Map<*, *> || it is Array<*>) { "Example value is not an object or an array" } }
    }
  }

  fun hasExample(): Boolean = example != null

  fun content() = if (hasExample()) example!!.normalizedValue else dataType.randomValue()

  fun asString(): String =
    when {
      "json" in contentType.lowercase() -> jsonMapper.writeValueAsString(content())
      else                              -> throw IllegalStateException("Only JSON content type is managed for now")
    }
}

fun String?.matches(body: Body) =
  when {
    this == null && body.dataType.isNullable                          -> success()
    this == null                                                      -> failure("Body cannot be null")
    "json" !in body.contentType.lowercase()                           -> failure("Only JSON content type is managed")
    body.dataType is ObjectDataType || body.dataType is ArrayDataType -> parseAndValidate(this, body.dataType)
    else                                                              -> failure("Body with Content Type '${body.contentType}' accepts only object or array type")
  }

fun String?.matchesExample(body: Body) =
  when {
    body.example == null                                              -> failure("Body Example is not defined")
    this == null && !body.dataType.isNullable                         -> failure("Body cannot be null")
    this == null && body.example.normalizedValue == null              -> success()
    "json" !in body.contentType.lowercase()                           -> failure("Only JSON content type is managed")
    body.dataType is ObjectDataType || body.dataType is ArrayDataType -> parseAndValidateExample(this!!, body.example)
    else                                                              -> failure("Body with Content Type '${body.contentType}' accepts only object or array type")
  }

private fun parseAndValidate(stringValue: String, dataType: DataType<*>) =
  try {
    val value = jsonMapper.readValue(stringValue, dataType.dataTypeClass)
    dataType.validate(value)
  } catch (e: Exception) {
    failure("Body does not match the expected type. Expected type: ${dataType.openApiType}")
  }

private fun parseAndValidateExample(stringValue: String, example: Example) =
  try {
    val value = jsonMapper.readValue(stringValue, example.normalizedValue!!::class.java)
    example.matches(value)
  } catch (e: Exception) {
    failure("Body does not match the expected type")
  }