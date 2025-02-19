package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success

sealed class DataType<T>(
  val name: String,
  val openApiType: String,
  val isNullable: Boolean = false,
  val dataTypeClass: Class<out T>,
  private val allowedValues: AllowedValues? = null) {

  @Suppress("UNCHECKED_CAST")
  internal fun validate(value: Any?): Result<T> =
    when {
      value == null && isNullable           -> success()
      value == null                         -> failure("Cannot be null")
      dataTypeClass.isInstance(value).not() -> failure("Wrong type. Expected type: $openApiType")
      allowedValues != null                 -> allowedValues.contains(value).map { value as T }
      else                                  -> doValidate(value as T)
    }

  @Suppress("UNCHECKED_CAST")
  internal fun randomValue(): T = allowedValues?.randomValue() as T ?: doRandomValue()

  protected abstract fun doValidate(value: T): Result<T>
  protected abstract fun doRandomValue(): T
}