package tech.sabai.contracteer.core

import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.Result.Companion.failure
import tech.sabai.contracteer.core.Result.Companion.success

class ResultPropertyPathTest {

  @Test
  fun `forProperty prepends property name to error path`() {
    // when
    val errors = failure<Any>("Wrong Type").forProperty("name").assertFailure()

    // then
    assert(errors.first() == "'name': Wrong Type")
  }

  @Test
  fun `forProperty builds dotted path for nested properties`() {
    // when
    val errors = failure<Any>("Wrong Type")
      .forProperty("street")
      .forProperty("address")
      .assertFailure()

    // then
    assert(errors.first() == "'address.street': Wrong Type")
  }

  @Test
  fun `forIndex prepends array index to error path`() {
    // when
    val errors = failure<Any>("Wrong Type").forIndex(0).assertFailure()

    // then
    assert(errors.first() == "'[0]': Wrong Type")
  }

  @Test
  fun `forIndex on property builds bracketed index before dotted property`() {
    // when
    val errors = failure<Any>("prop1", "Wrong Type").forIndex(1).assertFailure()

    // then
    assert(errors.first() == "'[1].prop1': Wrong Type")
  }

  @Test
  fun `forProperty and forIndex build mixed path`() {
    // when
    val errors = failure<Any>("Wrong Type")
      .forProperty("name")
      .forIndex(0)
      .forProperty("items")
      .assertFailure()

    // then
    assert(errors.first() == "'items[0].name': Wrong Type")
  }

  @Test
  fun `nested forIndex builds consecutive bracket indices`() {
    // when
    val errors = failure<Any>("Wrong Type")
      .forIndex(1)
      .forIndex(0)
      .assertFailure()

    // then
    assert(errors.first() == "'[0][1]': Wrong Type")
  }

  @Test
  fun `forProperty on success preserves value`() {
    // when
    val result = success(42).forProperty("name")

    // then
    val value = result.assertSuccess()
    assert(value == 42)
  }

  @Test
  fun `forIndex on success preserves value`() {
    // when
    val result = success(42).forIndex(0)

    // then
    val value = result.assertSuccess()
    assert(value == 42)
  }
}