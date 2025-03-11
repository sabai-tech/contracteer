package tech.sabai.contracteer.core.datatype

typealias MappingName = String
typealias DataTypeName = String

data class Discriminator(
  val propertyName: String,
  private val mappings: Map<MappingName, DataTypeName> = mapOf()) {

  fun dataTypeNames() =
    mappings.values

  fun getDataTypeNameFor(mappingName: MappingName) =
    mappings[mappingName] ?: mappingName

  fun getMappingName(dataTypeName: DataTypeName) =
    mappings.filterValues { it == dataTypeName }.keys.firstOrNull() ?: dataTypeName
}
