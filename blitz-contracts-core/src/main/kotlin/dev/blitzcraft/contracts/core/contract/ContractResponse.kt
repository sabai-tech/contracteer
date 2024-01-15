package dev.blitzcraft.contracts.core.contract

data class ContractResponse(
  val statusCode: Int,
  val headers: List<ContractParameter> = emptyList(),
  val body: Body? = null
) {
   fun hasBody(): Boolean = body!= null
}


