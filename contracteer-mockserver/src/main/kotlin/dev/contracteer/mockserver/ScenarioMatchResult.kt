package dev.contracteer.mockserver

import dev.contracteer.core.operation.Scenario

internal sealed interface ScenarioMatchResult {
  data class SingleMatch(val scenario: Scenario) : ScenarioMatchResult
  data class Ambiguous(val scenarios: List<Scenario>) : ScenarioMatchResult
  data object NoMatch : ScenarioMatchResult
}
