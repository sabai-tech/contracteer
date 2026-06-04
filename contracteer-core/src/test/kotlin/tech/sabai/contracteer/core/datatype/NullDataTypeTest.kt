package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.Result.Success
import tech.sabai.contracteer.core.datatype.GenerationOutcome.Value

class NullDataTypeTest {

  @Test
  fun `is nullable`() {
    // when
    val isNullable = NullDataType.isNullable

    // then
    assert(isNullable)
  }

  @Test
  fun `exposes 'null' as its OpenAPI type`() {
    // when
    val openApiType = NullDataType.openApiType

    // then
    assert(openApiType == "null")
  }

  @Test
  fun `uses Unit as a sentinel runtime class that no parsed JSON value matches`() {
    // when
    val dataTypeClass = NullDataType.dataTypeClass

    // then
    assert(dataTypeClass == Unit::class.java)
  }

  @Test
  fun `validates null value`() {
    // when
    val result = NullDataType.validate(null)

    // then
    assert(result is Success)
    assert((result as Success).value == null)
  }

  @Test
  fun `does not validate non-null string value`() {
    // when
    val result = NullDataType.validate("hello")

    // then
    assert(result.isFailure())
    assert(result.errors().first().contains("expected type 'null'"))
  }

  @Test
  fun `does not validate non-null number value`() {
    // when
    val result = NullDataType.validate(42)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `random value generation yields null`() {
    // when
    val outcome = NullDataType.randomValue(GenerationContext.default())

    // then
    assert(outcome is Value && outcome.value == null)
  }
}
