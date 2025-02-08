package tech.sabai.contracteer.core.swagger.converter

import io.swagger.v3.oas.models.media.ArraySchema
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.datatype.ArrayDataType
import tech.sabai.contracteer.core.swagger.safeNullable

internal object ArraySchemaConverter {

  fun convert(schema: ArraySchema): Result<ArrayDataType> =
    SchemaConverter.convert(schema.items).let {
      if (it.isSuccess()) success(ArrayDataType(schema.name, it.value!!, schema.safeNullable()))
      else it.retypeError()
    }
}