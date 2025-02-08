package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result.Companion.success
import kotlin.random.Random

class BooleanDataType(name: String= "Inline 'boolean' Schema", isNullable: Boolean = false):
    DataType<Boolean>(name, "boolean", isNullable, Boolean::class.javaObjectType) {

  override fun doValidate(value: Boolean) = success(value)

  override fun randomValue() = Random.nextBoolean()
}