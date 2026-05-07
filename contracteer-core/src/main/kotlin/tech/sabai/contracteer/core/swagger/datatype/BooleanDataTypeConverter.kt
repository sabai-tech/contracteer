package tech.sabai.contracteer.core.swagger.datatype

import io.swagger.v3.oas.models.media.Schema
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.datatype.BooleanDataType
import tech.sabai.contracteer.core.swagger.isNullable
import tech.sabai.contracteer.core.swagger.mapEnum

internal object BooleanDataTypeConverter {
  fun convert(schema: Schema<*>) =
    schema
      .mapEnum {
        when (it) {
          is Boolean -> success(it)
          else       -> failure("Schema '${schema.name}': enum value '$it' is not a valid boolean")
        }
      }.flatMap { enum ->
        BooleanDataType.create(
          name = schema.name,
          isNullable = schema.isNullable(),
          enum = enum
        )
      }
}