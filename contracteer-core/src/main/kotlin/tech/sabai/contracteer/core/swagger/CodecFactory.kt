package tech.sabai.contracteer.core.swagger

import tech.sabai.contracteer.core.Result
import tech.sabai.contracteer.core.Result.Companion.failureForKey
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.codec.DeepObjectParameterCodec
import tech.sabai.contracteer.core.codec.EncodingShape
import tech.sabai.contracteer.core.codec.FormParameterCodec
import tech.sabai.contracteer.core.codec.LabelParameterCodec
import tech.sabai.contracteer.core.codec.MatrixParameterCodec
import tech.sabai.contracteer.core.codec.ParameterCodec
import tech.sabai.contracteer.core.codec.PipeDelimitedParameterCodec
import tech.sabai.contracteer.core.codec.SimpleParameterCodec
import tech.sabai.contracteer.core.codec.SpaceDelimitedParameterCodec
import tech.sabai.contracteer.core.combineResults
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.operation.ParameterElement
import tech.sabai.contracteer.core.operation.ParameterElement.Cookie
import tech.sabai.contracteer.core.operation.ParameterElement.PathParam
import tech.sabai.contracteer.core.operation.ParameterElement.QueryParam
import tech.sabai.contracteer.core.result
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
    EncodingShape.of(dataType).flatMap { shape ->
      when (style) {
        Simple, Form, Label, Matrix   -> validateFlatStructuralValues(style, shape, paramName) andThen
                                           { validateFormAdditionalProperties(style, explode, shape, paramName) }
        DeepObject                    -> validateDeepObjectParameters(style, shape, paramName, explode)
        SpaceDelimited, PipeDelimited -> validateDelimitedArrayParameter(style, shape, paramName, explode)
      }
    }

  private fun validateFormAdditionalProperties(style: Style,
                                               explode: Boolean,
                                               shape: EncodingShape,
                                               paramName: String): Result<Unit> =
    if (style == Form && explode && shape is EncodingShape.Object && shape.constrainsAdditionalProperties())
      failureForKey(paramName,
                    "Style 'form' with explode=true cannot enforce 'additionalProperties' constraints: " +
                        "the encoding does not distinguish properties of this object from other query parameters. " +
                        "Use 'deepObject' instead — its 'paramName[key]' syntax unambiguously scopes properties to this parameter")
    else
      success()

  private fun EncodingShape.Object.constrainsAdditionalProperties(): Boolean =
    !additionalProperties.allowed || additionalProperties.itemType != null

  private fun validateFlatStructuralValues(style: Style,
                                           shape: EncodingShape,
                                           paramName: String): Result<Unit> =
    when (shape) {
      is EncodingShape.Object -> rejectIfPropertiesAreNonPrimitive(shape, paramName,
                                                                   "Style '${style.canonicalName}' does not support objects with nested objects or arrays in properties $UNDEFINED_BEHAVIOR")
      is EncodingShape.Array  -> rejectIfItemsAreNonPrimitive(shape, paramName,
                                                              "Style '${style.canonicalName}' does not support arrays with nested objects or arrays as items $UNDEFINED_BEHAVIOR")
      else                    -> success()
    }

  private fun validateDeepObjectParameters(style: Style,
                                           shape: EncodingShape,
                                           paramName: String,
                                           explode: Boolean): Result<Unit> =
    when {
      shape !is EncodingShape.Object -> failureForKey(paramName, "Style '${style.canonicalName}' requires object type")
      !explode                       -> failureForKey(paramName, "Style '${style.canonicalName}' requires explode=true")
      else                           -> rejectIfPropertiesAreNonPrimitive(shape, paramName,
                                                                          "Style '${style.canonicalName}' does not support nested objects or arrays in properties $UNDEFINED_BEHAVIOR")
    }

  private fun validateDelimitedArrayParameter(style: Style,
                                              shape: EncodingShape,
                                              paramName: String,
                                              explode: Boolean): Result<Unit> =
    when {
      shape !is EncodingShape.Array -> failureForKey(paramName, "Style '${style.canonicalName}' requires array type")
      explode                        -> failureForKey(paramName, "Style '${style.canonicalName}' requires explode=false")
      else                           -> success()
    }

  private fun rejectIfPropertiesAreNonPrimitive(shape: EncodingShape.Object,
                                                paramName: String,
                                                error: String): Result<Unit> =
    shape.properties.values
      .map { it.shape() }
      .combineResults()
      .flatMap { propShapes ->
        if (propShapes.any { it is EncodingShape.Object || it is EncodingShape.Array })
          failureForKey(paramName, error)
        else
          success()
      }

  private fun rejectIfItemsAreNonPrimitive(shape: EncodingShape.Array,
                                           paramName: String,
                                           error: String): Result<Unit> =
    shape.itemType.shape().flatMap { itemShape ->
      if (itemShape is EncodingShape.Object || itemShape is EncodingShape.Array)
        failureForKey(paramName, error)
      else
        success()
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

private const val UNDEFINED_BEHAVIOR = "(undefined behavior in the OpenAPI document)"
