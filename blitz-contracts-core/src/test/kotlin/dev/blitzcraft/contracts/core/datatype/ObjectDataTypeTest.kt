package dev.blitzcraft.contracts.core.datatype

import dev.blitzcraft.contracts.core.convert
import org.junit.jupiter.api.Test

class ObjectDataTypeTest {

  @Test
  fun `validates null value if it is nullable`() {
    // given
    val objectDataType = ObjectDataType(properties = mapOf("prop" to IntegerDataType()), isNullable = true)

    // when
    val result = objectDataType.validate(null)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate null value if it is not nullable`() {
    // given
    val objectDataType = ObjectDataType(properties = mapOf("prop" to IntegerDataType()), isNullable = false)

    // when
    val result = objectDataType.validate(null)

    // then
    assert(result.isSuccess().not())
  }

  @Test
  fun `does not validate value whose type is not Map`() {
    // given
    val objectDataType = ObjectDataType(properties = mapOf("prop" to IntegerDataType()))

    // when
    val result = objectDataType.validate(123)

    // then
    assert(result.isSuccess().not())
  }

  @Test
  fun `validates a value of type Map`() {
    // given
    val objectDataType = ObjectDataType(properties = mapOf("prop" to IntegerDataType()))

    // when
    val result = objectDataType.validate(mapOf("prop" to 1).convert())

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validates a map when a non required property is not present`() {
    // given
    val objectDataType = ObjectDataType(
      properties = mapOf(
        "prop" to IntegerDataType(),
        "prop2" to IntegerDataType()),
      requiredProperties = setOf("prop")
    )
    // when
    val result = objectDataType.validate(mapOf("prop" to 1).convert())

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validates a map when a non required and non nullable property is not present`() {
    // given
    val objectDataType = ObjectDataType(
      properties = mapOf(
        "prop" to IntegerDataType(),
        "prop2" to IntegerDataType(isNullable = false)
      ),
      requiredProperties = setOf("prop"))
    // when
    val result = objectDataType.validate(mapOf("prop" to 1).convert())

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate when a property is not of the right type`() {
    // given
    val objectDataType = ObjectDataType(properties = mapOf("prop" to IntegerDataType()))

    // when
    val result = objectDataType.validate(mapOf("prop" to true))

    // then
    assert(result.isSuccess().not())
  }

  @Test
  fun `does not validate when a non nullable property is null`() {
    // given
    val objectDataType = ObjectDataType(properties = mapOf(
      "prop" to IntegerDataType(isNullable = false),
      "prop2" to BooleanDataType()
    ))

    // when
    val result = objectDataType.validate(mapOf(
      "prop" to null,
      "prop2" to true))

    // then
    assert(result.isSuccess().not())
  }

  @Test
  fun `does not validate when a required property is missing`() {
    // given
    val objectDataType = ObjectDataType(
      properties = mapOf(
        "prop" to IntegerDataType(),
        "prop2" to BooleanDataType()),
      requiredProperties = setOf("prop"))

    // when
    val result = objectDataType.validate(mapOf("prop2" to true))

    // then
    assert(result.isSuccess().not())
    assert(result.errors().size == 1)
    assert(listOf("is required", "prop").all { result.errors().first().contains(it) })
  }
}