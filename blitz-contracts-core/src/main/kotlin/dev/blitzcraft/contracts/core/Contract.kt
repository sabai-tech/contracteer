package dev.blitzcraft.contracts.core

data class Contract(
  val request: RequestContract,
  val response: ResponseContract,
  val exampleKey: String? = null
) {

  fun description(): String {
    val requestContentType = request.body?.contentType?.let { "($it)" } ?: ""
    val responseContentType = response.body?.contentType?.let { "($it)" } ?: ""
    val description =
      "${request.method.uppercase()} ${request.path} $requestContentType-> ${response.statusCode} $responseContentType"
    return exampleKey?.let { "$description with example $it" } ?: description
  }
}
