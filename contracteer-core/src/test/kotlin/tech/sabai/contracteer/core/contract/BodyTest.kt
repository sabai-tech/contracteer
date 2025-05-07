package tech.sabai.contracteer.core.contract

import tech.sabai.contracteer.core.TestFixture.integerDataType
import kotlin.test.Test

class BodyTest {

  @Test
  fun `creation fails when content-type and datatype are not compatible`() {
    // when
    val result = Body.create(ContentType("application/json"), dataType = integerDataType())

    // then
    assert(result.isFailure())
  }
}
