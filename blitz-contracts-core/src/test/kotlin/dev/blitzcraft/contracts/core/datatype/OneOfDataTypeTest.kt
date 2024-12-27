package dev.blitzcraft.contracts.core.datatype

import dev.blitzcraft.contracts.core.convert
import org.junit.jupiter.api.Test

class OneOfDataTypeTest {
  private val dog = ObjectDataType(name = "dog",
                                   properties = mapOf("bark" to BooleanDataType(), "breed" to StringDataType()),
                                   requiredProperties = setOf("breed", "dog")
  )
  private val cat = ObjectDataType(properties = mapOf("hunts" to BooleanDataType(), "age" to IntegerDataType()),
                                   requiredProperties = setOf("hunts", "age")
  )

  @Test
  fun `do not validate when none of the sub datatype validates the value`() {
    // given
    val oneOfDataType = OneOfDataType(objectDataTypes = listOf(dog, cat))

    // when
    val result = oneOfDataType.validate(mapOf("hunts" to true, "bark" to true).convert())

    // then
    assert(result.isSuccess().not())
    assert(result.errors().first().contains("dog"))
    assert(result.errors().first().contains("Inline Schema"))
  }

  @Test
  fun `do not validate when more than one sub datatype validates the value`() {
    // given
    val oneOfDataType = OneOfDataType(objectDataTypes = listOf(dog, cat))

    // when
    val result = oneOfDataType.validate(mapOf(
      "bar" to true,
      "breed" to "breed",
      "hunts" to true,
      "age" to 1.convert())
    )

    // then
    assert(result.isSuccess().not())
    assert(result.errors().first().contains(Regex("dog|Inline Schema|breed|age")))
  }

  @Test
  fun `validate when a sub type validates the value`() {
    // given
    val oneOfDataType = OneOfDataType(objectDataTypes = listOf(dog, cat))

    // when
    val result = oneOfDataType.validate(mapOf(
      "bar" to true,
      "breed" to "breed")
    )

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `should generate a random value`() {
    // given
    val oneOfDataType = OneOfDataType(objectDataTypes = listOf(dog, cat))

    // when
    val randomValue = oneOfDataType.randomValue()

    // then
    assert(oneOfDataType.validate(randomValue).isSuccess())
  }
}