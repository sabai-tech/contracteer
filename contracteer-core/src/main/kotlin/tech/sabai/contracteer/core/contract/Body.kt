package tech.sabai.contracteer.core.contract

import io.github.oshai.kotlinlogging.KotlinLogging
import tech.sabai.contracteer.core.Mappers.jsonMapper
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.parse

private val logger = KotlinLogging.logger {}

@ConsistentCopyVisibility
data class Body private constructor(
  val contentType: ContentType,
  val dataType: DataType<out Any>,
  val example: Example? = null) {

  fun hasExample(): Boolean = example != null

  fun content() = if (hasExample()) example!!.normalizedValue else dataType.randomValue()

  fun validate(stringValue: String?) =
    when {
      stringValue == null && !dataType.isNullable -> failure("Body cannot be null")
      stringValue == null                         -> success(stringValue)
      example != null && contentType.isJson()     -> parseJson(stringValue).flatMap { example.validate(it) }
      example == null && contentType.isJson()     -> parseJson(stringValue).flatMap { dataType.validate(it) }
      else                                        -> dataType.parse(stringValue).flatMap { dataType.validate(it) }
    }.map { stringValue }

  fun asString(): String =
    when {
      contentType.isJson() -> jsonMapper.writeValueAsString(content())
      else                 -> content().toString()
    }

  private fun parseJson(stringValue: String) =
    try {
      success(jsonMapper.readValue(stringValue, dataType.dataTypeClass))
    } catch (e: Exception) {
      logger.debug { e }
      failure("Content type ${contentType.value} supports only schema 'object', 'anyOf', 'oneOf', 'allOf' or 'array' schema")
    }

  companion object {
    fun create(contentType: ContentType, dataType: DataType<out Any>, example: Example? = null) =
      contentType
        .validate(dataType)
        .flatMap {
          if (example == null) success(Body(contentType, dataType))
          else dataType.validate(example.normalizedValue).map { Body(contentType, dataType, example) }
        }
  }
}
