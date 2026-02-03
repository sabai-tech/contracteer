package tech.sabai.contracteer.core.contract

import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.serde.BasicSerde

@ConsistentCopyVisibility
data class ContractParameter private constructor(
  val name: String,
  val dataType: DataType<out Any>,
  val isRequired: Boolean = false,
  val example: Example? = null) {

  fun value(): Any? = if (example != null) example.normalizedValue else dataType.randomValue()

  fun stringValue() = BasicSerde.serialize(value())

  fun deserialize(value: String?) = BasicSerde.deserialize(value, dataType)

  companion object {
    fun create(name: String,
               dataType: DataType<out Any>,
               isRequired: Boolean = false,
               example: Example? = null,
               validateExample: Boolean = true) =
      when {
        example == null  -> success(ContractParameter(name, dataType, isRequired))
        !validateExample -> success(ContractParameter(name, dataType, isRequired, example))
        else             ->
          dataType
            .validate(example.normalizedValue)
            .forProperty(name)
            .map { ContractParameter(name, dataType, isRequired, example) }
      }
  }
}