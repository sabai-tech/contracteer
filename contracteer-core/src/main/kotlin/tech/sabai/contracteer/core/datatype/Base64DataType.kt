package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import java.util.*

class Base64DataType(name: String = "Inline 'string/byte' Schema", isNullable: Boolean = false):
    DataType<String>(name, "string/byte", isNullable, String::class.java) {

  private val loremIpsum =
    "Lorem ipsum dolor sit amet consectetur adipiscing elit sed do eiusmod tempor incididunt ut labore et dolore magna aliqua"


  override fun doValidate(value: String) =
    try {
      Base64.getDecoder().decode(value)
      success(value)
    } catch (e: IllegalArgumentException) {
      failure("not a valid Base64 encoded string")
    }

  override fun randomValue() =
    Base64.getEncoder().encodeToString(loremIpsum.split(" ").random().toByteArray())

}