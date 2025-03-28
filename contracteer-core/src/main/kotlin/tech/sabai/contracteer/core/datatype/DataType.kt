package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.normalize

sealed class DataType<T>(
  val name: String,
  val openApiType: String,
  val isNullable: Boolean = false,
  val dataTypeClass: Class<out T>,
  val allowedValues: AllowedValues? = null) {

  @Suppress("UNCHECKED_CAST")
  internal fun validate(value: Any?): Result<T> {
    val normalizedValue = value.normalize()
    return when {
      normalizedValue == null && isNullable      -> success()
      normalizedValue == null                    -> failure("Value cannot be null as the schema is non-nullable")
      !dataTypeClass.isInstance(normalizedValue) -> failure("Type mismatch, expected type '$openApiType'")
      allowedValues != null                      -> allowedValues.contains(normalizedValue).map { normalizedValue as T }
      else                                       -> doValidate(normalizedValue as T)
    }
  }

  @Suppress("UNCHECKED_CAST")
  fun randomValue(): T = allowedValues?.randomValue() as T ?: doRandomValue()

  abstract fun isFullyStructured():Boolean
  protected abstract fun doValidate(value: T): Result<T>
  protected abstract fun doRandomValue(): T
}