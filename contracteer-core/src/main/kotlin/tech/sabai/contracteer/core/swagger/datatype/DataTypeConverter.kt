package tech.sabai.contracteer.core.swagger.datatype

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.media.*
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.datatype.AllOfDataType
import tech.sabai.contracteer.core.datatype.AnyDataType
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.datatype.Discriminator
import tech.sabai.contracteer.core.swagger.SharedComponents
import tech.sabai.contracteer.core.swagger.safeMapping
import tech.sabai.contracteer.core.swagger.shortRef
import java.math.BigDecimal

internal class DataTypeConverter(private val sharedComponents: SharedComponents) {

  private val logger = KotlinLogging.logger {}
  private val dataTypeCache: MutableMap<String, DataType<out Any>> = mutableMapOf()
  private val discriminatorCache: MutableMap<String, Discriminator> = mutableMapOf()

  init {
    addDiscriminators(sharedComponents.schemas)
  }

  fun convertToDataType(schema: Schema<*>,
                        defaultName: String,
                        recursiveDepth: Int = MAX_RECURSIVE_DEPTH): Result<DataType<out Any>> {
    val ref = schema.shortRef()
    return when {
      recursiveDepth < 0             -> failure("Maximum recursive depth reached while converting Schema")
      ref == null                    -> convertSchema(schema, defaultName, recursiveDepth)
      dataTypeCache.containsKey(ref) -> success(dataTypeCache[ref]!!).also { logger.debug { "DataType already cached for Schema '${schema.`$ref`}'" } }
      else                           -> sharedComponents
        .resolve(schema)
        .flatMap { convertSchema(it!!, ref, recursiveDepth).map { dt -> dt!!.also { dataTypeCache[ref] = it } } }
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
    val convert = { s: Schema<*>, name: String, depth: Int -> convertToDataType(s, name, depth) }
    val discriminator = { s: Schema<*> -> convertToDiscriminator(s) }
    return when (schema) {
      is ArraySchema                -> ArrayDataTypeConverter.convert(schema, recursiveDepth, convert)
      is BinarySchema               -> BinaryDataTypeConverter.convert(schema)
      is ByteArraySchema            -> Base64DataTypeConverter.convert(schema)
      is BooleanSchema              -> BooleanDataTypeConverter.convert(schema)
      is ComposedSchema             -> convertComposedSchema(schema, recursiveDepth, convert, discriminator)
      is DateSchema                 -> DateDataTypeConverter.convert(schema)
      is DateTimeSchema             -> DateTimeDataTypeConverter.convert(schema)
      is EmailSchema                -> EmailDataTypeConverter.convert(schema)
      is IntegerSchema              -> IntegerDataTypeConverter.convert(schema)
      is NumberSchema               -> NumberDataTypeConverter.convert(schema)
      is StringSchema               -> StringDataTypeConverter.convert(schema, "string")
      is ObjectSchema, is MapSchema -> ObjectDataTypeConverter.convert(schema, recursiveDepth, convert)
      is PasswordSchema             -> StringDataTypeConverter.convert(schema, "string/password")
      is UUIDSchema                 -> UuidDataTypeConverter.convert(schema)
      else                          -> tryToInferSchemaType(schema, recursiveDepth, convert)
    }
  }

  private fun convertComposedSchema(schema: ComposedSchema,
                                    recursiveDepth: Int,
                                    convert: (Schema<*>, String, Int) -> Result<DataType<out Any>>,
                                    discriminator: (Schema<*>) -> Discriminator?): Result<DataType<out Any>> {
    val keywords = listOfNotNull(
      if (schema.allOf != null) "allOf" else null,
      if (schema.anyOf != null) "anyOf" else null,
      if (schema.oneOf != null) "oneOf" else null,
    )

    if (keywords.size > 1)
      return failure("Schema '${schema.name}' combines multiple composition keywords (${keywords.joinToString(", ")}). Only one of 'allOf', 'anyOf', or 'oneOf' per schema is supported.")

    val compositionResult = when {
      schema.allOf != null -> AllOfDataTypeConverter.convert(schema, recursiveDepth, convert, discriminator)
      schema.anyOf != null -> AnyOfDataTypeConverter.convert(schema, recursiveDepth, convert, discriminator)
      schema.oneOf != null -> OneOfDataTypeConverter.convert(schema, recursiveDepth, convert, discriminator)
      else                 -> return failure("Schema '${schema.name}' is a composed schema but has no 'allOf', 'anyOf', or 'oneOf' defined.")
    }

    val siblingResult = ObjectDataTypeConverter.convertSiblingObject(schema, recursiveDepth, convert)
        ?: return compositionResult

    if (schema.allOf != null) return compositionResult

    if (compositionResult.isFailure() || siblingResult.isFailure())
      return compositionResult combineWith siblingResult.retypeError()

    return AllOfDataType.create(
      name = schema.name,
      subTypes = listOf(compositionResult.value!!, siblingResult.value!!))
  }

  @Suppress("UNCHECKED_CAST")
  private fun tryToInferSchemaType(
    schema: Schema<*>,
    recursiveDepth: Int,
    convert: (Schema<*>, String, Int) -> Result<DataType<out Any>>
  ): Result<DataType<out Any>> =
    when {
      schema.properties != null ->
        ObjectDataTypeConverter
          .convert(schema, recursiveDepth, convert)
          .also { logger.warn { "Schema '${schema.name}' does not have a 'type' property defined, but defines properties. Considering it as an 'object' schema." } }
      schema.type == "string"   -> StringDataTypeConverter.convert(schema as Schema<String>, "string")
      schema.type == "number"   -> NumberDataTypeConverter.convert(schema as Schema<BigDecimal>)
      schema.type == "boolean"  -> BooleanDataTypeConverter.convert(schema as Schema<Boolean>)
      schema.isAnyType()        -> success(AnyDataType).also { logger.warn { "Schema '${schema.name}' is empty (anyType) and will be interpreted as accepting any type." } }
      else                      -> failure("Error while interpreting schema '${schema.name}'. The schema might be misconfigured or incomplete.")
    }

  @Suppress("UNCHECKED_CAST")
  private fun addDiscriminators(schemas: Map<String, Schema<*>>) {
    discriminatorCache.putAll(
      schemas
        .map { (name, schema) -> name to convertToDiscriminator(schema) }
        .filter { it.second != null }
        .toMap() as Map<String, Discriminator>
    )
  }

  companion object {
    private const val MAX_RECURSIVE_DEPTH = 25
  }
}

internal fun Schema<*>.isAnyType() =
  type == null &&
  properties.isNullOrEmpty() &&
  additionalProperties == null &&
  format == null &&
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
