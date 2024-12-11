package dev.blitzcraft.contracts.core.datatype

import dev.blitzcraft.contracts.core.validation.ValidationResult
import dev.blitzcraft.contracts.core.validation.ValidationResult.Companion.error
import dev.blitzcraft.contracts.core.validation.ValidationResult.Companion.success

sealed class DataType<T>(
  val openApiType: String,
  val isNullable: Boolean = false,
  val dataTypeClass: Class<out T>) {


  @Suppress("UNCHECKED_CAST")
  internal fun validate(value: Any?) =
    when {
      value == null && isNullable           -> success()
      value == null                         -> error("Cannot be null")
      dataTypeClass.isInstance(value).not() -> error("Wrong type. Expected type: $openApiType")
      else                                  -> doValidate(value as T)
    }

  protected abstract fun doValidate(value: T): ValidationResult

  internal abstract fun randomValue(): T
}
