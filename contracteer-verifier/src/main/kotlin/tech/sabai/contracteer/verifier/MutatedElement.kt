package tech.sabai.contracteer.verifier

import tech.sabai.contracteer.core.operation.ParameterElement

sealed class MutatedElement {

  data class Parameter(val element: ParameterElement) : MutatedElement()

  data object Body : MutatedElement()
}
