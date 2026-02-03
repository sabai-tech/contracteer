package tech.sabai.contracteer.core.contract

import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.datatype.ArrayDataType
import tech.sabai.contracteer.core.datatype.DataType

@ConsistentCopyVisibility
data class Body private constructor(
  val contentType: ContentType,
  val dataType: DataType<out Any>,
  val isRequired: Boolean,
  val example: Example? = null) {

  fun content() = if (example != null) example.normalizedValue else dataType.randomValue()

  fun asString(): String = contentType.serde.serialize(content())

  companion object {
    fun create(contentType: ContentType,
               dataType: DataType<out Any>,
               isRequired: Boolean = false,
               example: Example? = null,
               validateExample: Boolean = true) =
      contentType
        .validate(dataType)
        .flatMap {
          when {
            example == null  -> success(Body(contentType, dataType, isRequired))
            !validateExample -> success(Body(contentType, dataType, isRequired, example))
            else             ->
              dataType
                .validate(example.normalizedValue)
                .map { Body(contentType, dataType, isRequired, example) }
          }
        }

    private fun ContentType.validate(dataType: DataType<out Any>) =
      if (isJson() && !dataType.isFullyStructured() && dataType !is ArrayDataType)
        failure("Content type $value supports only 'object', 'anyOf', 'oneOf', 'allOf' or 'array' schema")
      else
        success(dataType)
  }
}
