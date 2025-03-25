package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.TestFixture.booleanDataType
import tech.sabai.contracteer.core.TestFixture.integerDataType
import tech.sabai.contracteer.core.TestFixture.objectDataType
import tech.sabai.contracteer.core.TestFixture.stringDataType

class ObjectDataTypeTest {

  @Test
  fun `creation fails when a required property is not defined as a property`() {
    // when
    val result = ObjectDataType.create(name = "cat",
                                       properties = mapOf("hunts" to booleanDataType(),
                                                          "age" to integerDataType()),
                                       allowAdditionalProperties = false,
                                       isNullable = false,
                                       requiredProperties = setOf("hunts", "age", "type"))
    // then
    assert(result.isFailure())
    assert(result.errors().first().contains("'type'"))
  }

  @Test
  fun `validation succeeds for a null value when nullable`() {
    // given
    val objectDataType = objectDataType(properties = mapOf("prop" to integerDataType()), isNullable = true)

    // when
    val result = objectDataType.validate(null)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validation fails for a null value when not nullable`() {
    // given
    val objectDataType = objectDataType(properties = mapOf("prop" to integerDataType()), isNullable = false)

    // when
    val result = objectDataType.validate(null)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `validation fails when a value is not of type Map`() {
    // given
    val objectDataType = objectDataType(properties = mapOf("prop" to integerDataType()))

    // when
    val result = objectDataType.validate(123)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `validation succeeds when a value is of type Map`() {
    // given
    val objectDataType = objectDataType(properties = mapOf("prop" to integerDataType()))

    // when
    val result = objectDataType.validate(mapOf("prop" to 1))

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validation succeeds when a non required property is not present`() {
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
  fun `validation succeeds when a non required and non nullable property is not present`() {
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
  fun `validation fails when a property is not of the expected type`() {
    // given
    val objectDataType = objectDataType(properties = mapOf("prop" to integerDataType()))

    // when
    val result = objectDataType.validate(mapOf("prop" to true))

    // then
    assert(result.isFailure())
  }

  @Test
  fun `validation fails when a non nullable property is null`() {
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
  fun `validation fails when a required property is missing`() {
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

  @Nested
  inner class WithAdditionalProperties {

    @Test
    fun `validation fails when extra properties are provided and additionalProperties is disabled`() {
      // given
      val objectDataType =
        objectDataType(properties = mapOf("prop" to integerDataType()), allowAdditionalProperties = false)

      // when
      val result = objectDataType.validate(mapOf("prop" to 1, "prop2" to 2, "prop3" to 3))

      // then
      assert(result.isFailure())
    }

    @Test
    fun `validation fails when extra properties are not of the expected type`() {
      // given
      val objectDataType = objectDataType(
        properties = mapOf("prop" to integerDataType()),
        allowAdditionalProperties = true,
        additionalPropertiesDataType = stringDataType())

      // when
      val result = objectDataType.validate(mapOf("prop" to 1, "prop2" to true, "prop3" to 3.5))

      // then
      assert(result.isFailure())
    }

    @Test
    fun `validation succeeds when extra properties datatype is not specified`() {
      // given
      val objectDataType = objectDataType(
        properties = mapOf("prop" to integerDataType()),
        allowAdditionalProperties = true)

      // when
      val result = objectDataType.validate(mapOf("prop" to 1, "prop2" to true, "prop3" to 3.5))

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `validation succeeds when allow additional properties is true but there is no extra properties`() {
      // given
      val objectDataType = objectDataType(
        properties = mapOf("prop" to integerDataType()),
        allowAdditionalProperties = true)

      // when
      val result = objectDataType.validate(mapOf("prop" to 1))

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `validation succeeds when allow additional properties datatype is specified but there is no extra properties`() {
      // given
      val objectDataType = objectDataType(
        properties = mapOf("prop" to integerDataType()),
        allowAdditionalProperties = true,
        additionalPropertiesDataType = stringDataType())

      // when
      val result = objectDataType.validate(mapOf("prop" to 1))

      // then
      assert(result.isSuccess())
    }
  }


  @Nested
  inner class WithEnum {

    @Test
    fun `creation fails when enum contains a value that does not match any provided sub datatype`() {
      // when
      val result = ObjectDataType.create(name = "object",
                                         properties = mapOf("prop" to integerDataType(), "prop2" to integerDataType()),
                                         requiredProperties = setOf("prop2"),
                                         allowAdditionalProperties = true,
                                         isNullable = false,
                                         enum = listOf(mapOf("prop" to 1, "prop2" to "2"), mapOf("prop" to 2)))

      // then
      assert(result.isFailure())
    }

    @Test
    fun `validation succeeds when the value is included in the enum`() {
      // given
      val objectDataType = objectDataType(properties = mapOf("prop" to integerDataType()),
                                          enum = listOf(mapOf("prop" to 1), mapOf("prop" to 2)))

      // when
      val result = objectDataType.validate(mapOf("prop" to 1))

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `validation fails when the value is not included in the enum`() {
      // given
      val objectDataType = objectDataType(properties = mapOf("prop" to integerDataType()),
                                          enum = listOf(mapOf("prop" to 1), mapOf("prop" to 2)))

      // when
      val result = objectDataType.validate(mapOf("john" to 5))

      // then
      assert(result.isFailure())
    }

    @Test
    fun `generates valid random value that matches one of the enumerated values`() {
      // given
      val enum = listOf(mapOf("prop" to "value1"), mapOf("prop" to "value2"))
      val objectDataType = objectDataType(properties = mapOf("prop" to stringDataType()), enum = enum)

      // when
      val result = objectDataType.randomValue()

      // then
      assert(enum.contains(result))
    }
  }
}
