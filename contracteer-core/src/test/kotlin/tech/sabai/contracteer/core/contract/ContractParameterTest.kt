package tech.sabai.contracteer.core.contract

import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.datatype.BooleanDataType
import tech.sabai.contracteer.core.datatype.IntegerDataType
import tech.sabai.contracteer.core.datatype.NumberDataType

class ContractParameterTest {
  @Test
  fun `string representing an integer matches Parameter of type integer`() {
    // given
    val param = ContractParameter("param", IntegerDataType.create().value!!)

    // when
    val result = "123".matches(param)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `string representing a boolean matches Parameter of type boolean`() {
    // given
    val param = ContractParameter("param", BooleanDataType.create().value!!)

    // when
    val result = "true".matches(param)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `string representing a decimal matches Parameter of type decimal`() {
    // given
    val param = ContractParameter("param", NumberDataType.create().value!!)

    // when
    val result = "1234.789".matches(param)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `string with integer value matches Example value`() {
    // given
    val param = ContractParameter("param", IntegerDataType.create().value!!, example = Example(1234))

    // when
    val result = "1234".matchesExample(param)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `string with boolean value matches Example value`() {
    // given
    val param = ContractParameter("param", BooleanDataType.create().value!!, example = Example(true))

    // when
    val result = "true".matchesExample(param)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `string with decimal value matches Example value`() {
    // given
    val param = ContractParameter("param", NumberDataType.create().value!!, example = Example(1234.56))

    // when
    val result = "1234.56".matchesExample(param)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `string value does not match example when it is not define`() {
    // given
    val param = ContractParameter("param", BooleanDataType.create().value!!)

    // when
    val result = "true".matchesExample(param)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `null string value does not match example of non nullable Contract Parameter`() {
    // given
    val param = ContractParameter("param", BooleanDataType.create(isNullable = false).value!!)

    // when
    val result = null.matchesExample(param)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `null string matches when example value is null `() {
    // given
    val param = ContractParameter("param", BooleanDataType.create(isNullable = true).value!!, example = Example(null))

    // when
    val result = null.matchesExample(param)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `null string matches nullable Contract Parameter`() {
    // given
    val param = ContractParameter("param", BooleanDataType.create(isNullable = true).value!!)

    // when
    val result = null.matches(param)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `null string does not match a non nullable Contract Parameter`() {
    // given
    val param = ContractParameter("param", BooleanDataType.create(isNullable = false).value!!)

    // when
    val result = null.matches(param)

    // then
    assert(!result.isSuccess())
  }
}