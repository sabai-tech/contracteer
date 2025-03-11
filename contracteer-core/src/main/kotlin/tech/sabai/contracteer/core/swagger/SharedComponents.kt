package tech.sabai.contracteer.core.swagger

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.Components.COMPONENTS_SCHEMAS_REF
import io.swagger.v3.oas.models.media.Schema
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.success

object SharedComponents {
  var components: Components? = null

  fun findSchema(schemaName: String): Result<Schema<*>> {
    val shortSchemaName = schemaName.replace(COMPONENTS_SCHEMAS_REF, "")
    val resolvedSchema = components?.schemas?.get(shortSchemaName)

    return if (resolvedSchema != null) success(resolvedSchema.apply { name = shortSchemaName })
    else error("Schema '$schemaName' not found")
  }
}