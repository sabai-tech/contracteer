package tech.sabai.contracteer.core

import java.net.URLEncoder

/**
 * URL encoding utilities supporting RFC 3986 `allowReserved` semantics.
 */
object UrlEncoding {

  private val RFC3986_RESERVED = setOf(
    ':', '/', '?', '#', '[', ']', '@', '!', '$', '&', '\'', '(', ')', '*', '+', ',', ';', '='
  )

  /**
   * URL-encodes [value]. When [allowReserved] is `true`, RFC 3986 reserved characters
   * (`:/?#[]@!$&'()*+,;=`) are kept as-is. When `false`, standard URL encoding is applied.
   */
  fun encode(value: String, allowReserved: Boolean): String =
    if (allowReserved) encodePreservingReserved(value) else urlEncode(value)

  private fun encodePreservingReserved(value: String): String =
    value.map { char ->
      if (char in RFC3986_RESERVED || char.isUnreserved()) char.toString()
      else urlEncode(char.toString())
    }.joinToString("")

  private fun Char.isUnreserved(): Boolean =
    this in 'A'..'Z' ||
    this in 'a'..'z' ||
    this in '0'..'9' ||
    this == '-' ||
    this == '.' ||
    this == '_' ||
    this == '~'

  private fun urlEncode(value: String): String = URLEncoder.encode(value, "UTF-8")
}