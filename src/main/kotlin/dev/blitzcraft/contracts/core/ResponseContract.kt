package dev.blitzcraft.contracts.core

data class ResponseContract(
  val statusCode: Int,
  val headers: Map<String, Property> = emptyMap(),
  val body: Body? = null
)




