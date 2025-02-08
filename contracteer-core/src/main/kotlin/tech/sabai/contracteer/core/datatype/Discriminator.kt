package tech.sabai.contracteer.core.datatype

data class Discriminator(
  val propertyName: String,
  val mapping: Map<String, ObjectDataType> = mapOf()
)
