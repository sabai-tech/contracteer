package tech.sabai.contracteer.core.swagger.converter

import io.swagger.v3.oas.models.media.*
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.datatype.*
import tech.sabai.contracteer.core.swagger.*
import tech.sabai.contracteer.core.swagger.fullyResolve
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.safeExclusiveMinimum
import tech.sabai.contracteer.core.swagger.safeNullable

object SchemaConverter {

  fun convert(schema: Schema<*>): Result<DataType<out Any>> {
    return when (val fullyResolved = schema.fullyResolve()) {
      is ComposedSchema  -> ComposedSchemaConverter.convert(fullyResolved)
      is BooleanSchema   -> BooleanDataType.create(fullyResolved.name, fullyResolved.safeNullable(),fullyResolved.safeEnum())
      is IntegerSchema   -> createIntegerDataType(fullyResolved)
      is NumberSchema    -> createNumberDataType(fullyResolved)
      is StringSchema    -> StringDataType.create(fullyResolved.name, "string", isNullable = fullyResolved.safeNullable(), fullyResolved.safeEnum())
      is PasswordSchema  -> StringDataType.create(fullyResolved.name, "string/password", fullyResolved.safeNullable(), fullyResolved.safeEnum())
      is BinarySchema    -> BinaryDataType.create(fullyResolved.name, fullyResolved.safeNullable(), fullyResolved.safeEnum())
      is UUIDSchema      -> UuidDataType.create(fullyResolved.name, fullyResolved.safeNullable(), fullyResolved.safeEnum())
      is ByteArraySchema -> Base64DataType.create(fullyResolved.name, fullyResolved.safeNullable(), fullyResolved.safeEnum())
      is EmailSchema     -> EmailDataType.create(fullyResolved.name, fullyResolved.safeNullable(), fullyResolved.safeEnum())
      is DateTimeSchema  -> DateTimeDataType.create(fullyResolved.name, fullyResolved.safeNullable(), fullyResolved.safeEnum())
      is DateSchema      -> DateDataType.create(fullyResolved.name, fullyResolved.safeNullable(), fullyResolved.safeEnum())
      is ObjectSchema    -> ObjectSchemaConverter.convert(fullyResolved)
      is ArraySchema     -> ArraySchemaConverter.convert(fullyResolved)
      else               -> failure("Schema ${fullyResolved::class.java.simpleName} is not yet supported")
    }
  }

  private fun createIntegerDataType(schema: Schema<out Any>) =
    IntegerDataType.create(
      name = schema.name,
      isNullable = schema.safeNullable(),
      minimum = schema.minimum,
      maximum = schema.maximum,
      exclusiveMinimum = schema.safeExclusiveMinimum(),
      exclusiveMaximum = schema.safeExclusiveMaximum(),
      enum = schema.safeEnum())

  private fun createNumberDataType(schema: Schema<out Any>) =
    NumberDataType.create(
      name = schema.name,
      isNullable = schema.safeNullable(),
      minimum = schema.minimum,
      maximum = schema.maximum,
      exclusiveMinimum = schema.safeExclusiveMinimum(),
      exclusiveMaximum = schema.safeExclusiveMaximum(),
      enum = schema.safeEnum())
}