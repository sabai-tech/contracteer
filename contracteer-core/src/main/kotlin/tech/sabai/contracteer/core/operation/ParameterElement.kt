package tech.sabai.contracteer.core.operation

sealed class ParameterElement {
  abstract val name: String

  data class PathParam(override val name: String) : ParameterElement()
  data class QueryParam(override val name: String) : ParameterElement()
  data class Header(override val name: String) : ParameterElement()
  data class Cookie(override val name: String) : ParameterElement()
}
