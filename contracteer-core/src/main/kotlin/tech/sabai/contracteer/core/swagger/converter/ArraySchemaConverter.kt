package tech.sabai.contracteer.core.swagger.converter

import io.swagger.v3.oas.models.media.ArraySchema
import tech.sabai.contracteer.core.datatype.ArrayDataType
import tech.sabai.contracteer.core.swagger.safeEnum
import tech.sabai.contracteer.core.swagger.safeNullable

internal object ArraySchemaConverter {

  fun convert(schema: ArraySchema) =
    SchemaConverter.convert(schema.items).let {
      if (it.isSuccess()) ArrayDataType.create(schema.name, it.value!!, schema.safeNullable(), schema.safeEnum())
      else it.retypeError()
    }
}