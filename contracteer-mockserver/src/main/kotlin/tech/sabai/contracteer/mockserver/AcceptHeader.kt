package tech.sabai.contracteer.mockserver

import tech.sabai.contracteer.core.operation.ContentType

/**
 * Parsed representation of an HTTP `Accept` header.
 *
 * Supports multiple media types with quality factors per RFC 7231.
 */
class AcceptHeader private constructor(val mediaTypes: List<MediaRange>) {

  fun acceptsAny() = mediaTypes.isEmpty() || mediaTypes.any { it.type == "*/*" && it.quality > 0.0 }

  fun bestMatch(candidates: List<ContentType>): ContentType? =
    candidates
      .mapNotNull { candidate -> matchingQuality(candidate)?.let { q -> candidate to q } }
      .maxByOrNull { it.second }
      ?.first

  private fun matchingQuality(candidate: ContentType): Double? =
    mediaTypes
      .filter { ContentType(it.type).validate(candidate.value).isSuccess() && it.quality > 0.0 }
      .maxByOrNull { it.quality * specificity(it.type) }
      ?.quality

  private fun specificity(type: String): Double = when {
    type == "*/*"       -> 0.001
    type.endsWith("/*") -> 0.01
    else                -> 1.0
  }

  data class MediaRange(val type: String, val quality: Double)

  companion object {
    fun parse(header: String?): AcceptHeader {
      if (header.isNullOrBlank()) return AcceptHeader(emptyList())

      val mediaTypes = header
        .split(",")
        .map { part ->
          val segments = part.trim().split(";")
          val type = segments.first().trim()
          val quality = segments
                          .drop(1)
                          .map { it.trim() }
                          .firstOrNull { it.startsWith("q=") }
                          ?.removePrefix("q=")
                          ?.toDoubleOrNull()
                        ?: 1.0
          MediaRange(type, quality)
        }

      return AcceptHeader(mediaTypes)
    }
  }
}