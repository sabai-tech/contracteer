package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.combineResults

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

  fun validate(dataType: DataType<out Any>): Result<DataType<out Any>> =
    when {
      !dataType.isFullyStructured() -> failure("Discriminator can only be used with 'object', 'anyOf', 'oneOf' or 'allOf' schema")
      dataType is ObjectDataType    -> validateObjectDataType(dataType)
      dataType is AnyOfDataType     -> dataType.subTypes.map { validate(it) }.combineResults().map { dataType }
      dataType is OneOfDataType     -> dataType.subTypes.map { validate(it) }.combineResults().map { dataType }
      dataType is AllOfDataType     -> validateAllOf(dataType)
      else                          -> failure("Discriminator can only be used with 'object', 'anyOf', 'oneOf' or 'allOf' schema")
    }

  private fun validateAllOf(dataType: AllOfDataType): Result<AllOfDataType> {
    val results = dataType.subTypes.map { validate(it) }
    val successes = results.count { it.isSuccess() }
    return when {
      successes == 1 -> success(dataType)
      successes > 1  -> failure("Discriminator property '${propertyName}' is defined in multiple 'allOf' sub schemas")
      else           -> results.combineResults().retypeError()
    }
  }

  private fun validateObjectDataType(dataType: ObjectDataType) =
    when {
      !dataType.requiredProperties.contains(propertyName)  -> failure("Schema '${dataType.name}': discriminator property '$propertyName' is missing")
      dataType.properties[propertyName] !is StringDataType -> failure("Schema '${dataType.name}': discriminator property '$propertyName' must be of type 'string'. Found '${dataType.properties[propertyName]!!.openApiType}'")
      else                                                 -> success(dataType)
    }
}
