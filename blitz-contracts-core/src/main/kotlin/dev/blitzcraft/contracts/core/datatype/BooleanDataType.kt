package dev.blitzcraft.contracts.core.datatype

import dev.blitzcraft.contracts.core.Result.Companion.success
import kotlin.random.Random

class BooleanDataType(name: String= "Inline Schema", isNullable: Boolean = false):
    DataType<Boolean>(name, "boolean", isNullable, Boolean::class.javaObjectType) {

  override fun doValidate(value: Boolean) = success(value)

  override fun randomValue() = Random.nextBoolean()
}