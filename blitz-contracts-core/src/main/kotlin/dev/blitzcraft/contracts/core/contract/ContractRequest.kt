package dev.blitzcraft.contracts.core.contract

data class ContractRequest(
  val method: String,
  val path: String,
  val pathParameters: List<PathParameter> = emptyList(),
  val queryParameters: List<ContractParameter> = emptyList(),
  val headers: List<ContractParameter> = emptyList(),
  val cookies: List<ContractParameter> = emptyList(),
  val body: Body? = null
) {
  fun withBody(body: Body) = copy(body = body)
}



