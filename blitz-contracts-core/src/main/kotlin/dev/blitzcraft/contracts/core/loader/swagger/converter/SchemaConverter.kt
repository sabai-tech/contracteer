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

      is BooleanSchema   -> success(BooleanDataType(fullyResolved.name, fullyResolved.safeNullable()))
      is IntegerSchema   -> success(IntegerDataType(fullyResolved.name, isNullable = fullyResolved.safeNullable()))
      is NumberSchema    -> success(NumberDataType(fullyResolved.name, isNullable = fullyResolved.safeNullable()))
      is StringSchema    -> success(StringDataType(fullyResolved.name, isNullable = fullyResolved.safeNullable()))
      is PasswordSchema  -> success(StringDataType(fullyResolved.name, "string/password", fullyResolved.safeNullable()))
      is BinarySchema    -> success(BinaryDataType(fullyResolved.name,  fullyResolved.safeNullable()))
      is UUIDSchema      -> success(UuidDataType(fullyResolved.name, fullyResolved.safeNullable()))
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