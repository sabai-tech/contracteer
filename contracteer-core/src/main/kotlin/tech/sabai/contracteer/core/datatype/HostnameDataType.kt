package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success
import tech.sabai.contracteer.core.datatype.GenerationOutcome.Value

/** OpenAPI `string` type with `format: hostname`. Values must be valid RFC 1123 hostnames. */
class HostnameDataType private constructor(name: String,
                                           isNullable: Boolean,
                                           allowedValues: AllowedValues? = null):
    ResolvedDataType<String>(name, "string/hostname", isNullable, String::class.java, allowedValues) {

  override fun isFullyStructured() = false

  override fun doValidate(value: String) =
    when {
      value.length > MAX_HOSTNAME_LENGTH -> failure("Invalid hostname. The provided string exceeds $MAX_HOSTNAME_LENGTH characters.")
      hostnameRegex.matches(value)       -> success(value)
      else                               -> failure("Invalid hostname. The provided string is not a valid RFC 1123 hostname.")
    }

  override fun doRandomValue(ctx: GenerationContext): GenerationOutcome<String> = Value(randomHostname())

  companion object {
    private const val MAX_HOSTNAME_LENGTH = 253
    private const val LABEL_PATTERN = "[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?"
    private val hostnameRegex = "^$LABEL_PATTERN(?:\\.$LABEL_PATTERN)*$".toRegex()
    private val TOP_LEVEL_DOMAINS = listOf("com", "io", "org", "net", "dev")

    @JvmStatic
    fun create(name: String, isNullable: Boolean, enum: List<String?>) =
      HostnameDataType(name, isNullable).let { dataType ->
        if (enum.isEmpty()) success(dataType)
        else AllowedValues.create(enum, dataType).map { HostnameDataType(name, isNullable, it) }
      }

    internal fun randomHostname(): String {
      val labels = (1..(2..3).random()).map { randomAlphanumeric(3..8) }
      return (labels + TOP_LEVEL_DOMAINS.random()).joinToString(".")
    }
  }
}
