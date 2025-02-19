package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result.Companion.success
import kotlin.random.Random

class StringDataType private constructor(name: String,
                                         openApiType: String,
                                         isNullable: Boolean,
                                         allowedValues: AllowedValues? = null):
    DataType<String>(name, openApiType, isNullable, String::class.java, allowedValues) {

  private val loremIpsum =
    "Lorem ipsum dolor sit amet consectetur adipiscing elit sed do eiusmod tempor incididunt ut labore et dolore magna aliqua"

  override fun doValidate(value: String) = success(value)

  override fun doRandomValue(): String =
    loremIpsum.split(" ").shuffled().take(Random.nextInt(2, 7)).joinToString(separator = " ")

  companion object {
    fun create(
      name: String = "Inline 'string' Schema",
      openApiType: String,
      isNullable: Boolean = false,
      enum: List<Any?> = emptyList()) =
      StringDataType(name, openApiType, isNullable).let { dataType ->
        if (enum.isEmpty()) success(dataType)
        else AllowedValues.create(enum, dataType).map { StringDataType(name, openApiType, isNullable, it) }
      }
  }
}