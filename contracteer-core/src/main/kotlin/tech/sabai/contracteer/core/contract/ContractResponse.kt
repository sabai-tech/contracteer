package tech.sabai.contracteer.core.contract

data class ContractResponse(
  val statusCode: Int,
  val headers: List<ContractParameter> = emptyList(),
  val body: Body? = null
) {
  fun hasBody(): Boolean = body != null
  fun withBody(body: Body) = copy(body = body)
}


