package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.datatype.GenerationOutcome.Value

/** A data type that accepts any value. Used for empty or untyped OpenAPI schemas. */
object AnyDataType: ResolvedDataType<Any>("any type", "any type", false, Any::class.java, null) {

  private const val SAMPLE_VALUE = "RANDOM VALUE FOR ANY TYPE SCHEMA"

  override fun doValidate(value: Any): Result<Any> = success(value)

  override fun doRandomValue(ctx: GenerationContext): GenerationOutcome<Any> = Value(SAMPLE_VALUE)
}