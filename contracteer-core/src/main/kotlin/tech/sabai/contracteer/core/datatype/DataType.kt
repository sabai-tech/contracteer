package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result

/**
 * Base type for all OpenAPI data types supported by Contracteer.
 *
 * Each subclass corresponds to an OpenAPI schema type (string, integer, object, etc.)
 * and provides validation and random value generation. Instances are created through
 * companion `create` factory methods that return [Result] to report validation errors.
 */
sealed interface DataType<T> {

  /** The schema name (typically from the OpenAPI component name or a generated label). */
  val name: String

  /** The OpenAPI type identifier (e.g. `"string"`, `"integer"`, `"object"`). */
  val openApiType: String

  /** Whether the schema allows null values. */
  val isNullable: Boolean

  /** The JVM class used for runtime type checking. */
  val dataTypeClass: Class<out T>

  /** The `enum` constraint, if declared in the schema. */
  val allowedValues: AllowedValues?

  /**
   * Validates [value] against this data type's constraints.
   *
   * @return a [Result] containing the validated value on success, or validation errors on failure
   */
  fun validate(value: Any?): Result<T?>

  /** Generates a random value conforming to this data type's constraints. */
  fun randomValue(): T?

  /** Returns `true` if this type has a fully defined structure (object, allOf, oneOf, anyOf). */
  fun isFullyStructured(): Boolean

  /** Returns a variant suitable for request validation and generation (readOnly properties excluded). */
  fun asRequestType(): DataType<T>

  /** Returns a variant suitable for response validation and generation (writeOnly properties excluded). */
  fun asResponseType(): DataType<T>
}