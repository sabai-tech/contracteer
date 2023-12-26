package dev.blitzcraft.contracts.core

data class ResponseContract(
  val statusCode: Int,
  val headers: List<Property> = emptyList(),
  val body: Body? = null
)




