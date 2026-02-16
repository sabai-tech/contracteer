package tech.sabai.contracteer.verifier

import tech.sabai.contracteer.core.operation.ParameterElement

/** Identifies which request element was mutated in a [VerificationCase.TypeMismatch]. */
sealed class MutatedElement {

  /** A request parameter (path, query, header, or cookie) was mutated. */
  data class Parameter(val element: ParameterElement): MutatedElement()

  /** The request body was mutated. */
  data object Body: MutatedElement()
}
