package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result

abstract class CompositeDataType<T>(
  name: String,
  openApiType: String,
  isNullable: Boolean,
  dataTypeClass: Class<out T>,
  allowedValues: AllowedValues? = null): DataType<T>(name, openApiType, isNullable, dataTypeClass, allowedValues) {

  abstract fun isStructured():Boolean
  abstract fun hasDiscriminatorProperty(name: String): Result<String>
}
