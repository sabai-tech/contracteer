package dev.blitzcraft.contracts.core.loader.swagger

import dev.blitzcraft.contracts.core.contract.ContractParameter
import dev.blitzcraft.contracts.core.contract.Example
import dev.blitzcraft.contracts.core.contract.PathParameter
import dev.blitzcraft.contracts.core.datatype.*
import dev.blitzcraft.contracts.core.datatype.Discriminator
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.headers.Header
import io.swagger.v3.oas.models.media.*
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.responses.ApiResponse


internal fun MediaType.safeExamples() = examples ?: emptyMap()

internal fun MediaType.contractExample(exampleKey: String?): Example? =
  exampleKey?.let { safeExamples()[exampleKey]?.let { Example(it.value) } }

internal fun Parameter.safeExamples() =
  examples ?: emptyMap()

internal fun Parameter.contractExample(exampleKey: String?): Example? =
  exampleKey?.let { safeExamples()[exampleKey]?.let { Example(it.value) } }

internal fun Parameter.toContractParameter(exampleKey: String?) =
  ContractParameter(name, schema.toDataType(), required ?: false, contractExample(exampleKey))

internal fun Parameter.toPathParameter(exampleKey: String?) =
  PathParameter(name, schema.toDataType(), contractExample(exampleKey))

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

internal fun ApiResponse.headersParameters(exampleKey: String? = null) =
  safeHeaders().map { it.toContractParameter(exampleKey) }

internal fun Operation.safeParameters() =
  parameters ?: emptyList()

internal fun Operation.requestExampleKeys() =
  safeParameters().exampleKeys() + (requestBody?.content?.exampleKeys() ?: emptySet())

internal fun Operation.pathParameters(exampleKey: String? = null) =
  safeParameters().filter { it.`in` == "path" }.map { it.toPathParameter(exampleKey) }

internal fun Operation.queryParameters(exampleKey: String? = null) =
  safeParameters().filter { it.`in` == "query" }.map { it.toContractParameter(exampleKey) }

internal fun Operation.headersParameters(exampleKey: String? = null) =
  safeParameters().filter { it.`in` == "header" }.map { it.toContractParameter(exampleKey) }

internal fun Operation.cookiesParameter(exampleKey: String? = null) =
  safeParameters().filter { it.`in` == "cookie" }.map { it.toContractParameter(exampleKey) }

internal fun Map.Entry<String, Header>.toContractParameter(exampleKey: String? = null) =
  ContractParameter(key, value.schema.toDataType(), value.required ?: false, value.contractExample(exampleKey))

internal fun Map<String, Header>.exampleKeys() =
  flatMap { it.value.safeExamples().keys }.toSet()


internal fun Schema<*>.toDataType(): DataType<*> =
  when (val fullyResolved = this.fullyResolve()) {
    is ComposedSchema  -> createComposedObjectDataType(fullyResolved)
    is BooleanSchema   -> BooleanDataType(fullyResolved.name, fullyResolved.safeNullable())
    is IntegerSchema   -> IntegerDataType(fullyResolved.name, fullyResolved.safeNullable())
    is NumberSchema    -> NumberDataType(fullyResolved.name, fullyResolved.safeNullable())
    is StringSchema    -> StringDataType(fullyResolved.name, isNullable = fullyResolved.safeNullable())
    is PasswordSchema  -> StringDataType(fullyResolved.name, "string/password", fullyResolved.safeNullable())
    is BinarySchema    -> StringDataType(fullyResolved.name, "string/binary", fullyResolved.safeNullable())
    is UUIDSchema      -> UuidDataType(fullyResolved.name, fullyResolved.safeNullable())
    is ByteArraySchema -> Base64DataType(fullyResolved.name, fullyResolved.safeNullable())
    is EmailSchema     -> EmailDataType(fullyResolved.name, fullyResolved.safeNullable())
    is DateTimeSchema  -> DateTimeDataType(fullyResolved.name, fullyResolved.safeNullable())
    is DateSchema      -> DateDataType(fullyResolved.name, fullyResolved.safeNullable())
    is ObjectSchema    -> ObjectDataType(name = fullyResolved.name,
                                         properties = fullyResolved.properties.mapValues { it.value.toDataType() },
                                         requiredProperties = fullyResolved.required?.toSet() ?: emptySet(),
                                         isNullable = fullyResolved.safeNullable())
    is ArraySchema     -> ArrayDataType(name = fullyResolved.name,
                                        itemDataType = fullyResolved.items.toDataType(),
                                        isNullable = fullyResolved.safeNullable())
    else               -> TODO("Schema ${this::class.java} is not yet supported")
  }

private fun createComposedObjectDataType(composedSchema: ComposedSchema) =
  when {
    composedSchema.oneOf != null -> OneOfDataType(composedSchema.name,
                                                  composedSchema.oneOf.map { it.toDataType() as StructuredObjectDataType },
                                                  getDiscriminatorFrom(composedSchema),
                                                  composedSchema.safeNullable())
    composedSchema.anyOf != null -> AnyOfDataType(composedSchema.name,
                                                  composedSchema.anyOf.map { it.toDataType() as StructuredObjectDataType },
                                                  getDiscriminatorFrom(composedSchema),
                                                  composedSchema.safeNullable())
    composedSchema.allOf != null -> AllOfDataType(composedSchema.name,
                                                  composedSchema.allOf.map { it.toDataType() as StructuredObjectDataType },
                                                  composedSchema.safeNullable())
    else                         -> TODO("Schema ${composedSchema::class.java} is not yet supported")
  }

private fun getDiscriminatorFrom(composedSchema: ComposedSchema) =
  composedSchema.discriminator?.let {
    Discriminator(
      it.propertyName,
      it.mapping.mapValues { schemaName ->
        SharedComponents.findSchema(schemaName.value).toDataType() as ObjectDataType
      })
  }

internal fun Schema<*>.safeNullable() = nullable ?: false
internal fun Schema<*>.fullyResolve() =
  this.`$ref`?.let { SharedComponents.findSchema(it) } ?: this.apply { name = name ?: "Inline Schema" }
