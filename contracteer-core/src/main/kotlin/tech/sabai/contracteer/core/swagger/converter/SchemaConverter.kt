package tech.sabai.contracteer.core.swagger.converter

import io.swagger.v3.oas.models.media.*
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.datatype.*
import tech.sabai.contracteer.core.swagger.fullyResolve
import tech.sabai.contracteer.core.swagger.safeNullable

object SchemaConverter {

  fun convert(schema: Schema<*>): Result<DataType<out Any>> {
    return when (val fullyResolved = schema.fullyResolve()) {
      is ComposedSchema  -> ComposedSchemaConverter.convert(fullyResolved)

      is BooleanSchema   -> success(BooleanDataType(fullyResolved.name, fullyResolved.safeNullable()))
      is IntegerSchema   -> success(IntegerDataType(fullyResolved.name, isNullable = fullyResolved.safeNullable()))
      is NumberSchema    -> success(NumberDataType(fullyResolved.name, isNullable = fullyResolved.safeNullable()))
      is StringSchema    -> success(StringDataType(fullyResolved.name, isNullable = fullyResolved.safeNullable()))
      is PasswordSchema  -> success(StringDataType(fullyResolved.name, "string/password", fullyResolved.safeNullable()))
      is BinarySchema    -> success(BinaryDataType(fullyResolved.name, fullyResolved.safeNullable()))
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