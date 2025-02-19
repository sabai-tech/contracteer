package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.DataTypeFixture.anyOfDataType
import tech.sabai.contracteer.core.DataTypeFixture.booleanDataType
import tech.sabai.contracteer.core.DataTypeFixture.integerDataType
import tech.sabai.contracteer.core.DataTypeFixture.objectDataType
import tech.sabai.contracteer.core.DataTypeFixture.stringDataType
import tech.sabai.contracteer.core.normalize

class AnyOfDataTypeTest {

  private val dog = objectDataType(name = "dog",
                                   properties = mapOf("bark" to booleanDataType(),
                                                      "breed" to stringDataType(),
                                                      "type" to stringDataType()),
                                   requiredProperties = setOf("breed", "type"))

  private val cat = objectDataType(name = "cat",
                                   properties = mapOf("hunts" to booleanDataType(),
                                                      "age" to integerDataType(),
                                                      "type" to stringDataType()),
                                   requiredProperties = setOf("hunts", "age", "type"))

  @Test
  fun `do not validate when none of the sub datatype validates the value`() {
    // given
    val anyOfDataType = anyOfDataType(subTypes = listOf(dog, cat))

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
    val anyOfDataType = anyOfDataType(subTypes = listOf(dog, cat))

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
    val anyOfDataType = anyOfDataType(subTypes = listOf(dog, cat))

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
    val anyOfDataType = anyOfDataType(subTypes = listOf(dog, cat), discriminator = Discriminator("type"))

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
    val anyOfDataType = anyOfDataType(
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
    val anyOfDataType = anyOfDataType(subTypes = listOf(dog, cat))

    // when
    val randomValue = anyOfDataType.randomValue()

    // then
    assert(anyOfDataType.validate(randomValue).isSuccess())
  }

  @Test
  fun `generate a random value with the right discriminating value`() {
    // given
    val anyOfDataType = anyOfDataType(
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

  @Test
  fun `validates with enum values`() {
    // given
    val anyOfDataType = anyOfDataType(
      subTypes = listOf(dog, cat),
      enum = listOf(
        mapOf("bark" to true,
              "breed" to "breed",
              "type" to "dog"),
        mapOf("hunts" to true,
              "age" to 2,
              "type" to "cat"
        )
      )
    )

    // when
    val result = anyOfDataType.validate(mapOf("bark" to true,
                                              "breed" to "breed",
                                              "type" to "dog"))

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate a string representing a date with enum values`() {
    // given
    val anyOfDataType = anyOfDataType(
      subTypes = listOf(dog, cat),
      enum = listOf(
        mapOf("bark" to true,
              "breed" to "breed",
              "type" to "dog"),
        mapOf("hunts" to true,
              "age" to 2,
              "type" to "cat"
        )
      )
    )

    // when
    val result = anyOfDataType.validate(mapOf("hunts" to false,
                                              "age" to 2,
                                              "type" to "cat"))

    // then
    assert(result.isFailure())
  }

  @Test
  fun `generates random value with enum values`() {
    // given
    val enum = listOf(
      mapOf("bark" to true,
            "breed" to "breed",
            "type" to "dog"),
      mapOf("hunts" to true,
            "age" to 2,
            "type" to "cat"
      )
    )
    val anyOfDataType = anyOfDataType(subTypes = listOf(dog, cat), enum = enum)

    // when
    val result = anyOfDataType.randomValue()

    // then
    assert(enum.map { obj -> obj.map { it.key to it.value.normalize() }.toMap() }.contains(result))
  }
}