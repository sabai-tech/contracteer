package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result.Companion.success
import kotlin.random.Random

class StringDataType(
  name: String= "Inline 'string' Schema",
  openApiType: String = "string",
  isNullable: Boolean = false): DataType<String>(name, openApiType, isNullable, String::class.java) {

  private val loremIpsum =
    "Lorem ipsum dolor sit amet consectetur adipiscing elit sed do eiusmod tempor incididunt ut labore et dolore magna aliqua"

  override fun doValidate(value: String) = success(value)

  override fun randomValue(): String =
    loremIpsum.split(" ").shuffled().take(Random.nextInt(2, 7)).joinToString(separator = " ")
}