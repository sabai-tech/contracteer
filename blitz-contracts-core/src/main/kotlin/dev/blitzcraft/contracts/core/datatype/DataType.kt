package dev.blitzcraft.contracts.core.datatype

import dev.blitzcraft.contracts.core.Result
import dev.blitzcraft.contracts.core.Result.Companion.failure
import dev.blitzcraft.contracts.core.Result.Companion.success

sealed class DataType<T>(
  val name: String,
  val openApiType: String,
  val isNullable: Boolean = false,
  val dataTypeClass: Class<out T>) {


  @Suppress("UNCHECKED_CAST")
  internal fun validate(value: Any?): Result<T> =
    when {
      value == null && isNullable           -> success()
      value == null                         -> failure("Cannot be null")
      dataTypeClass.isInstance(value).not() -> failure("Wrong type. Expected type: $openApiType")
      else                                  -> doValidate(value as T)
    }

  protected abstract fun doValidate(value: T): Result<T>

  internal abstract fun randomValue(): T
}
