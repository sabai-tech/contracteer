package tech.sabai.contracteer.core.contract

data class Contract(
  val request: ContractRequest,
  val response: ContractResponse,
  val exampleKey: String? = null
) {

  fun hasExample(): Boolean = exampleKey!= null

  fun description(): String {
    val requestContentType = request.body?.contentType?.let { "($it)" } ?: ""
    val responseContentType = response.body?.contentType?.let { "($it)" } ?: ""
    val description =
      "${request.method.uppercase()} ${request.path} $requestContentType -> ${response.statusCode} $responseContentType"
    return exampleKey?.let { "$description with example '$it'" } ?: description
  }
}
