package tech.sabai.contracteer.core.swagger

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failureForKey
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.result
import tech.sabai.contracteer.core.codec.DeepObjectParameterCodec
import tech.sabai.contracteer.core.codec.FormParameterCodec
import tech.sabai.contracteer.core.codec.LabelParameterCodec
import tech.sabai.contracteer.core.codec.MatrixParameterCodec
import tech.sabai.contracteer.core.codec.ParameterCodec
import tech.sabai.contracteer.core.codec.PipeDelimitedParameterCodec
import tech.sabai.contracteer.core.codec.SimpleParameterCodec
import tech.sabai.contracteer.core.codec.SpaceDelimitedParameterCodec
import tech.sabai.contracteer.core.datatype.ArrayDataType
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.datatype.ObjectDataType
import tech.sabai.contracteer.core.operation.ParameterElement
import tech.sabai.contracteer.core.operation.ParameterElement.Cookie
import tech.sabai.contracteer.core.operation.ParameterElement.PathParam
import tech.sabai.contracteer.core.operation.ParameterElement.QueryParam
import tech.sabai.contracteer.core.swagger.Style.DeepObject
import tech.sabai.contracteer.core.swagger.Style.Form
import tech.sabai.contracteer.core.swagger.Style.Label
import tech.sabai.contracteer.core.swagger.Style.Matrix
import tech.sabai.contracteer.core.swagger.Style.PipeDelimited
import tech.sabai.contracteer.core.swagger.Style.Simple
import tech.sabai.contracteer.core.swagger.Style.SpaceDelimited

internal class CodecFactory {

  fun createCodec(element: ParameterElement,
                  style: String?,
                  explode: Boolean?,
                  dataType: DataType<out Any>,
                  paramName: String,
                  allowReserved: Boolean = false): Result<ParameterCodec> =
    result {
      val (actualStyle, actualExplode) = resolveStyle(element, style, explode, paramName).bind()
      validateStyleConstraints(actualStyle, actualExplode, dataType, paramName).bind()
      buildCodec(actualStyle, actualExplode, paramName, allowReserved)
    }

  private fun buildCodec(style: Style, explode: Boolean, paramName: String, allowReserved: Boolean): ParameterCodec =
    when (style) {
      Simple         -> SimpleParameterCodec(paramName, explode)
      Form           -> FormParameterCodec(paramName, explode, allowReserved)
      Label          -> LabelParameterCodec(paramName, explode)
      Matrix         -> MatrixParameterCodec(paramName, explode)
      SpaceDelimited -> SpaceDelimitedParameterCodec(paramName, allowReserved)
      PipeDelimited  -> PipeDelimitedParameterCodec(paramName, allowReserved)
      DeepObject     -> DeepObjectParameterCodec(paramName, allowReserved)
    }

  private fun resolveStyle(element: ParameterElement,
                           style: String?,
                           explode: Boolean?,
                           paramName: String): Result<Pair<Style, Boolean>> {
    val (defaultStyle, defaultExplode, supportedStyles) = when (element) {
      is PathParam               -> StyleDefaults(Simple, false, setOf(Simple, Label, Matrix))
      is QueryParam              -> StyleDefaults(Form, true, setOf(Form, SpaceDelimited, PipeDelimited, DeepObject))
      is ParameterElement.Header -> StyleDefaults(Simple, false, setOf(Simple))
      is Cookie                  -> StyleDefaults(Form, true, setOf(Form))
    }
    val actualStyle = if (style == null) defaultStyle else Style.parse(style)

    return if (actualStyle != null && actualStyle in supportedStyles)
      success(actualStyle to (explode ?: defaultExplode))
    else
      failureForKey(paramName, "Style '$style' is not supported for ${element.locationName} parameters")
  }

  private fun validateStyleConstraints(style: Style,
                                       explode: Boolean,
                                       dataType: DataType<out Any>,
                                       paramName: String): Result<Unit> =
    when (style) {
      Simple, Form, Label, Matrix   -> validateFlatObjectProperties(style, dataType, paramName)
      DeepObject                    -> validateDeepObjectParameters(style, dataType, paramName, explode)
      SpaceDelimited, PipeDelimited -> validateDelimitedArrayParameter(style, dataType, paramName, explode)
    }

  private fun validateFlatObjectProperties(style: Style,
                                           dataType: DataType<out Any>,
                                           paramName: String): Result<Unit> =
    if (dataType is ObjectDataType && dataType.hasNonPrimitiveProperties())
      failureForKey(
        paramName,
        "Style '${style.canonicalName}' does not support objects with nested objects or arrays in properties $UNDEFINED_BEHAVIOR")
    else
      success()

  private fun validateDeepObjectParameters(style: Style,
                                           dataType: DataType<out Any>,
                                           paramName: String,
                                           explode: Boolean): Result<Unit> =
    when {
      dataType !is ObjectDataType          -> failureForKey(paramName, "Style '${style.canonicalName}' requires object type")
      !explode                             -> failureForKey(paramName, "Style '${style.canonicalName}' requires explode=true")
      dataType.hasNonPrimitiveProperties() -> failureForKey(paramName, "Style '${style.canonicalName}' does not support nested objects or arrays in properties $UNDEFINED_BEHAVIOR")
      else                                 -> success()
    }

  private fun validateDelimitedArrayParameter(style: Style,
                                              dataType: DataType<out Any>,
                                              paramName: String,
                                              explode: Boolean): Result<Unit> =
    when {
      dataType !is ArrayDataType -> failureForKey(paramName, "Style '${style.canonicalName}' requires array type")
      explode                    -> failureForKey(paramName, "Style '${style.canonicalName}' requires explode=false")
      else                       -> success()
    }
}

internal sealed class Style(val canonicalName: String) {
  object Simple: Style("simple")
  object Form: Style("form")
  object Label: Style("label")
  object Matrix: Style("matrix")
  object SpaceDelimited: Style("spaceDelimited")
  object PipeDelimited: Style("pipeDelimited")
  object DeepObject: Style("deepObject")

  companion object {
    fun parse(raw: String): Style? = when (raw.lowercase().replace("_", "")) {
      "simple"         -> Simple
      "form"           -> Form
      "label"          -> Label
      "matrix"         -> Matrix
      "spacedelimited" -> SpaceDelimited
      "pipedelimited"  -> PipeDelimited
      "deepobject"     -> DeepObject
      else             -> null
    }
  }
}

private data class StyleDefaults(
  val defaultStyle: Style,
  val defaultExplode: Boolean,
  val supportedStyles: Set<Style>
)

private val ParameterElement.locationName: String
  get() = when (this) {
    is PathParam               -> "path"
    is QueryParam              -> "query"
    is ParameterElement.Header -> "header"
    is Cookie                  -> "cookie"
  }

private fun ObjectDataType.hasNonPrimitiveProperties(): Boolean =
  properties.values.any { it.isNonPrimitive() }

private fun DataType<out Any>.isNonPrimitive(): Boolean =
  isFullyStructured() || this is ArrayDataType

private const val UNDEFINED_BEHAVIOR = "(undefined behavior in the OpenAPI specification)"
