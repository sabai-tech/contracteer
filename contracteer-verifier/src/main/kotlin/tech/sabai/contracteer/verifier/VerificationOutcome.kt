package tech.sabai.contracteer.verifier

import tech.sabai.contracteer.core.Result

/** The result of verifying a single [VerificationCase] against a server. */
data class VerificationOutcome(
  val case: VerificationCase,
  val result: Result<Unit>
)
