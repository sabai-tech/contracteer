package tech.sabai.contracteer.core.swagger.converter.requestbody

import io.swagger.v3.oas.models.parameters.RequestBody
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.contract.Body
import tech.sabai.contracteer.core.contract.ContentType
import tech.sabai.contracteer.core.swagger.contractExample
import tech.sabai.contracteer.core.swagger.converter.schema.SchemaConverter

object RequestBodyConverter {
  private const val RECURSIVE_MAX_DEPTH = 10
  private const val COMPONENTS_REQUEST_BODY_BASE_REF = "#/components/requestBodies/"

  lateinit var sharedRequestBodies: Map<String, RequestBody>

  fun convert(requestBody: RequestBody,
              exampleKey: String?,
              maxRecursiveDepth: Int = RECURSIVE_MAX_DEPTH): Result<List<Body>> {
    val ref = requestBody.shortRef()
    return when {
      maxRecursiveDepth < 0                    -> failure("Maximum recursive depth reached while converting Body")
      ref == null                              -> convertRequestBody(requestBody, exampleKey)
      sharedRequestBodies[ref]?.`$ref` != null -> convert(sharedRequestBodies[ref]!!, exampleKey, maxRecursiveDepth - 1)
      sharedRequestBodies[ref] != null         -> convertRequestBody(sharedRequestBodies[ref]!!, exampleKey)
      else                                     -> failure("Request Body ${requestBody.`$ref`} in 'components/requestBodies' section")
    }
  }

  private fun convertRequestBody(body: RequestBody, exampleKey: String?): Result<List<Body>> {
    return if (body.content == null)
      success(emptyList())
    else
      body.content.map { (contentType, mediaType) ->
        mediaType.contractExample(exampleKey)
          .flatMap { resolvedExample ->
            SchemaConverter
              .convertToDataType(mediaType.schema, "")
              .flatMap { Body.create(ContentType(contentType), it!!, resolvedExample) }
          }
      }.combineResults()
  }

  private fun RequestBody.shortRef() =
    this.`$ref`?.replace(COMPONENTS_REQUEST_BODY_BASE_REF, "")
}