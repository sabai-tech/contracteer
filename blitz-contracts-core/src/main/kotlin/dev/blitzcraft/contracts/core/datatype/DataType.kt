package dev.blitzcraft.contracts.core.datatype

import dev.blitzcraft.contracts.core.validation.ValidationResult
import dev.blitzcraft.contracts.core.validation.ValidationResult.Companion.success
import dev.blitzcraft.contracts.core.validation.ValidationResult.Companion.error
import io.swagger.v3.oas.models.media.*

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

class UnsupportedSchemeException(schema: Schema<*>): Exception("Schema ${schema::class.java} is not supported")

fun Schema<*>.toDataType(): DataType<*> =
  when (this) {
    is BooleanSchema -> BooleanDataType(isNullable = safeNullable())
    is IntegerSchema -> IntegerDataType(isNullable = safeNullable())
    is NumberSchema  -> DecimalDataType(isNullable = safeNullable())
    is StringSchema  -> StringDataType(isNullable = safeNullable())
    is ObjectSchema  -> ObjectDataType(properties = properties.mapValues { it.value.toDataType() },
                                       requiredProperties = required?.toSet() ?: emptySet(),
                                       isNullable = safeNullable())
    is ArraySchema   -> ArrayDataType(itemDataType = items.toDataType(),
                                      isNullable = safeNullable())
    else             -> throw UnsupportedSchemeException(this)
  }

fun Schema<*>.safeNullable() = nullable ?: false




