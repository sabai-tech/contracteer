package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.datatype.GenerationOutcome.Value
import java.net.URI
import java.net.URISyntaxException

/**
 * OpenAPI `string` type with `format: uri-reference`. Values must be a valid URI reference per
 * RFC 3986: either an absolute URI or a relative reference. Generation always produces a
 * relative reference; the absolute form is the responsibility of `format: uri`.
 */
class UriReferenceDataType private constructor(name: String,
                                               isNullable: Boolean,
                                               allowedValues: AllowedValues? = null):
    ResolvedDataType<String>(name, "string/uri-reference", isNullable, String::class.java, allowedValues) {

  override fun doValidate(value: String) =
    try {
      URI(value)
      success(value)
    } catch (_: URISyntaxException) {
      failure("Invalid uri-reference. The provided string is not a valid URI reference.")
    }

  override fun doRandomValue(ctx: GenerationContext): GenerationOutcome<String> = Value(randomRelativeReference())

  companion object {
    @JvmStatic
    fun create(name: String, isNullable: Boolean, enum: List<String?>) =
      UriReferenceDataType(name, isNullable).let { dataType ->
        if (enum.isEmpty()) success(dataType)
        else AllowedValues.create(enum, dataType).map { UriReferenceDataType(name, isNullable, it) }
      }

    private fun randomRelativeReference(): String {
      val leadingSlash = if ((0..1).random() == 1) "/" else ""
      val path = (1..(2..3).random()).joinToString("/") { randomAlphanumeric(3..8) }
      val query = if ((0..1).random() == 1) "?${randomAlphanumeric(3..8)}=${randomAlphanumeric(3..8)}" else ""
      return "$leadingSlash$path$query"
    }
  }
}
