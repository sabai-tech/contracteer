package tech.sabai.contracteer.core.swagger.converter

import io.swagger.v3.oas.models.media.*
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.datatype.*
import tech.sabai.contracteer.core.normalize
import tech.sabai.contracteer.core.swagger.*
import java.math.BigDecimal
import java.time.ZoneId
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
import java.util.*

object SchemaConverter {

  fun convert(schema: Schema<*>): Result<DataType<out Any>> {
    return when (val fullyResolved = schema.fullyResolve()) {
      is ComposedSchema  -> ComposedSchemaConverter.convert(fullyResolved)
      is BooleanSchema   -> createBooleanDataType(fullyResolved)
      is IntegerSchema   -> createIntegerDataType(fullyResolved)
      is NumberSchema    -> createNumberDataType(fullyResolved)
      is StringSchema    -> createStringDataType(fullyResolved, "string")
      is PasswordSchema  -> createStringDataType(fullyResolved, "string/password")
      is BinarySchema    -> createBinaryDataType(fullyResolved)
      is UUIDSchema      -> createUuidDataType(fullyResolved)
      is ByteArraySchema -> createBase64DataType(fullyResolved)
      is EmailSchema     -> createEmailDataType(fullyResolved)
      is DateTimeSchema  -> createDateTimeDataType(fullyResolved)
      is DateSchema      -> createDateDataType(fullyResolved)
      is ObjectSchema    -> createObjectDataType(fullyResolved)
      is ArraySchema     -> createArrayDataType(fullyResolved)
      else               -> failure("Schema ${fullyResolved::class.java.simpleName} is not yet supported")
    }
  }

  private fun createIntegerDataType(schema: IntegerSchema) =
    IntegerDataType.create(
      name = schema.name,
      isNullable = schema.safeNullable(),
      minimum = schema.minimum,
      maximum = schema.maximum,
      exclusiveMinimum = schema.safeExclusiveMinimum(),
      exclusiveMaximum = schema.safeExclusiveMaximum(),
      enum = schema.safeEnum().map { it.normalize() as BigDecimal? })

  private fun createNumberDataType(schema: NumberSchema) =
    NumberDataType.create(
      name = schema.name,
      isNullable = schema.safeNullable(),
      minimum = schema.minimum,
      maximum = schema.maximum,
      exclusiveMinimum = schema.safeExclusiveMinimum(),
      exclusiveMaximum = schema.safeExclusiveMaximum(),
      enum = schema.safeEnum())

  private fun createStringDataType(schema: Schema<String>, openApiType: String) =
    StringDataType.create(
      name = schema.name,
      openApiType,
      isNullable = schema.safeNullable(),
      minLength = schema.minLength,
      maxLength = schema.maxLength,
      enum = schema.safeEnum())

  private fun createBase64DataType(schema: ByteArraySchema) =
    Base64DataType.create(
      name = schema.name,
      isNullable = schema.safeNullable(),
      minLength = schema.minLength,
      maxLength = schema.maxLength,
      enum = schema.safeEnum().map { Base64.getEncoder().encodeToString(it) }
    )

  private fun createBinaryDataType(schema: BinarySchema) =
    BinaryDataType.create(
      name = schema.name,
      isNullable = schema.safeNullable(),
      minLength = schema.minLength,
      maxLength = schema.maxLength,
      enum = schema.safeEnum().map { String(it) }
    )

  private fun createUuidDataType(schema: UUIDSchema) =
    UuidDataType.create(
      name = schema.name,
      isNullable = schema.safeNullable(),
      enum = schema.safeEnum().map { it.toString() }
    )

  private fun createEmailDataType(schema: EmailSchema) =
    EmailDataType.create(
      name = schema.name,
      isNullable = schema.safeNullable(),
      minLength = schema.minLength,
      maxLength = schema.maxLength,
      enum = schema.safeEnum(),
    )

  private fun createDateDataType(schema: DateSchema) =
    DateDataType.create(
      name = schema.name,
      isNullable = schema.safeNullable(),
      enum = schema
        .safeEnum()
        .map { it?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()?.format(ISO_LOCAL_DATE) }
    )

  private fun createDateTimeDataType(schema: DateTimeSchema) =
    DateTimeDataType.create(
      name = schema.name,
      isNullable = schema.safeNullable(),
      enum = schema.safeEnum().map { it?.format(ISO_OFFSET_DATE_TIME) }
    )

  private fun createBooleanDataType(schema: BooleanSchema) =
    BooleanDataType.create(
      name = schema.name,
      isNullable = schema.safeNullable(),
      enum = schema.safeEnum()
    )

  private fun createArrayDataType(schema: ArraySchema) =
    convert(schema.items).flatMap { itemDataType ->
      ArrayDataType.create(
        name = schema.name,
        itemDataType = itemDataType!!,
        isNullable = schema.safeNullable(),
        enum = schema.safeEnum().map { it.normalize() }
      )
    }

  private fun createObjectDataType(schema: ObjectSchema): Result<ObjectDataType> {
    val propertyDataTypeResults = schema.properties.mapValues { convert(it.value) }
    return propertyDataTypeResults.values
      .combineResults()
      .flatMap {
        ObjectDataType.create(
          name = schema.name,
          properties = propertyDataTypeResults.mapValues { it.value.value!! },
          requiredProperties = schema.required?.toSet() ?: emptySet(),
          isNullable = schema.safeNullable(),
          enum = schema.safeEnum().map { it.normalize() }
        )
      }
  }
}
