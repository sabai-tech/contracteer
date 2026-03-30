package tech.sabai.contracteer.core.swagger

import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.headers.Header
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success

internal class SharedComponents(
  val schemas: Map<String, Schema<*>>,
  private val parameters: Map<String, Parameter>,
  private val requestBodies: Map<String, RequestBody>,
  private val headers: Map<String, Header>,
  private val examples: Map<String, Example>,
  private val responses: Map<String, ApiResponse>
) {

  fun resolve(schema: Schema<*>): Result<Schema<*>> =
    resolveRef(schema, schemas, { it.shortRef() }, "Schema", "components/schemas")

  fun resolve(parameter: Parameter): Result<Parameter> =
    resolveRef(parameter, parameters, Parameter::shortRef, "Parameter", "components/parameters")

  fun resolve(requestBody: RequestBody): Result<RequestBody> =
    resolveRef(requestBody, requestBodies, RequestBody::shortRef, "Request Body", "components/requestBodies")

  fun resolve(header: Header): Result<Header> =
    resolveRef(header, headers, Header::shortRef, "Response Header", "components/headers")

  fun resolve(example: Example): Result<Example> =
    resolveRef(example, examples, Example::shortRef, "Example", "components/examples")

  fun resolve(response: ApiResponse): Result<ApiResponse> =
    resolveRef(response, responses, ApiResponse::shortRef, "Response", "components/responses")

  private fun <T> resolveRef(component: T,
                             sharedComponents: Map<String, T>,
                             getRef: (T) -> String?,
                             componentName: String,
                             section: String,
                             maxDepth: Int = 10): Result<T> {
    val ref = getRef(component)
    return when {
      maxDepth < 0                               -> failure("Maximum recursive depth reached while resolving $componentName")
      ref == null                                -> success(component)
      sharedComponents[ref]?.let(getRef) != null -> resolveRef(sharedComponents[ref]!!,sharedComponents,getRef,componentName,section,maxDepth - 1)
      sharedComponents[ref] != null              -> success(sharedComponents[ref]!!)
      else                                       -> failure("$componentName '$ref' not found in '$section' section")
    }
  }
}
