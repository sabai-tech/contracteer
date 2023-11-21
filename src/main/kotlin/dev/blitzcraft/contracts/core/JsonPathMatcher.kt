package dev.blitzcraft.contracts.core

import dev.blitzcraft.contracts.core.datatype.ArrayDataType
import dev.blitzcraft.contracts.core.datatype.DataType
import dev.blitzcraft.contracts.core.datatype.ObjectDataType

object JsonPathMatcher {

  fun regexMatchers(dataType: DataType<*>) =
    when (dataType) {
      is ObjectDataType -> dataType.properties.flatMap { regexMatcher("$", it.toPair()) }
      is ArrayDataType  -> arrayRegexMatcher("$", dataType.itemDataType)
      else              -> throw IllegalArgumentException("Invalid Json. The root must be an Object or an Array")
    }

  fun exampleMatchers(anObject: Any?) =
    when (anObject) {
      is Map<*,*> -> anObject.flatMap { valueMatcher("$", it.toPair()) }
      is Array<*> -> anObject.flatMap { arrayValueMatcher("$", it) }
      else              -> throw IllegalArgumentException("Invalid Json. The root must be an Object or an Array")
    }


  private fun regexMatcher(path: String, namedProperty: Pair<String, Property>): List<String> =
    when (val dataType = namedProperty.second.dataType) {
      is ObjectDataType -> dataType.properties.flatMap { regexMatcher("$path['${namedProperty.first}']", it.toPair()) }
      is ArrayDataType  -> arrayRegexMatcher("$path['${namedProperty.first}']", dataType.itemDataType)
      else              -> listOf("$path[?(@['${namedProperty.first}'] =~ /${namedProperty.second.dataType.regexPattern()}/)]")
    }

  private fun arrayRegexMatcher(path: String, dataType: DataType<*>): List<String> =
    when (dataType) {
      is ObjectDataType -> dataType.properties.flatMap { regexMatcher("$path[*]", it.toPair()) }
      is ArrayDataType  -> arrayRegexMatcher("$path[*]", dataType.itemDataType)
      else              -> listOf("$path[?(@ =~ /${dataType.regexPattern()}/)]")
    }

  private fun valueMatcher(path: String, nameAndValue: Pair<Any?, Any?>): List<String> =
    when (val value = nameAndValue.second) {
      is Map<*, *> -> value.flatMap { valueMatcher("$path['${nameAndValue.first}']", it.toPair()) }
      is Array<*>  -> value.flatMap { arrayValueMatcher("$path['${nameAndValue.first}']", it) }
      else         -> listOf("$path[?(@['${nameAndValue.first}'] == ${nameAndValue.second})]")
    }

  private fun arrayValueMatcher(path: String, value: Any?): List<String> =
    when (value) {
      is Map<*, *> -> value.flatMap { valueMatcher("$path[*]", it.toPair()) }
      is Array<*>  -> value.flatMap { arrayValueMatcher("$path[*]", it) }
      else         -> listOf("$path[?(@ == $value)]")
    }
}
