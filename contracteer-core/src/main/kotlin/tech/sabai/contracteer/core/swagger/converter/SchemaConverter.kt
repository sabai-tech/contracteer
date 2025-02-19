package tech.sabai.contracteer.core.swagger.converter

import io.swagger.v3.oas.models.media.*
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.datatype.*
import tech.sabai.contracteer.core.swagger.fullyResolve
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.safeNullable

object SchemaConverter {

  fun convert(schema: Schema<*>): Result<DataType<out Any>> {
    return when (val fullyResolved = schema.fullyResolve()) {
      is ComposedSchema  -> ComposedSchemaConverter.convert(fullyResolved)
      is BooleanSchema   -> BooleanDataType.create(fullyResolved.name, fullyResolved.safeNullable(),fullyResolved.safeEnum())
      is IntegerSchema   -> IntegerDataType.create(fullyResolved.name, isNullable = fullyResolved.safeNullable(), fullyResolved.safeEnum())
      is NumberSchema    -> NumberDataType.create(fullyResolved.name, isNullable = fullyResolved.safeNullable(), fullyResolved.safeEnum())
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
}