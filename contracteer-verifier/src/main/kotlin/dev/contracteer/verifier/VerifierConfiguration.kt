package dev.contracteer.verifier

import java.net.URI

/** Configuration for [OpenApiVerifier]. */
data class VerifierConfiguration(
  val baseUrl: String = "http://localhost:8080"
) {
  init {
    val uri = runCatching { URI(baseUrl) }.getOrElse { throw IllegalArgumentException("Invalid baseUrl: $baseUrl", it) }

    require(uri.isAbsolute && uri.scheme in setOf("http", "https")) {
      "baseUrl must be an absolute http(s) URL: $baseUrl"
    }
    require(uri.rawQuery == null) {
      "baseUrl must not contain a query string: $baseUrl"
    }
    require(uri.rawFragment == null) {
      "baseUrl must not contain a fragment: $baseUrl"
    }
  }
}
