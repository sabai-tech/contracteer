package dev.blitzcraft.contracts.core.datatype

import net.datafaker.Faker

class StringDataType: DataType<String> {
  override fun nextValue(): String {
    return Faker().lorem().maxLengthSentence(20)
  }

  override fun regexPattern() = "[\\S\\s]+" // ".*" does not work with JsonPath as it always matches
}