package tech.sabai.contracteer.core.contract

import io.github.oshai.kotlinlogging.KotlinLogging
import tech.sabai.contracteer.core.Mappers.jsonMapper
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.datatype.ArrayDataType
import tech.sabai.contracteer.core.datatype.DataType

private val logger = KotlinLogging.logger {}

@ConsistentCopyVisibility
data class Body private constructor(
  val contentType: ContentType,
  val dataType: DataType<*>,
  val example: Example? = null) {

  fun hasExample(): Boolean = example != null

  fun content() = if (hasExample()) example!!.normalizedValue else dataType.randomValue()

  fun asString(): String =
    when {
      contentType.isJson() -> jsonMapper.writeValueAsString(content())
      else                 -> content().toString()
    }

  companion object {
    fun create(contentType: ContentType, dataType: DataType<out Any>, example: Example? = null) =
      contentType.support(dataType)
        .flatMap {
          if (example == null) success(Body(contentType, dataType))
          else dataType.validate(example.normalizedValue).map { Body(contentType, dataType, example) }
        }
  }
}

fun String?.matches(body: Body) =
  when {
    this == null && body.dataType.isNullable                       -> success()
    this == null                                                   -> failure("Body cannot be null")
    body.contentType.isJson() && body.dataType is ArrayDataType    -> parseAndValidate(this, body.dataType)
    body.contentType.isJson() && body.dataType.isFullyStructured() -> parseAndValidate(this, body.dataType)
    else                                                           -> body.dataType.validate(this)
  }

fun String?.matchesExample(body: Body) =
  when {
    body.example == null                                           -> failure("Body Example is not defined")
    this == null && !body.dataType.isNullable                      -> failure("Body cannot be null")
    this == null && body.example.normalizedValue == null           -> success()
    body.contentType.isJson() && body.dataType is ArrayDataType    -> parseAndValidateExample(this!!, body.example)
    body.contentType.isJson() && body.dataType.isFullyStructured() -> parseAndValidateExample(this!!, body.example)
    else                                                           -> body.example.matches(this)
  }

private fun parseAndValidate(stringValue: String, dataType: DataType<*>) =
  try {
    val value = jsonMapper.readValue(stringValue, dataType.dataTypeClass)
    dataType.validate(value)
  } catch (e: Exception) {
    logger.debug { e }
    failure("Body does not match the expected type. Expected type: ${dataType.openApiType}")
  }

private fun parseAndValidateExample(stringValue: String, example: Example) =
  try {
    val value = jsonMapper.readValue(stringValue, example.normalizedValue!!::class.java)
    example.matches(value)
  } catch (e: Exception) {
    logger.debug { e }
    failure("Body does not match the expected type")
  }