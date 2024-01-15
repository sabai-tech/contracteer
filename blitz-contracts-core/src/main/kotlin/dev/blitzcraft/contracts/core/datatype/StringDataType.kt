package dev.blitzcraft.contracts.core.datatype

import dev.blitzcraft.contracts.core.validation.ValidationResult.Companion.success
import kotlin.random.Random

class StringDataType(isNullable: Boolean = false):
    DataType<String>("string", isNullable, String::class.java) {

  private val loremIpsum =
    "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."

  override fun doValidate(value: String) = success()

  override fun randomValue(): String =
    loremIpsum.split(" ").shuffled().take(Random.nextInt(2, 7)).joinToString(separator = " ")
}