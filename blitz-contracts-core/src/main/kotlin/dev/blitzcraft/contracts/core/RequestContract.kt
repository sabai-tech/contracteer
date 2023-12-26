package dev.blitzcraft.contracts.core

data class RequestContract(
  val method: String,
  val path: String,
  val pathParameters: List<Property> = emptyList(),
  val queryParameters: List<Property> = emptyList(),
  val headers: List<Property> = emptyList(),
  val cookies: List<Property> = emptyList(),
  val body: Body? = null
)


