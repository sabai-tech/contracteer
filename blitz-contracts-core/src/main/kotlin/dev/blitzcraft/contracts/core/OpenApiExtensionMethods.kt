package dev.blitzcraft.contracts.core

import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.headers.Header
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.responses.ApiResponse

fun Map.Entry<PathItem.HttpMethod, Operation>.method() = key
fun Map.Entry<PathItem.HttpMethod, Operation>.operation() = value
fun Map.Entry<String, PathItem>.path() = key
fun Map.Entry<String, PathItem>.item() = value
fun Map.Entry<String, ApiResponse>.code() = key
fun Map.Entry<String, ApiResponse>.response() = value
fun MediaType.safeExamples() = examples ?: emptyMap()
fun Parameter.safeExamples() = examples ?: emptyMap()
fun Header.safeExamples() = examples ?: emptyMap()
fun Operation.safeParameters(): List<Parameter> = parameters ?: emptyList()
fun ApiResponse.safeHeaders() = headers ?: emptyMap()
