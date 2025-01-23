package dev.blitzcraft.contracts.core.loader.swagger.converter

import dev.blitzcraft.contracts.core.Result
import dev.blitzcraft.contracts.core.Result.Companion.success
import dev.blitzcraft.contracts.core.datatype.ArrayDataType
import dev.blitzcraft.contracts.core.loader.swagger.safeNullable
import io.swagger.v3.oas.models.media.ArraySchema

internal object ArraySchemaConverter {

  fun convert(schema: ArraySchema): Result<ArrayDataType> =
    SchemaConverter.convert(schema.items).let {
      if (it.isSuccess()) success(ArrayDataType(schema.name, it.value!!, schema.safeNullable()))
      else it.retypeError()
    }
}