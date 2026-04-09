package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.normalize

/**
 * A fully resolved data type with concrete validation and random value generation.
 *
 * All concrete OpenAPI schema types (StringDataType, ObjectDataType, etc.) extend this class.
 *
 * @param name the schema name (typically from the OpenAPI component name or a generated label)
 * @param openApiType the OpenAPI type identifier (e.g. `"string"`, `"integer"`, `"object"`)
 * @param isNullable whether the schema allows null values
 * @param dataTypeClass the JVM class used for runtime type checking
 * @param allowedValues the `enum` constraint, if declared in the schema
 */
sealed class ResolvedDataType<T>(
  override val name: String,
  override val openApiType: String,
  override val isNullable: Boolean = false,
  override val dataTypeClass: Class<out T>,
  override val allowedValues: AllowedValues? = null): DataType<T> {

  @Suppress("UNCHECKED_CAST")
  override fun validate(value: Any?): Result<T?> {
    val normalizedValue = value.normalize()
    return when {
      normalizedValue == null && isNullable      -> success(null)
      normalizedValue == null                    -> failure("Value cannot be null as the schema is non-nullable")
      !dataTypeClass.isInstance(normalizedValue) -> failure("Type mismatch, expected type '$openApiType'")
      allowedValues != null                      -> allowedValues!!.contains(normalizedValue).map { normalizedValue as T }
      else                                       -> doValidate(normalizedValue as T)
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun randomValue(): T? {
    return if (allowedValues != null) allowedValues!!.randomValue() as T?
    else doRandomValue()
  }

  override fun asRequestType(): DataType<T> = this

  override fun asResponseType(): DataType<T> = this

  protected abstract fun doValidate(value: T): Result<T>
  protected abstract fun doRandomValue(): T?
}