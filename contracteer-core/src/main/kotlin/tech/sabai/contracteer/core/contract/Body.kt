package tech.sabai.contracteer.core.contract

import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.datatype.DataType

@ConsistentCopyVisibility
data class Body private constructor(
  val contentType: ContentType,
  val dataType: DataType<out Any>,
  val isRequired: Boolean,
  val example: Example? = null) {

  fun content() = if (example != null) example.normalizedValue else dataType.randomValue()

  fun asString(): String = contentType.serialize(content())

  companion object {
    fun create(contentType: ContentType,
               dataType: DataType<out Any>,
               isRequired: Boolean = false,
               example: Example? = null) =
      contentType
        .validate(dataType)
        .flatMap {
          if (example == null) success(Body(contentType, dataType, isRequired))
          else dataType.validate(example.normalizedValue).map { Body(contentType, dataType, isRequired, example) }
        }
  }
}
