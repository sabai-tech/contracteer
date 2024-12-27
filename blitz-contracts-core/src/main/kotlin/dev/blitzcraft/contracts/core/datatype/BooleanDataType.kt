package dev.blitzcraft.contracts.core.datatype

import dev.blitzcraft.contracts.core.validation.ValidationResult.Companion.success
import kotlin.random.Random

class BooleanDataType(name: String= "Inline Schema", isNullable: Boolean = false):
    DataType<Boolean>(name, "boolean", isNullable, Boolean::class.javaObjectType) {

  override fun doValidate(value: Boolean) = success()

  override fun randomValue() = Random.nextBoolean()
}