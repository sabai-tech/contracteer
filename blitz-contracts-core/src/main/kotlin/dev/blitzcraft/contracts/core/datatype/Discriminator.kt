package dev.blitzcraft.contracts.core.datatype

data class Discriminator(
  val propertyName: String,
  val mapping: Map<String, ObjectDataType> = mapOf()
)
