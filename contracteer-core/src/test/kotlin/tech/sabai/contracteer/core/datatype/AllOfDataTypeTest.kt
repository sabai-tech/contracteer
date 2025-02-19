package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.DataTypeFixture.allOfDataType
import tech.sabai.contracteer.core.DataTypeFixture.booleanDataType
import tech.sabai.contracteer.core.DataTypeFixture.integerDataType
import tech.sabai.contracteer.core.DataTypeFixture.objectDataType
import tech.sabai.contracteer.core.DataTypeFixture.stringDataType
import tech.sabai.contracteer.core.normalize

class AllOfDataTypeTest {
  private val dog = objectDataType(name = "dog",
                                   properties = mapOf("bark" to booleanDataType(),
                                                      "breed" to stringDataType()),
                                   requiredProperties = setOf("breed"))

  private val cat = objectDataType(properties = mapOf("hunts" to booleanDataType(),
                                                      "age" to integerDataType()),
                                   requiredProperties = setOf("hunts", "age"))

  @Test
  fun `do not validate when some of the sub datatype does not validate the value`() {
    // given
    val allOfDataType = allOfDataType(subTypes = listOf(dog, cat))

    // when
    val result = allOfDataType.validate(mapOf("hunts" to true, "bark" to true))

    // then
    assert(result.isFailure())
    assert(result.errors().first().contains(Regex("dog|Inline Schema|breed|age")))
  }

  @Test
  fun `validate when all sub datatype validates the value`() {
    // given
    val allOfDataType = allOfDataType(subTypes = listOf(dog, cat))

    // when
    val result = allOfDataType.validate(mapOf(
      "breed" to "breed",
      "hunts" to true,
      "age" to 1)
    )

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `should generate a random value`() {
    // given
    val allOfDataType = allOfDataType(subTypes = listOf(dog, cat))

    // when
    val randomValue = allOfDataType.randomValue()

    // then
    assert(allOfDataType.validate(randomValue).isSuccess())
  }

  @Test
  fun `validates with enum values`() {
    // given
    val allOfDataType = allOfDataType(
      subTypes = listOf(dog, cat),
      enum = listOf(
        mapOf("bark" to true, "hunts" to true, "breed" to "breed", "age" to 1),
        mapOf("hunts" to true, "age" to 2, "breed" to "breed"),
      ))

    // when
    val result = allOfDataType.validate(mapOf("hunts" to true, "age" to 2, "breed" to "breed"))

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate with enum values`() {
    // given
    val allOfDataType = allOfDataType(
      subTypes = listOf(dog, cat),
      enum = listOf(
        mapOf("bark" to true, "hunts" to true, "breed" to "breed", "age" to 1),
        mapOf("hunts" to true, "age" to 2, "breed" to "breed"),
      ))

    // when
    val result = allOfDataType.validate(mapOf("hunts" to true, "age" to 3, "breed" to "breed"))

    // then
    assert(result.isFailure())
  }

  @Test
  fun `generates random value with enum values`() {
    // given
    val enum = listOf(
      mapOf("bark" to true, "hunts" to true, "breed" to "breed", "age" to 1),
      mapOf("hunts" to true, "age" to 2, "breed" to "breed"),
    )
    val dateDataType = allOfDataType(subTypes = listOf(dog, cat), enum = enum)

    // when
    val result = dateDataType.randomValue()

    // then
    assert(enum.map { obj -> obj.map { it.key to it.value.normalize() }.toMap() }.contains(result))
  }
}