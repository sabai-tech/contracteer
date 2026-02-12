package tech.sabai.contracteer.verifier

import tech.sabai.contracteer.core.Result

data class VerificationOutcome(
  val case: VerificationCase,
  val result: Result<Unit>
)
