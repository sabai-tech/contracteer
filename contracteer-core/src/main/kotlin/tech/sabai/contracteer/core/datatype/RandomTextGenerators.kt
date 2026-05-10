package tech.sabai.contracteer.core.datatype

private const val ALPHANUMERIC_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789"

/** Generates a string of lowercase alphanumeric characters whose length is randomly picked from [lengthRange]. */
internal fun randomAlphanumeric(lengthRange: IntRange): String =
  CharArray(lengthRange.random()) { ALPHANUMERIC_CHARS.random() }.concatToString()
