package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.success

object AnyDataType: DataType<Any>("any type", "any type", false, Any::class.java, null) {

  override fun isFullyStructured() = false

  override fun doValidate(value: Any): Result<Any> = success(value)

  override fun doRandomValue(): Any = "RANDOM VALUE FOR ANY TYPE SCHEMA"
}