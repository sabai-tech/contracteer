package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.datatype.GenerationOutcome.Value
import tech.sabai.contracteer.core.normalize

/**
 * A fully resolved data type with concrete validation and random value generation.
 *
 * All concrete OpenAPI schema types (StringDataType, ObjectDataType, etc.) extend this class.
 *
 * Random value generation is dispatched in two layers:
 * - This class consumes the [GenerationContext] node budget around every [doRandomValue] call
 *   and short-circuits to a [GenerationOutcome.Value] when [allowedValues] is set (enum picks
 *   are terminal and bypass budget accounting).
 * - Subclasses implement [doRandomValue] to produce a value for their specific type.
 *   Primitives wrap their output in [GenerationOutcome.Value] without consulting [ctx];
 *   containers thread [ctx] into their children so budget accounting and cycle detection
 *   continue to apply.
 *
 * @param name the schema name (typically from the OpenAPI component name or a generated label)
 * @param openApiType the OpenAPI type identifier (e.g. `"string"`, `"integer"`, `"object"`)
 * @param isNullable whether the schema allows null values
 * @param dataTypeClass the JVM class used for runtime type checking
 * @param allowedValues the `enum` constraint, if declared in the schema
 */
sealed class ResolvedDataType<T : Any>(
  override val name: String,
  override val openApiType: String,
  override val isNullable: Boolean = false,
  override val dataTypeClass: Class<out T>,
  override val allowedValues: AllowedValues? = null): DataType<T> {

  override fun validate(value: Any?): Result<T?> {
    val normalizedValue = value.normalize()
    return when {
      normalizedValue == null && isNullable      -> success(null)
      normalizedValue == null                    -> failure("Value cannot be null as the schema is non-nullable")
      !dataTypeClass.isInstance(normalizedValue) -> failure("Type mismatch, expected type '$openApiType'")
      allowedValues != null                      -> allowedValues!!.contains(normalizedValue).map { typed(normalizedValue) }
      else                                       -> doValidate(typed(normalizedValue))
    }
  }

  override fun randomValue(ctx: GenerationContext): GenerationOutcome<T> =
    allowedValues?.let { fromAllowedValues(it) }
    ?: ctx.budget.step { doRandomValue(ctx) }

  override fun asRequestType(): DataType<T> = this

  override fun asResponseType(): DataType<T> = this

  protected abstract fun doValidate(value: T): Result<T>
  protected abstract fun doRandomValue(ctx: GenerationContext): GenerationOutcome<T>

  // Cast is safe: `dataTypeClass.isInstance(value)` was checked by the caller in `validate`.
  @Suppress("UNCHECKED_CAST")
  private fun typed(value: Any): T = value as T

  // Cast is safe: AllowedValues entries are validated against this type at creation.
  @Suppress("UNCHECKED_CAST")
  private fun fromAllowedValues(allowedValues: AllowedValues): GenerationOutcome<T> =
    Value(allowedValues.randomValue() as T?)
}
