package dev.blitzcraft.contracts.core.datatype

import dev.blitzcraft.contracts.core.Result.Companion.failure
import dev.blitzcraft.contracts.core.Result.Companion.success

class EmailDataType(name: String= "Inline 'string/email' Schema", isNullable: Boolean = false):
    DataType<String>(name, "string/email", isNullable, String::class.java) {

  private val loremIpsum =
    "Lorem ipsum dolor sit amet consectetur adipiscing elit sed do eiusmod tempor incididunt ut labore et dolore magna aliqua"

  private val emailRegex = (
      "^(?:[a-zA-Z0-9!#\$%&'*+/=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#\$%&'*+/=?^_`{|}~-]+)*|" +
      "\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|" +
      "\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@" +
      "(?:(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}|" +
      "\\[(?:(?:25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2})\\.){3}" +
      "(?:25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2}|[a-zA-Z0-9-]*[a-zA-Z0-9]:" +
      "(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|" +
      "\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+))\$"
                           ).toRegex()

  override fun doValidate(value: String) =
     if (emailRegex.matches(value)) success(value) else failure("not a valid email")

  override fun randomValue(): String {
    val words = loremIpsum.split(" ")
    val randomUser = List(2) { words.random() }.joinToString(".")
    val randomDomain = List(2) { words.random() }.joinToString(".")
    return "$randomUser@$randomDomain"
  }
}