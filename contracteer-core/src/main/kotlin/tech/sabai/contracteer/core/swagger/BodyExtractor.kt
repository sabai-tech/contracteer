package tech.sabai.contracteer.core.swagger

import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.datatype.AnyDataType
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.normalize
import tech.sabai.contracteer.core.operation.BodySchema
import tech.sabai.contracteer.core.operation.ContentType
import tech.sabai.contracteer.core.result
import tech.sabai.contracteer.core.serde.PlainTextSerde
import tech.sabai.contracteer.core.swagger.datatype.DataTypeConverter

internal class BodyExtractor(
  private val sharedComponents: SharedComponents,
  private val dataTypeConverter: DataTypeConverter,
  private val serdeFactory: SerdeFactory
) {

  fun extractRequestBodies(operation: Operation): Result<List<ExtractedBodySchema>> =
    operation.requestBody?.let { convertRequestBodies(it) } ?: success(emptyList())

  fun extractResponseBodies(response: ApiResponse): Result<List<ExtractedBodySchema>> =
    convertBodySchemas(response.content ?: emptyMap(), isRequired = false) { it.asResponseType() }

  private fun convertRequestBodies(requestBody: RequestBody): Result<List<ExtractedBodySchema>> =
    sharedComponents.resolve(requestBody).flatMap { body ->
      convertBodySchemas(body.content ?: emptyMap(), body.safeRequired()) { it.asRequestType() }
    }

  private fun convertBodySchemas(content: Map<String, MediaType>,
                                 isRequired: Boolean,
                                 asType: (DataType<out Any>) -> DataType<out Any>): Result<List<ExtractedBodySchema>> =
    if (content.isEmpty()) success(emptyList())
    else {
      val multiContent = content.size > 1
      content
        .map { (key, mediaType) ->
          val contentType = ContentType(key)
          convertBodySchema(contentType, mediaType, isRequired, asType)
            .let { if (multiContent) it.forKey(contentType.value) else it }
        }
        .combineResults()
    }

  private fun convertBodySchema(contentType: ContentType,
                                mediaType: MediaType,
                                isRequired: Boolean,
                                asType: (DataType<out Any>) -> DataType<out Any>): Result<ExtractedBodySchema> =
    result {
      val dataType = dataTypeConverter.convertMediaTypeSchema(mediaType).bind()
      val examples = sharedComponents.resolve(mediaType.safeExamples()).bind()
        .mapValues { (_, example) -> example.value?.normalize() }
      val (bodyType, serde) =
        if (dataType is AnyDataType) dataType to PlainTextSerde
        else asType(dataType) to serdeFactory.buildSerde(contentType, mediaType, dataType).bind()
      ExtractedBodySchema(BodySchema(contentType, bodyType, isRequired, serde), examples)
    }
}
