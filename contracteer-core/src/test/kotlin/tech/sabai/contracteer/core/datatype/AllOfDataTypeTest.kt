package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Test

class AllOfDataTypeTest {
  private val dog = ObjectDataType(name = "dog",
                                   properties = mapOf("bark" to BooleanDataType(), "breed" to StringDataType()),
                                   requiredProperties = setOf("breed")
  )
  private val cat = ObjectDataType(properties = mapOf("hunts" to BooleanDataType(), "age" to IntegerDataType()),
                                   requiredProperties = setOf("hunts", "age")
  )

  @Test
  fun `do not validate when some of the sub datatype does not validate the value`() {
    // given
    val allOfDataType = AllOfDataType(subTypes = listOf(dog, cat))

    // when
    val result = allOfDataType.validate(mapOf("hunts" to true, "bark" to true))


    // then
    assert(result.isFailure())
    assert(result.errors().first().contains(Regex("dog|Inline Schema|breed|age")))
  }

  @Test
  fun `validate when all sub datatype validates the value`() {
    // given
    val allOfDataType = AllOfDataType(subTypes = listOf(dog, cat))

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
    val allOfDataType = AllOfDataType(subTypes = listOf(dog, cat))

    // when
    val randomValue = allOfDataType.randomValue()

    // then
    assert(allOfDataType.validate(randomValue).isSuccess())
  }
}