package tech.sabai.contracteer.core.contract

import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.datatype.*
import tech.sabai.contracteer.core.parse

@ConsistentCopyVisibility
data class ContractParameter private constructor(
  val name: String,
  val dataType: DataType<out Any>,
  val isRequired: Boolean = false,
  val example: Example? = null) {

  fun value(): Any? = if (example != null) example.normalizedValue else dataType.randomValue()

  fun stringValue(): String {
    return when {
      dataType.isFullyStructured() -> TODO("Not yet implemented")
      dataType is ArrayDataType    -> TODO("Not yet implemented")
      else                         -> value().toString()
    }
  }

  fun validate(stringValue: String?) =
    if (stringValue == null && !dataType.isNullable) failure(name, "Cannot be null")
    else {
     dataType.parse(stringValue)
        .flatMap { example?.validate(it) ?: dataType.validate(it) }
        .forProperty(name)
        .map { stringValue }
    }

  companion object {
    fun create(name: String,
               dataType: DataType<out Any>,
               isRequired: Boolean = false,
               example: Example? = null) =
      if (example == null) success(ContractParameter(name, dataType, isRequired))
      else {
        dataType
          .validate(example.normalizedValue)
          .forProperty(name)
          .map { ContractParameter(name, dataType, isRequired, example) }
      }
  }
}