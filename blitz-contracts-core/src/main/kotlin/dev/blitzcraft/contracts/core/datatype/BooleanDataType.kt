package dev.blitzcraft.contracts.core.datatype

import dev.blitzcraft.contracts.core.validation.ValidationResult.Companion.success
import kotlin.random.Random

class BooleanDataType(isNullable: Boolean = false):
    DataType<Boolean>(openApiType = "boolean", isNullable, Boolean::class.javaObjectType) {

  override fun doValidate(value: Boolean) = success()

  override fun randomValue() = Random.nextBoolean()
}