package dev.blitzcraft.contracts.core.loader.swagger

import dev.blitzcraft.contracts.core.datatype.*
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.headers.Header
import io.swagger.v3.oas.models.media.*
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.responses.ApiResponse

internal fun Map.Entry<PathItem.HttpMethod, Operation>.method() = key
internal fun Map.Entry<PathItem.HttpMethod, Operation>.operation() = value
internal fun Map.Entry<String, PathItem>.path() = key
internal fun Map.Entry<String, PathItem>.item() = value
internal fun Map.Entry<String, ApiResponse>.code() = key
internal fun Map.Entry<String, ApiResponse>.response() = value
internal fun Map.Entry<String, MediaType>.mediaType() = value
internal fun Map.Entry<String, MediaType>.content() = key
internal fun Map.Entry<String, Example>.example() = value
internal fun Map.Entry<String, Example>.name() = key
internal fun Map<String, Header>.exampleKeys() = flatMap { it.value.safeExamples().keys }.toSet()
internal fun MediaType.safeExamples() = examples ?: emptyMap()
internal fun Parameter.safeExamples() = examples ?: emptyMap()
internal fun Header.safeExamples() = examples ?: emptyMap()
internal fun List<Parameter>.exampleKeys() = flatMap { it.safeExamples().keys }.toSet()
internal fun Content.exampleKeys() = flatMap { it.value.safeExamples().keys }.toSet()
internal fun ApiResponse.safeHeaders() = headers ?: emptyMap()
internal fun ApiResponse.exampleKeys() = safeHeaders().exampleKeys() + bodyExampleKeys()
internal fun ApiResponse.bodyExampleKeys() = content?.exampleKeys() ?: emptySet()
internal fun Operation.safeParameters(): List<Parameter> = parameters ?: emptyList()
internal fun Operation.requestExampleKeys() =
  safeParameters().exampleKeys() + (requestBody?.content?.exampleKeys() ?: emptySet())


internal fun Schema<*>.toDataType(): DataType<*> =
  when (val fullyResolved = this.fullyResolve()) {
    is ComposedSchema  -> createComposedObjectDataType(fullyResolved)
    is BooleanSchema   -> BooleanDataType(fullyResolved.name, fullyResolved.safeNullable())
    is IntegerSchema   -> IntegerDataType(fullyResolved.name, fullyResolved.safeNullable())
    is NumberSchema    -> DecimalDataType(fullyResolved.name, fullyResolved.safeNullable())
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

fun createComposedObjectDataType(composedSchema: ComposedSchema) =
  when {
    composedSchema.oneOf != null -> OneOfDataType(composedSchema.name,
                                                  composedSchema.oneOf.map { it.toDataType() as ObjectDataType },
                                                  composedSchema.safeNullable())
    composedSchema.anyOf != null -> AnyOfDataType(composedSchema.name,
                                                  composedSchema.anyOf.map { it.toDataType() as ObjectDataType },
                                                  composedSchema.safeNullable())
    else                         -> TODO("Not Yet Implemented")
  }

internal fun Schema<*>.safeNullable() = nullable ?: false
internal fun Schema<*>.fullyResolve() = SharedComponents.fullyResolve(this)
