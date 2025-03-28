package tech.sabai.contracteer.core.swagger.converter.schema

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.media.*
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.datatype.AnyDataType
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.datatype.Discriminator
import tech.sabai.contracteer.core.swagger.safeMapping
import tech.sabai.contracteer.core.swagger.shortRef
import java.math.BigDecimal

object SchemaConverter {
  private val logger = KotlinLogging.logger {}
  private const val MAX_RECURSIVE_DEPTH = 25
  private var sharedSchemas: Map<String, Schema<*>> = emptyMap()
  private val dataTypeCache: MutableMap<String, DataType<out Any>> = mutableMapOf()
  private val discriminatorCache: MutableMap<String, Discriminator> = mutableMapOf()

  internal fun setSharedSchemas(sharedSchemas: Map<String, Schema<*>>) {
    this.sharedSchemas = sharedSchemas
    dataTypeCache.clear()
    discriminatorCache.clear()
    addDiscriminators(sharedSchemas)
  }

  fun convertToDataType(schema: Schema<*>,
                        defaultName: String,
                        recursiveDepth: Int = MAX_RECURSIVE_DEPTH): Result<DataType<out Any>> {
    val ref = schema.shortRef()
    return when {
      recursiveDepth < 0             -> failure("Maximum recursive depth reached while converting Schema")
      ref == null                    -> convertSchema(schema, defaultName, recursiveDepth)
      dataTypeCache.containsKey(ref) -> success(dataTypeCache[ref]!!).also { logger.debug { "DataType already cached for Schema '${schema.`$ref`}'" } }
      sharedSchemas.containsKey(ref) -> convertSchema(sharedSchemas[ref]!!, ref, recursiveDepth).map { it!!.also { dataTypeCache[ref] = it } }
      else                           -> failure("Schema '${schema.`$ref`}' not found in 'components/schemas' section")
    }
  }

  fun convertToDiscriminator(schema: Schema<*>) =
    if (schema.shortRef() != null) discriminatorCache[schema.shortRef()]
    else schema.discriminator?.let {
      Discriminator(
        it.propertyName,
        it.safeMapping().mapValues { mapping -> mapping.value.replace(Components.COMPONENTS_SCHEMAS_REF, "") }
      )
    }

  private fun convertSchema(schema: Schema<*>, schemaName: String, recursiveDepth: Int): Result<DataType<out Any>> {
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
      is ObjectSchema, is MapSchema              -> ObjectSchemaConverter.convert(schema, recursiveDepth)
      is PasswordSchema                          -> StringSchemaConverter.convert(schema, "string/password")
      is UUIDSchema                              -> UuidSchemaConverter.convert(schema)
      else                                       -> tryToInterfereSchemaType(schema, recursiveDepth)
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun tryToInterfereSchemaType(schema: Schema<*>, recursiveDepth: Int): Result<DataType<out Any>> =
    when {
      schema.properties != null ->
        ObjectSchemaConverter
          .convert(schema, recursiveDepth)
          .also { logger.warn { "Schema '${schema.name}' does not have a 'type' property defined, but defines properties. Considering it as an 'object' schema." } }
      schema.type == "string"   -> StringSchemaConverter.convert(schema as Schema<String>, "string")
      schema.type == "number"   -> NumberSchemaConverter.convert(schema as Schema<BigDecimal>)
      schema.type == "boolean"  -> BooleanSchemaConverter.convert(schema as Schema<Boolean>)
      schema.isAnyType()        -> success(AnyDataType).also { logger.warn { "Schema '${schema.name}' is empty (anyType) and will be interpreted as accepting any type." } }
      else                      -> failure("Error while interpreting schema '${schema.name}'. The schema might be misconfigured or incomplete.")
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

  fun Schema<*>.isAnyType() =
    type == null &&
    properties.isNullOrEmpty() &&
    additionalProperties == null &&
    format == null &&
    title == null &&
    description == null &&
    maximum == null &&
    minimum == null &&
    exclusiveMaximum == null &&
    exclusiveMinimum == null &&
    pattern == null &&
    minLength == null &&
    maxLength == null &&
    multipleOf == null &&
    default == null &&
    example == null &&
    `enum`.isNullOrEmpty()
}