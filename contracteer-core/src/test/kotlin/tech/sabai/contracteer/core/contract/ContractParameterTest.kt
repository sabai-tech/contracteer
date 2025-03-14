package tech.sabai.contracteer.core.contract

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.TestFixture.booleanDataType
import tech.sabai.contracteer.core.TestFixture.integerDataType
import tech.sabai.contracteer.core.TestFixture.parameter
import tech.sabai.contracteer.core.TestFixture.stringDataType

class ParameterTest {

  @Test
  fun `creation succeeds without example`() {
    // when
    val result = ContractParameter.create(name = "prop1", dataType = integerDataType(), isRequired = true)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validation succeeds when string representation matches the datatype`() {
    // given
    val param = parameter("param", integerDataType())

    // when
    val result = param.validate("123")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validation fails for null string when not nullable`() {
    // given
    val param = parameter("param", booleanDataType(isNullable = false))

    // when
    val result = param.validate(null)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `validation succeeds for null string when is nullable`() {
    // given
    val param = parameter("param", booleanDataType(isNullable = true))

    // when
    val result = param.validate(null)

    // then
    assert(result.isSuccess())
  }

  @Nested
  inner class WithExample {

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

    @Test
    fun `validation succeeds when string representation is equal to example value`() {
      // given
      val param = parameter("param", integerDataType(), example = Example(1234))

      // when
      val result = param.validate("1234")

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `validation succeeds for null string when example value is null `() {
      // given
      val param = parameter("param", booleanDataType(isNullable = true), example = Example(null))

      // when
      val result = param.validate(null)

      // then
      assert(result.isSuccess())
    }
  }
}