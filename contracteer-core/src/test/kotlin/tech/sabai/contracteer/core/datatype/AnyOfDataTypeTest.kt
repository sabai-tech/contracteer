package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Test

class AnyOfDataTypeTest {

  private val dog = ObjectDataType(name = "dog",
                                   properties = mapOf("bark" to BooleanDataType(),
                                                      "breed" to StringDataType(),
                                                      "type" to StringDataType()),
                                   requiredProperties = setOf("breed", "type")
  )
  private val cat = ObjectDataType(name = "cat",
                                   properties = mapOf("hunts" to BooleanDataType(),
                                                      "age" to IntegerDataType(),
                                                      "type" to StringDataType()),
                                   requiredProperties = setOf("hunts", "age", "type")
  )

  @Test
  fun `do not validate when none of the sub datatype validates the value`() {
    // given
    val anyOfDataType = AnyOfDataType(subTypes = listOf(dog, cat))

    // when
    val result = anyOfDataType.validate(mapOf("hunts" to true, "bark" to true))


    // then
    assert(result.isFailure())
    assert(result.errors().first().contains("dog"))
    assert(result.errors().first().contains("cat"))
  }

  @Test
  fun `validate when more than one sub datatype validates the value`() {
    // given
    val anyOfDataType = AnyOfDataType(subTypes = listOf(dog, cat))

    // when
    val result = anyOfDataType.validate(mapOf(
      "breed" to "breed",
      "hunts" to true,
      "age" to 1,
      "type" to "dog")
    )

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validate when a sub type validates the value`() {
    // given
    val anyOfDataType = AnyOfDataType(subTypes = listOf(dog, cat))

    // when
    val result = anyOfDataType.validate(mapOf(
      "bark" to true,
      "breed" to "breed",
      "type" to "dog")
    )

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `do not validate when discriminator property has a wrong value`() {
    // given
    val anyOfDataType = AnyOfDataType(subTypes = listOf(dog, cat), discriminator = Discriminator("type"))

    // when
    val result = anyOfDataType.validate(mapOf("bark" to true,
                                              "breed" to "breed",
                                              "type" to "pet")
    )

    // then
    assert(result.isFailure())
  }

  @Test
  fun `validate when discriminator property is equal to a mapping value `() {
    // given
    val anyOfDataType = AnyOfDataType(
      subTypes = listOf(dog, cat),
      discriminator = Discriminator("type", mapOf("BigDog" to dog))
    )

    // when
    val result = anyOfDataType.validate(mapOf("bark" to true,
                                              "breed" to "breed",
                                              "type" to "BigDog")
    )

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `should generate a random value`() {
    // given
    val anyOfDataType = AnyOfDataType(subTypes = listOf(dog, cat))

    // when
    val randomValue = anyOfDataType.randomValue()

    // then
    assert(anyOfDataType.validate(randomValue).isSuccess())
  }

  @Test
  fun `generate a random value with the right discriminating value`() {
    // given
    val anyOfDataType = AnyOfDataType(
      subTypes = listOf(dog, cat),
      discriminator = Discriminator("type", mapOf("BigDog" to dog))
    )

    // when
    val randomValue = anyOfDataType.randomValue()

    // then
    assert(
      (randomValue["type"] == "BigDog" && randomValue["bark"] is Boolean && randomValue["breed"] is String)
      ||
      (randomValue["type"] == "cat" && randomValue["hunts"] is Boolean && randomValue["age"] is Number)
    )
  }
}