package tech.sabai.contracteer.verifier

/** Connection configuration for [ServerVerifier]. */
data class ServerConfiguration(
  val baseUrl: String = "http://localhost",
  val port: Int = 8080
)
