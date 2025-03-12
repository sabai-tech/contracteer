package tech.sabai.contracteer.core.datatype

abstract class CompositeDataType<T>(
  name: String,
  openApiType: String,
  isNullable: Boolean,
  val subTypes: List<DataType<out T>>,
  dataTypeClass: Class<out T>,
  allowedValues: AllowedValues? = null): DataType<T>(name, openApiType, isNullable, dataTypeClass, allowedValues)
