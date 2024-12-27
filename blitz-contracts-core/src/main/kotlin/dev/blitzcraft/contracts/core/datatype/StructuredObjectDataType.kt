package dev.blitzcraft.contracts.core.datatype

@Suppress("UNCHECKED_CAST")
abstract class StructuredObjectDataType(name: String, openApiType: String, isNullable: Boolean):
    DataType<Map<String, Any?>>(name, openApiType, isNullable, Map::class.java as Class<Map<String, Any?>>)