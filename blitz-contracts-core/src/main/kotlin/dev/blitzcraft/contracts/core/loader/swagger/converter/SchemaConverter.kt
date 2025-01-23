package dev.blitzcraft.contracts.core.loader.swagger.converter

import dev.blitzcraft.contracts.core.Result
import dev.blitzcraft.contracts.core.Result.Companion.failure
import dev.blitzcraft.contracts.core.Result.Companion.success
import dev.blitzcraft.contracts.core.datatype.*
import dev.blitzcraft.contracts.core.loader.swagger.fullyResolve
import dev.blitzcraft.contracts.core.loader.swagger.safeNullable
import io.swagger.v3.oas.models.media.*

object SchemaConverter {

  fun convert(schema: Schema<*>): Result<DataType<out Any>> {
    return when (val fullyResolved = schema.fullyResolve()) {
      is ComposedSchema  -> ComposedSchemaConverter.convert(fullyResolved)

      is BooleanSchema   -> success(BooleanDataType(schema.name, schema.safeNullable()))
      is IntegerSchema   -> success(IntegerDataType(schema.name, isNullable = schema.safeNullable()))
      is NumberSchema    -> success(NumberDataType(schema.name, isNullable = schema.safeNullable()))
      is StringSchema    -> success(StringDataType(schema.name, isNullable = schema.safeNullable()))
      is PasswordSchema  -> success(StringDataType(schema.name, "string/password", schema.safeNullable()))
      is BinarySchema    -> success(StringDataType(schema.name, "string/binary", schema.safeNullable()))
      is UUIDSchema      -> success(UuidDataType(schema.name, schema.safeNullable()))
      is ByteArraySchema -> success(Base64DataType(fullyResolved.name, fullyResolved.safeNullable()))
      is EmailSchema     -> success(EmailDataType(fullyResolved.name, fullyResolved.safeNullable()))
      is DateTimeSchema  -> success(DateTimeDataType(fullyResolved.name, fullyResolved.safeNullable()))
      is DateSchema      -> success(DateDataType(fullyResolved.name, fullyResolved.safeNullable()))
      is ObjectSchema    -> ObjectSchemaConverter.convert(fullyResolved)
      is ArraySchema     -> ArraySchemaConverter.convert(fullyResolved)
      else               -> failure("Schema ${fullyResolved::class.java.simpleName} is not yet supported")
    }
  }
}