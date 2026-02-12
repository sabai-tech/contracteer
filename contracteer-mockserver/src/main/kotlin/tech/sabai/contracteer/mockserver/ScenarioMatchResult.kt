package tech.sabai.contracteer.mockserver

import tech.sabai.contracteer.core.operation.Scenario

internal sealed interface ScenarioMatchResult {
  data class SingleMatch(val scenario: Scenario) : ScenarioMatchResult
  data class Ambiguous(val scenarios: List<Scenario>) : ScenarioMatchResult
  data object NoMatch : ScenarioMatchResult
}
