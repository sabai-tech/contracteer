package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.datatype.GenerationOutcome.Value
import kotlin.random.Random

/** OpenAPI `boolean` type. */
class BooleanDataType private constructor(name: String, isNullable: Boolean, allowedValues: AllowedValues? = null):
    ResolvedDataType<Boolean>(name, "boolean", isNullable, Boolean::class.javaObjectType, allowedValues) {

  override fun doValidate(value: Boolean) = success(value)

  override fun doRandomValue(ctx: GenerationContext): GenerationOutcome<Boolean> = Value(Random.nextBoolean())

  companion object {
    @JvmStatic
    @JvmOverloads
    fun create(
      name: String,
      isNullable: Boolean = false,
      enum: List<Boolean?> = emptyList()
    ) =
      BooleanDataType(name, isNullable).let { dataType ->
        if (enum.isEmpty()) success(dataType)
        else AllowedValues.create(enum, dataType).map { BooleanDataType(name, isNullable, it) }
      }
  }
}