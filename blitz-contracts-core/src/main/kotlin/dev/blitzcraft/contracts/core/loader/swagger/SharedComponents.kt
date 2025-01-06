package dev.blitzcraft.contracts.core.loader.swagger

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.Components.*
import io.swagger.v3.oas.models.media.Schema

object SharedComponents {
  lateinit var components: Components

  fun findSchema(schemaName: String): Schema<*> {
    val shortSchemaName = schemaName.replace(COMPONENTS_SCHEMAS_REF, "")
    val resolvedSchema = components.schemas[shortSchemaName] ?: throw IllegalStateException("Schema $schemaName not found")

    return resolvedSchema.apply { name = shortSchemaName }
  }

}