package tech.sabai.contracteer.core.contract

import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.TestFixture.integerDataType
import tech.sabai.contracteer.core.TestFixture.stringDataType

class ContractParameterTest {

  @Test
  fun `creation succeeds without example`() {
    // when
    val result = ContractParameter.create(name = "prop1", dataType = integerDataType(), isRequired = true)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `creation fails when example is valid`() {
      // when
      val result = ContractParameter.create(name = "prop1", dataType = stringDataType(), example = Example(42))

      // then
      assert(result.isFailure())
    }

    @Test
    fun `creation fails for example value null when datatype is not nullable`() {
      // when
      val result = ContractParameter.create(name = "prop1",
                                            dataType = stringDataType(isNullable = false),
                                            example = Example(null))

      // then
      assert(result.isFailure())
    }

    @Test
    fun `creation succeeds when example is valid`() {
      // when
      val result = ContractParameter.create(name = "prop1", dataType = integerDataType(), example = Example(42))

      // then
      assert(result.isSuccess())
    }
}