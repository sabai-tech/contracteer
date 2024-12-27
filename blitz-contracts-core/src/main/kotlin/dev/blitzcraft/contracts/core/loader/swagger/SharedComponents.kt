package dev.blitzcraft.contracts.core.loader.swagger

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.Components.*
import io.swagger.v3.oas.models.media.Schema

object SharedComponents {
  lateinit var components: Components

  fun fullyResolve(schema: Schema<*>): Schema<*> {
    if (schema.`$ref`.isNullOrEmpty()) return schema.apply { name = "Inline Schema" }

    val resolvedSchema = components.schemas[schema.`$ref`.replace(COMPONENTS_SCHEMAS_REF, "")]
                         ?: throw IllegalStateException("Schema ${schema.`$ref`} not found")

    return resolvedSchema.apply { name = schema.`$ref`.replace(COMPONENTS_SCHEMAS_REF, "") }
  }
}