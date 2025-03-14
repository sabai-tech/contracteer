package tech.sabai.contracteer.core.contract

import org.junit.jupiter.api.Test

class ExampleTest {


  @Test
  fun `matches Example for null value`() {
    // given
    val example = Example(null)

    // when
    val result = example.validate(null)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `matches Example for simple value`() {
    // given
    val example = Example(1)

    // when
    val result = example.validate(1)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not match Example when simple value are different`() {
    // given
    val example = Example(1)

    // when
    val result = example.validate(2)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `array does not match Example if sizes are different`() {
    // given
    val example = Example(arrayOf(1, 2))

    // when
    val result = example.validate(arrayOf(1, 2, 42))

    // then
    assert(result.isFailure())
    assert(result.errors().size == 1)
    assert(result.errors().first().contains("size"))
  }

  @Test
  fun `array does not matche Example if items type are different`() {
    // given
    val example = Example(arrayOf("John", "Doe"))

    // when
    val result = example.validate(arrayOf(1, 2))

    // then
    assert(result.isFailure())
    assert(result.errors().size == 2)
    assert(listOf("0", "John", "1").all { it in result.errors()[0] })
    assert(listOf("1", "Doe", "2").all { it in result.errors()[1] })
  }

  @Test
  fun `array does not validate Example if item values are different`() {
    // given
    val example = Example(arrayOf("John", "Doe"))

    // when
    val result = example.validate(arrayOf("John", "42"))

    // then
    assert(result.isFailure())
    assert(result.errors().size == 1)
    assert(listOf("1", "Doe", "42").all { it in result.errors().first() })
  }

  @Test
  fun `array validate Example if item values are equal`() {
    // given
    val example = Example(arrayOf("John", "Doe"))

    // when
    val result = example.validate(arrayOf("John", "Doe"))

    // then
    assert(result.isSuccess())

  }

  @Test
  fun `array does not validate Example when sub array item values are different`() {
    // given
    val example = Example(arrayOf(
      arrayOf(1, 2),
      arrayOf(99, 101)))

    // when
    val result = example.validate(arrayOf(
      arrayOf(1, 2),
      arrayOf(42, 101))
    )

    // then
    assert(result.isFailure())
    assert(result.errors().size == 1)
    assert(listOf("0", "1", "42", "99").all { it in result.errors().first() })
  }

  @Test
  fun `array does not validate Example when some property of sub map item values are different`() {
    // given
    val example = Example(arrayOf(
      mapOf("id" to 1,
            "age" to 20),
      mapOf("id" to 2,
            "age" to 42)))

    // when
    val result = example.validate(arrayOf(
      mapOf("id" to 1,
            "age" to 20),
      mapOf("id" to 2,
            "age" to 99))
    )

    // then
    assert(result.isFailure())
    assert(result.errors().size == 1)
    assert(listOf("1", "age", "42", "99").all { it in result.errors().first() })
  }

  @Test
  fun `map does not validate Example when property names are not equal`() {
    // given
    val example = Example(mapOf(
      "name" to "John",
      "age" to 20,
    ))

    // when
    val result = example.validate(mapOf(
      "id" to "John",
      "address" to 20,
    ))

    // then
    assert(result.isFailure())
    assert(result.errors().size == 1)
    assert(result.errors().first().contains("Property names"))
  }

  @Test
  fun `map does not validate Example when property values are not the same type`() {
    // given
    val example = Example(mapOf(
      "name" to "John",
      "age" to 20,
    ))

    // when
    val result = example.validate(mapOf(
      "name" to "John",
      "age" to true,
    ))

    // then
    assert(result.isFailure())
    assert(result.errors().size == 1)
    assert(listOf("age", "20", "true").all { it in result.errors().first() })
  }

  @Test
  fun `map validates Example when properties are equal`() {
    // given
    val example = Example(mapOf(
      "name" to "John",
      "age" to 20
    ))

    // when
    val result = example.validate(mapOf(
      "name" to "John",
      "age" to 20
    ))

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `map does not validate Example when property values of sub map are not equals`() {
    // given
    val example = Example(mapOf(
      "user" to mapOf(
        "name" to "John",
        "age" to 20
      )))

    // when
    val result = example.validate(mapOf(
      "user" to mapOf(
        "name" to "John",
        "age" to 21
      )))

    // then
    assert(result.isFailure())
    assert(result.errors().size == 1)
    assert(listOf("user", "age", "20", "21").all { it in result.errors().first() })
  }

  @Test
  fun `map does not match Example when an item value of a property of type array are not equals`() {
    // given
    val example = Example(mapOf(
      "name" to "John",
      "productIds" to arrayOf(1, 2, 3, 4, 5, 6)

    ))

    // when
    val result = example.validate(mapOf(
      "name" to "John",
      "productIds" to arrayOf(1, 2, 42, 4, 5, 6)
    ))

    // then
    assert(result.isFailure())
    assert(result.errors().size == 1)
    assert(listOf("productIds", "2", "3", "42").all { it in result.errors().first() })
  }
}