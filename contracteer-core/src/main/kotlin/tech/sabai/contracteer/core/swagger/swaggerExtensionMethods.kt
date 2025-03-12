package tech.sabai.contracteer.core.swagger

import io.swagger.v3.oas.models.Components.COMPONENTS_SCHEMAS_REF
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.headers.Header
import io.swagger.v3.oas.models.media.*
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.responses.ApiResponse
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.contract.*
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.datatype.Discriminator
import tech.sabai.contracteer.core.swagger.converter.*


internal fun MediaType.safeExamples() =
  examples ?: emptyMap()

internal fun MediaType.contractExample(exampleKey: String?): Example? =
  exampleKey?.let { safeExamples()[exampleKey]?.let { Example(it.value) } }

internal fun Parameter.safeExamples() =
  examples ?: emptyMap()

internal fun Parameter.contractExample(exampleKey: String?): Example? =
  exampleKey?.let { safeExamples()[exampleKey]?.let { Example(it.value) } }

internal fun Header.safeExamples() =
  examples ?: emptyMap()

internal fun Header.contractExample(exampleKey: String?): Example? =
  exampleKey?.let { safeExamples()[exampleKey]?.let { Example(it.value) } }

internal fun List<Parameter>.exampleKeys() =
  flatMap { it.safeExamples().keys }.toSet()

internal fun Content.exampleKeys() =
  flatMap { it.value.safeExamples().keys }.toSet()

internal fun ApiResponse.safeHeaders() =
  headers ?: emptyMap()

internal fun ApiResponse.exampleKeys() =
  safeHeaders().exampleKeys() + bodyExampleKeys()

internal fun ApiResponse.bodyExampleKeys() =
  content?.exampleKeys() ?: emptySet()

internal fun Operation.safeParameters() =
  parameters ?: emptyList()

internal fun Operation.requestExampleKeys() =
  safeParameters().exampleKeys() + (requestBody?.content?.exampleKeys() ?: emptySet())

internal fun Map<String, Header>.exampleKeys() =
  flatMap { it.value.safeExamples().keys }.toSet()

internal fun Schema<*>.safeNullable() =
  nullable ?: false

internal fun <T> Schema<T>.safeEnum() =
  enum ?: emptyList()

internal fun Schema<*>.safeExclusiveMinimum() =
  exclusiveMinimum ?: false

internal fun Schema<*>.safeExclusiveMaximum() =
  exclusiveMaximum ?: false

internal fun Schema<*>.fullyResolve() =
  this.`$ref`?.let { SharedComponents.findSchema(it) } ?: success(this.apply { name = name ?: "Inline Schema" })

internal fun Schema<*>.toContracteerDiscriminator() =
  discriminator?.let {
    Discriminator(it.propertyName,
                  it.safeMapping().mapValues { mapping -> mapping.value.replace(COMPONENTS_SCHEMAS_REF, "") })
  }

internal fun Schema<*>.convertToDataType(): Result<DataType<out Any>> =
  fullyResolve().flatMap { fullyResolved ->
    when (fullyResolved) {
      is ArraySchema                                    -> ArraySchemaConverter.convert(fullyResolved)
      is BinarySchema                                   -> BinarySchemaConverter.convert(fullyResolved)
      is ByteArraySchema                                -> Base64SchemaConverter.convert(fullyResolved)
      is BooleanSchema                                  -> BooleanSchemaConverter.convert(fullyResolved)
      is ComposedSchema if(fullyResolved.allOf != null) -> AllOfSchemaConverter.convert(fullyResolved)
      is ComposedSchema if(fullyResolved.anyOf != null) -> AnyOfSchemaConverter.convert(fullyResolved)
      is ComposedSchema if(fullyResolved.oneOf != null) -> OneOfSchemaConverter.convert(fullyResolved)
      is DateSchema                                     -> DateSchemaConverter.convert(fullyResolved)
      is DateTimeSchema                                 -> DateTimeSchemaConverter.convert(fullyResolved)
      is EmailSchema                                    -> EmailSchemaConverter.convert(fullyResolved)
      is IntegerSchema                                  -> IntegerSchemaConverter.convert(fullyResolved)
      is NumberSchema                                   -> NumberSchemaConverter.convert(fullyResolved)
      is StringSchema                                   -> StringSchemaConverter.convert(fullyResolved, "string")
      is ObjectSchema                                   -> ObjectSchemaConverter.convert(fullyResolved)
      is PasswordSchema                                 -> StringSchemaConverter.convert(fullyResolved, "string/password")
      is UUIDSchema                                     -> UuidSchemaConverter.convert(fullyResolved)
      else                                              -> failure("Schema ${fullyResolved!!::class.java.simpleName} is not yet supported")
    }
  }

internal fun io.swagger.v3.oas.models.media.Discriminator.safeMapping() =
  mapping ?: emptyMap()

internal fun Operation.generatePathParameters(exampleKey: String? = null) =
  safeParameters()
    .filter { it.`in` == "path" }
    .map { param ->
      // TODO manage param.required
      val example = param.contractExample(exampleKey)
      param.schema.convertToDataType()
        .flatMap { it!!.validateExample(example, param.name) }
        .map { PathParameter(param.name, it!!, example) }
    }.combineResults()

internal fun Operation.generateQueryParameters(exampleKey: String? = null) =
  safeParameters()
    .filter { it.`in` == "query" }
    .toContractParameters(exampleKey)

internal fun Operation.generateRequestHeaders(exampleKey: String? = null) =
  safeParameters()
    .filter { it.`in` == "header" }
    .toContractParameters(exampleKey)

internal fun Operation.generateRequestCookies(exampleKey: String? = null) =
  safeParameters()
    .filter { it.`in` == "cookie" }
    .toContractParameters(exampleKey)

internal fun Operation.generateRequestBodies(exampleKey: String? = null): Result<List<Body>> =
  if (requestBody?.content != null) {
    requestBody.content
      .map { (contentType, mediaType) ->
        mediaType.schema
          .convertToDataType()
          .flatMap { Body.create(ContentType(contentType), it!!, mediaType.contractExample(exampleKey)) }
      }.combineResults()
  } else success(emptyList())

internal fun ApiResponse.generateResponseBodies(exampleKey: String? = null): Result<List<Body>> =
  if (content != null) {
    content.map { (contentType, mediaType) ->
      mediaType.schema.convertToDataType()
        .flatMap { Body.create(ContentType(contentType), it!!, mediaType.contractExample(exampleKey)) }
    }.combineResults()
  } else success(emptyList())

internal fun ApiResponse.generateResponseHeaders(exampleKey: String? = null) =
  safeHeaders().map { (name, header) ->
    val example = header.contractExample(exampleKey)
    header.schema.convertToDataType()
      .flatMap { it!!.validateExample(example, name) }
      .map { ContractParameter(name, it!!, header.required ?: false, example) }
  }.combineResults()

internal fun List<Parameter>.toContractParameters(exampleKey: String?): Result<List<ContractParameter>> =
  map { param ->
    val example = param.contractExample(exampleKey)
    param.schema.convertToDataType()
      .flatMap { it!!.validateExample(example, param.name) }
      .map { ContractParameter(param.name, it!!, param.required ?: false, example) }
  }.combineResults()

private fun DataType<*>.validateExample(example: Example?, propertyName: String? = null): Result<DataType<*>> =
  if (example == null) success(this)
  else validate(example.normalizedValue)
    .let { if (propertyName != null) it.forProperty(propertyName) else it }
    .map { this }
