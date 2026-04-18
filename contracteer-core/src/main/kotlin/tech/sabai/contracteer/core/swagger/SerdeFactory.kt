package tech.sabai.contracteer.core.swagger

import io.swagger.v3.oas.models.media.MediaType
import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.accumulate
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.datatype.ArrayDataType
import tech.sabai.contracteer.core.datatype.Base64DataType
import tech.sabai.contracteer.core.datatype.BinaryDataType
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.datatype.ObjectDataType
import tech.sabai.contracteer.core.operation.ContentType
import tech.sabai.contracteer.core.operation.ParameterElement.QueryParam
import tech.sabai.contracteer.core.serde.FormUrlEncodedSerde
import tech.sabai.contracteer.core.serde.JsonSerde
import tech.sabai.contracteer.core.serde.MultipartSerde
import tech.sabai.contracteer.core.serde.PartConfig
import tech.sabai.contracteer.core.serde.PlainTextSerde
import tech.sabai.contracteer.core.serde.Serde

internal class SerdeFactory(private val codecFactory: CodecFactory) {

  fun buildSerde(contentType: ContentType,
                 mediaType: MediaType,
                 dataType: DataType<out Any>): Result<Serde> =
    when {
      contentType.isFormUrlEncoded()                                                      ->
        when (dataType) {
          is ObjectDataType -> buildFormUrlEncodedSerde(dataType, mediaType)
          else              -> failure("Content type ${contentType.value} requires object schema")
        }

      contentType.isMultipart()                                                           ->
        when (dataType) {
          is ObjectDataType -> buildMultipartSerde(dataType, mediaType)
          else              -> failure("Content type ${contentType.value} requires object schema")
        }

      contentType.isJson()                                                                ->
        success(JsonSerde)

      !contentType.isXml() && (dataType.isFullyStructured() || dataType is ArrayDataType) ->
        failure("Content type ${contentType.value} supports only primitive schemas (string, integer, number, boolean and their formats)")

      else                                                                                ->
        success(PlainTextSerde)
    }

  private fun buildFormUrlEncodedSerde(dataType: ObjectDataType, mediaType: MediaType): Result<Serde> {
    val nestedTypeErrors = validateFormUrlEncodedProperties(dataType)
    if (nestedTypeErrors.isFailure()) return nestedTypeErrors.retypeError()

    val encodingMap = mediaType.encoding ?: emptyMap()
    return dataType.properties
      .map { (name, type) ->
        val encoding = encodingMap[name]
        val allowReserved = encoding?.allowReserved == true
        codecFactory.createCodec(QueryParam(name), encoding?.style?.toString(), encoding?.explode, type, name)
          .map { name to FormUrlEncodedSerde.PropertyEncoding(it, allowReserved) }
      }
      .combineResults()
      .map<Serde> { FormUrlEncodedSerde(it.toMap()) }
  }

  private fun validateFormUrlEncodedProperties(dataType: ObjectDataType): Result<Unit> =
    dataType.properties.entries.accumulate { (name, type) ->
      when (type) {
        is ObjectDataType                                      ->
          failure(name, "Form-urlencoded does not support nested object properties $UNDEFINED_BEHAVIOR")

        is ArrayDataType if type.itemDataType.isNonPrimitive() ->
          failure(name, "Form-urlencoded does not support arrays of complex types $UNDEFINED_BEHAVIOR")

        else                                                   ->
          success()
      }
    }

  private fun buildMultipartSerde(dataType: ObjectDataType, mediaType: MediaType): Result<Serde> {
    val encodingMap = mediaType.encoding ?: emptyMap()
    val partConfigs = dataType.properties.map { (name, propType) ->
      val contentType = encodingMap[name]?.contentType ?: defaultPartContentType(propType)
      val isFile = propType.isBinary()
      val isFileArray = propType is ArrayDataType && propType.itemDataType.isBinary()
      name to PartConfig(contentType, serdeForContentType(contentType), isFile || isFileArray, isFileArray)
    }.toMap()
    return success(MultipartSerde(partConfigs))
  }

  private fun defaultPartContentType(dataType: DataType<out Any>): String = when {
    dataType.isBinary()                                           -> "application/octet-stream"
    dataType is ArrayDataType && dataType.itemDataType.isBinary() -> "application/octet-stream"
    dataType is ArrayDataType || dataType.isFullyStructured()     -> "application/json"
    else                                                          -> "text/plain"
  }

  private fun serdeForContentType(contentType: String): Serde =
    if ("json" in contentType.lowercase()) JsonSerde else PlainTextSerde
}

private fun DataType<out Any>.isBinary() = this is BinaryDataType || this is Base64DataType

private fun DataType<out Any>.isNonPrimitive(): Boolean =
  isFullyStructured() || this is ArrayDataType

private const val UNDEFINED_BEHAVIOR = "(undefined behavior in the OpenAPI specification)"
