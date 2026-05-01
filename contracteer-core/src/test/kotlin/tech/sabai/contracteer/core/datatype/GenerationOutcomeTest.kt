package tech.sabai.contracteer.core.datatype

import tech.sabai.contracteer.core.Result.Failure
import tech.sabai.contracteer.core.Result.Success
import tech.sabai.contracteer.core.datatype.GenerationOutcome.Boundary
import tech.sabai.contracteer.core.datatype.GenerationOutcome.Reason
import tech.sabai.contracteer.core.datatype.GenerationOutcome.Value
import kotlin.test.Test

class GenerationOutcomeTest {

  @Test
  fun `toResult maps Value to Success with the carried value, including null`() {
    // given
    val value: GenerationOutcome<String> = Value(null)

    // when
    val result = value.toResult()

    // then
    assert(result is Success && result.value == null)
  }

  @Test
  fun `toResult on a CYCLE boundary renders the CYCLE explanation at the boundary path`() {
    // given
    val boundary: GenerationOutcome<String> = Boundary(Reason.CYCLE, path = "friend.friend")

    // when
    val errorMessage = boundary.toResult().errors().single()

    // then
    assert(boundary.toResult() is Failure)
    assert(errorMessage == "'friend.friend': ${Reason.CYCLE.explanation()}")
  }

  @Test
  fun `toResult on a DEPTH boundary renders the DEPTH explanation at the boundary path`() {
    // given
    val boundary: GenerationOutcome<String> = Boundary(Reason.DEPTH, path = "outer.inner")

    // when
    val errorMessage = boundary.toResult().errors().single()

    // then
    assert(boundary.toResult() is Failure)
    assert(errorMessage == "'outer.inner': ${Reason.DEPTH.explanation()}")
  }

  @Test
  fun `toResult on a NODES boundary renders the NODES explanation at the boundary path`() {
    // given
    val boundary: GenerationOutcome<String> = Boundary(Reason.NODES, path = "items.[3]")

    // when
    val errorMessage = boundary.toResult().errors().single()

    // then
    assert(boundary.toResult() is Failure)
    assert(errorMessage == "'items.[3]': ${Reason.NODES.explanation()}")
  }
}