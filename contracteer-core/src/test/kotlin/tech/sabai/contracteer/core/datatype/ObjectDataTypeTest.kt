package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.DataTypeFixture.booleanDataType
import tech.sabai.contracteer.core.DataTypeFixture.integerDataType
import tech.sabai.contracteer.core.DataTypeFixture.objectDataType
import tech.sabai.contracteer.core.DataTypeFixture.stringDataType

class ObjectDataTypeTest {

  @Test
  fun `validates null value if it is nullable`() {
    // given
    val objectDataType = objectDataType(properties = mapOf("prop" to integerDataType()), isNullable = true)

    // when
    val result = objectDataType.validate(null)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate null value if it is not nullable`() {
    // given
    val objectDataType = objectDataType(properties = mapOf("prop" to integerDataType()), isNullable = false)

    // when
    val result = objectDataType.validate(null)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `does not validate value whose type is not Map`() {
    // given
    val objectDataType = objectDataType(properties = mapOf("prop" to integerDataType()))

    // when
    val result = objectDataType.validate(123)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `validates a value of type Map`() {
    // given
    val objectDataType = objectDataType(properties = mapOf("prop" to integerDataType()))

    // when
    val result = objectDataType.validate(mapOf("prop" to 1))

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validates a map when a non required property is not present`() {
    // given
    val objectDataType = objectDataType(
      properties = mapOf(
        "prop" to integerDataType(),
        "prop2" to integerDataType()),
      requiredProperties = setOf("prop")
    )
    // when
    val result = objectDataType.validate(mapOf("prop" to 1))

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validates a map when a non required and non nullable property is not present`() {
    // given
    val objectDataType = objectDataType(
      properties = mapOf(
        "prop" to integerDataType(),
        "prop2" to integerDataType(isNullable = false)
      ),
      requiredProperties = setOf("prop"))
    // when
    val result = objectDataType.validate(mapOf("prop" to 1))

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate when a property is not of the right type`() {
    // given
    val objectDataType = objectDataType(properties = mapOf("prop" to integerDataType()))

    // when
    val result = objectDataType.validate(mapOf("prop" to true))

    // then
    assert(result.isFailure())
  }

  @Test
  fun `does not validate when a non nullable property is null`() {
    // given
    val objectDataType = objectDataType(properties = mapOf(
      "prop" to integerDataType(isNullable = false),
      "prop2" to booleanDataType()
    ))

    // when
    val result = objectDataType.validate(mapOf(
      "prop" to null,
      "prop2" to true))

    // then
    assert(result.isFailure())
  }

  @Test
  fun `does not validate when a required property is missing`() {
    // given
    val objectDataType = objectDataType(
      properties = mapOf(
        "prop" to integerDataType(),
        "prop2" to booleanDataType()),
      requiredProperties = setOf("prop"))

    // when
    val result = objectDataType.validate(mapOf("prop2" to true))

    // then
    assert(result.isFailure())
    assert(result.errors().size == 1)
    assert(listOf("is required", "prop").all { result.errors().first().contains(it) })
  }

  @Test
  fun `validates a Map with enum values`() {
    // given
    val objectDataType = objectDataType(properties = mapOf("prop" to integerDataType()),
                                        enum = listOf(mapOf("prop" to 1), mapOf("prop" to 2)))

    // when
    val result = objectDataType.validate(mapOf("prop" to 1))

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate a Map with enum values`() {
    // given
    val objectDataType = objectDataType(properties = mapOf("prop" to integerDataType()),
                                        enum = listOf(mapOf("prop" to 1), mapOf("prop" to 2)))

    // when
    val result = objectDataType.validate(mapOf("john" to 5))

    // then
    assert(result.isFailure())
  }

  @Test
  fun `generates random value with enum values`() {
    // given
    val enum = listOf(mapOf("prop" to "value1"), mapOf("prop" to "value2"))
    val objectDataType = objectDataType(properties = mapOf("prop" to stringDataType()), enum = enum)

    // when
    val result = objectDataType.randomValue()

    // then
    assert(enum.contains(result))
  }
}