package tech.sabai.contracteer.core.swagger.converter

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.models.Components.COMPONENTS_SCHEMAS_REF
import io.swagger.v3.oas.models.media.*
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.datatype.Discriminator
import tech.sabai.contracteer.core.swagger.safeMapping
import tech.sabai.contracteer.core.swagger.shortRef

private val logger = KotlinLogging.logger {}

private const val RECURSIVE_MAX_DEPTH = 10

object SchemaConverter {
  private var sharedSchemas: Map<String, Schema<*>> = emptyMap()
  private val dataTypeCache: MutableMap<String, DataType<out Any>> = mutableMapOf()
  private val discriminatorCache: MutableMap<String, Discriminator> = mutableMapOf()

  fun setSharedSchemas(schemas: Map<String, Schema<*>>) {
    this.sharedSchemas = schemas
    dataTypeCache.clear()
    discriminatorCache.clear()
    addDiscriminators(schemas)
  }

  fun convertToDataType(schema: Schema<*>,
                        defaultName: String = "Inline Schema",
                        recursiveDepth: Int = RECURSIVE_MAX_DEPTH): Result<DataType<out Any>> {
    val ref = schema.shortRef()
    return when {
      recursiveDepth < 0             ->
        failure("Max recursive depth reached")

      ref == null                    ->
        convertSchema(schema, defaultName, recursiveDepth)

      dataTypeCache.containsKey(ref) ->
        success(dataTypeCache[ref]!!).also { logger.debug { "DataType already cached for Schema '${schema.`$ref`}'" } }

      sharedSchemas.containsKey(ref) ->
        convertSchema(sharedSchemas[ref]!!, ref, recursiveDepth).map { it!!.also { dataTypeCache[ref] = it } }

      else                           ->
        failure("Schema ${schema.`$ref`} not found")
    }
  }

  fun convertToDiscriminator(schema: Schema<*>) =
    if (schema.shortRef() != null) discriminatorCache[schema.shortRef()]
    else schema.discriminator?.let {
      Discriminator(
        it.propertyName,
        it.safeMapping().mapValues { mapping -> mapping.value.replace(COMPONENTS_SCHEMAS_REF, "") }
      )
    }


  private fun convertSchema(schema: Schema<*>,
                            schemaName: String,
                            recursiveDepth: Int): Result<DataType<out Any>> {
    schema.name = schemaName
    logger.debug { "Creating Datatype for Schema '${schema.name}' with max recursive depth $recursiveDepth" }
    return when (schema) {
      is ArraySchema                             -> ArraySchemaConverter.convert(schema, recursiveDepth)
      is BinarySchema                            -> BinarySchemaConverter.convert(schema)
      is ByteArraySchema                         -> Base64SchemaConverter.convert(schema)
      is BooleanSchema                           -> BooleanSchemaConverter.convert(schema)
      is ComposedSchema if(schema.allOf != null) -> AllOfSchemaConverter.convert(schema, recursiveDepth)
      is ComposedSchema if(schema.anyOf != null) -> AnyOfSchemaConverter.convert(schema, recursiveDepth)
      is ComposedSchema if(schema.oneOf != null) -> OneOfSchemaConverter.convert(schema, recursiveDepth)
      is DateSchema                              -> DateSchemaConverter.convert(schema)
      is DateTimeSchema                          -> DateTimeSchemaConverter.convert(schema)
      is EmailSchema                             -> EmailSchemaConverter.convert(schema)
      is IntegerSchema                           -> IntegerSchemaConverter.convert(schema)
      is NumberSchema                            -> NumberSchemaConverter.convert(schema)
      is StringSchema                            -> StringSchemaConverter.convert(schema, "string")
      is ObjectSchema                            -> ObjectSchemaConverter.convert(schema, recursiveDepth)
      is PasswordSchema                          -> StringSchemaConverter.convert(schema, "string/password")
      is UUIDSchema                              -> UuidSchemaConverter.convert(schema)
      else                                       -> failure("Schema ${schema::class.java.simpleName} is not yet supported")
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun addDiscriminators(schemas: Map<String, Schema<*>>) {
    dataTypeCache.clear()
    discriminatorCache.putAll(
      schemas
        .map { (name, schema) -> name to convertToDiscriminator(schema) }
        .filter { it.second != null }
        .toMap() as Map<String, Discriminator>
    )
  }
}