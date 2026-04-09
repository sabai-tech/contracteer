package tech.sabai.contracteer.core.datatype

/**
 * Base type for composite schemas (`allOf`, `anyOf`, `oneOf`) that combine multiple [subTypes].
 */
abstract class CompositeDataType<T>(
  name: String,
  openApiType: String,
  isNullable: Boolean,
  val subTypes: List<DataType<out T>>,
  dataTypeClass: Class<out T>,
  allowedValues: AllowedValues? = null): ResolvedDataType<T>(name, openApiType, isNullable, dataTypeClass, allowedValues)
