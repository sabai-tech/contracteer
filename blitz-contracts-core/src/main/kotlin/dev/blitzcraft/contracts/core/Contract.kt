package dev.blitzcraft.contracts.core

data class Contract(
  val request: RequestContract,
  val response: ResponseContract,
  val exampleKey: String? = null
) {

  fun description() =
    if (exampleKey != null) "${request.method.uppercase()}: ${request.path}  with example $exampleKey"
    else "${request.method.uppercase()}: ${request.path}"
}