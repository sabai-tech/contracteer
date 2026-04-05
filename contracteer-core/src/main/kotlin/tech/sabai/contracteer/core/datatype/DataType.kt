package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.normalize

/**
 * Base type for all OpenAPI data types supported by Contracteer.
 *
 * Each subclass corresponds to an OpenAPI schema type (string, integer, object, etc.)
 * and provides validation and random value generation. Instances are created through
 * companion `create` factory methods that return [Result] to report validation errors.
 *
 * @param name the schema name (typically from the OpenAPI component name or a generated label)
 * @param openApiType the OpenAPI type identifier (e.g. `"string"`, `"integer"`, `"object"`)
 * @param isNullable whether the schema allows null values
 * @param dataTypeClass the JVM class used for runtime type checking
 * @param allowedValues the `enum` constraint, if declared in the schema
 */
sealed class DataType<T>(
  val name: String,
  val openApiType: String,
  val isNullable: Boolean = false,
  val dataTypeClass: Class<out T>,
  val allowedValues: AllowedValues? = null) {

  /**
   * Validates [value] against this data type's constraints.
   *
   * @return a [Result] containing the validated value on success, or validation errors on failure
   */
  @Suppress("UNCHECKED_CAST")
  fun validate(value: Any?): Result<T> {
    val normalizedValue = value.normalize()
    return when {
      normalizedValue == null && isNullable      -> success(null as T)
      normalizedValue == null                    -> failure("Value cannot be null as the schema is non-nullable")
      !dataTypeClass.isInstance(normalizedValue) -> failure("Type mismatch, expected type '$openApiType'")
      allowedValues != null                      -> allowedValues.contains(normalizedValue).map { normalizedValue as T }
      else                                       -> doValidate(normalizedValue as T)
    }
  }

  /** Generates a random value conforming to this data type's constraints. */
  @Suppress("UNCHECKED_CAST")
  fun randomValue(): T = allowedValues?.randomValue() as T ?: doRandomValue()

  /** Returns `true` if this type has a fully defined structure (object, allOf, oneOf, anyOf). */
  abstract fun isFullyStructured(): Boolean

  /** Returns a variant suitable for request validation and generation (readOnly properties excluded). */
  open fun asRequestType(): DataType<T> = this

  /** Returns a variant suitable for response validation and generation (writeOnly properties excluded). */
  open fun asResponseType(): DataType<T> = this

  protected abstract fun doValidate(value: T): Result<T>
  protected abstract fun doRandomValue(): T
}