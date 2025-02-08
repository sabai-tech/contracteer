package tech.sabai.contracteer.core.contract

import tech.sabai.contracteer.core.Mappers.jsonMapper
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.datatype.ArrayDataType
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.datatype.StructuredObjectDataType

data class Body(
  val contentType: String,
  val dataType: DataType<*>,
  val example: Example? = null) {

  fun hasExample(): Boolean = example != null

  fun content() = if (hasExample()) example!!.normalizedValue else dataType.randomValue()

  fun asString(): String =
    when {
      "json" in contentType.lowercase() -> jsonMapper.writeValueAsString(content())
      else                              -> content().toString()
    }
}

fun String?.matches(body: Body) =
  when {
    this == null && body.dataType.isNullable                                            -> success()
    this == null                                                                        -> failure("Body cannot be null")
    body.dataType is StructuredObjectDataType && "json" in body.contentType.lowercase() -> parseAndValidate(this, body.dataType)
    body.dataType is ArrayDataType && "json" in body.contentType.lowercase()            -> parseAndValidate(this, body.dataType)
    else                                                                                -> body.dataType.validate(this)
  }

fun String?.matchesExample(body: Body) =
  when {
    body.example == null                                                                -> failure("Body Example is not defined")
    this == null && !body.dataType.isNullable                                           -> failure("Body cannot be null")
    this == null && body.example.normalizedValue == null                                -> success()
    body.dataType is StructuredObjectDataType && "json" in body.contentType.lowercase() -> parseAndValidateExample(this!!, body.example)
    body.dataType is ArrayDataType && "json" in body.contentType.lowercase()            -> parseAndValidateExample(this!!, body.example)
    else                                                                                -> body.example.matches(this)
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