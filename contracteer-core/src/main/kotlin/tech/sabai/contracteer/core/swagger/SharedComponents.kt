package tech.sabai.contracteer.core.swagger

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.Components.COMPONENTS_SCHEMAS_REF
import io.swagger.v3.oas.models.media.Schema

object SharedComponents {
  var components: Components? = null

  fun findSchema(schemaName: String): Schema<*> {
    val shortSchemaName = schemaName.replace(COMPONENTS_SCHEMAS_REF, "")
    val resolvedSchema = components?.schemas?.get(shortSchemaName) ?: throw IllegalStateException("Schema $schemaName not found")

    return resolvedSchema.apply { name = shortSchemaName }
  }
}