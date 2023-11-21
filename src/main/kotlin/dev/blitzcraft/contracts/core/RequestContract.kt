package dev.blitzcraft.contracts.core

data class RequestContract(
  val method: String,
  val path: String,
  val pathParameters: Map<String, Property> = emptyMap(),
  val queryParameters: Map<String, Property> = emptyMap(),
  val headers: Map<String, Property> = emptyMap(),
  val cookies: Map<String, Property> = emptyMap(),
  val body: Body? = null
)


