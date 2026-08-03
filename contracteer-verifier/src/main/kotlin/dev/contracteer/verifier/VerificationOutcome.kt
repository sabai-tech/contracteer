package dev.contracteer.verifier

import dev.contracteer.core.Result

/** The result of verifying a single [VerificationCase] against a server. */
data class VerificationOutcome(
  val case: VerificationCase,
  val result: Result<Unit>
)
