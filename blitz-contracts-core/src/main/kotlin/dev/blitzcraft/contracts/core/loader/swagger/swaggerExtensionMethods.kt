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
  when (this) {
    is BooleanSchema   -> BooleanDataType(safeNullable())
    is IntegerSchema   -> IntegerDataType(safeNullable())
    is NumberSchema    -> DecimalDataType(safeNullable())
    is StringSchema    -> StringDataType(isNullable =  safeNullable())
    is PasswordSchema  -> StringDataType("string/password", safeNullable())
    is BinarySchema    -> StringDataType("string/binary", safeNullable())
    is UUIDSchema      -> UuidDataType(safeNullable())
    is ByteArraySchema -> Base64DataType(safeNullable())
    is EmailSchema     -> EmailDataType(safeNullable())
    is DateTimeSchema  -> DateTimeDataType(safeNullable())
    is DateSchema      -> DateDataType(safeNullable())
    is ObjectSchema    -> ObjectDataType(properties = properties.mapValues { it.value.toDataType() },
                                         requiredProperties = required?.toSet() ?: emptySet(),
                                         isNullable = safeNullable())
    is ArraySchema     -> ArrayDataType(itemDataType = items.toDataType(),
                                        isNullable = safeNullable())
    else               -> TODO("Schema ${this::class.java} is not yet supported")
  }

internal fun Schema<*>.safeNullable() = nullable ?: false
