package dev.contracteer.core.datatype

import dev.contracteer.core.Result.Companion.failure
import dev.contracteer.core.Result.Companion.success
import dev.contracteer.core.datatype.GenerationOutcome.Value
import java.net.URI
import java.net.URISyntaxException

/** OpenAPI `string` type with `format: uri`. Values must be absolute URIs per RFC 3986. */
class UriDataType private constructor(name: String,
                                      isNullable: Boolean,
                                      allowedValues: AllowedValues? = null):
    ResolvedDataType<String>(name, "string/uri", isNullable, String::class.java, allowedValues) {

  override fun doValidate(value: String) =
    try {
      if (URI(value).isAbsolute) success(value)
      else failure("Invalid uri. The provided string is not an absolute URI.")
    } catch (_: URISyntaxException) {
      failure("Invalid uri. The provided string is not a valid URI.")
    }

  override fun doRandomValue(ctx: GenerationContext): GenerationOutcome<String> = Value(randomAbsoluteUri())

  companion object {
    @JvmStatic
    fun create(name: String, isNullable: Boolean, enum: List<String?>) =
      UriDataType(name, isNullable).let { dataType ->
        if (enum.isEmpty()) success(dataType)
        else AllowedValues.create(enum, dataType).map { UriDataType(name, isNullable, it) }
      }

    private fun randomAbsoluteUri(): String {
      val host = HostnameDataType.randomHostname()
      val path = randomAlphanumeric(3..8)
      val query = if ((0..1).random() == 1) "?${randomAlphanumeric(3..8)}=${randomAlphanumeric(3..8)}" else ""
      return "https://$host/$path$query"
    }
  }
}
