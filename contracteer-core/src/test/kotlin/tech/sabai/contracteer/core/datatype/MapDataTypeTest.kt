package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.TestFixture.integerDataType
import tech.sabai.contracteer.core.TestFixture.mapDataType
import tech.sabai.contracteer.core.TestFixture.stringDataType

class MapDataTypeTest {

  @Test
  fun `creation fails when a required property is not defined as a property`() {
    // when
    val result = MapDataType.create(name = "cat",
                                    properties = setOf("age"),
                                    requiredProperties = setOf("hunts"),
                                    valueDataType = stringDataType(),
                                    isNullable = false)
    // then
    assert(result.isFailure())
    assert(result.errors().first().contains("'hunts'"))
  }

  @Test
  fun `validation succeeds for a null value when nullable`() {
    // given
    val mapDataType = mapDataType(valueDatatype = stringDataType(), isNullable = true)

    // when
    val result = mapDataType.validate(null)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validation fails for a null value when not nullable`() {
    // given
    val mapDataType = mapDataType(valueDatatype = stringDataType())

    // when
    val result = mapDataType.validate(null)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `validation fails when a value is not of type Map`() {
    // given
    val mapDataType = mapDataType(valueDatatype = stringDataType())

    // when
    val result = mapDataType.validate(123)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `validation succeeds when a value is of type Map`() {
    // given
    val mapDataType = mapDataType(valueDatatype = stringDataType())

    // when
    val result = mapDataType.validate(mapOf("prop" to "value"))

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validation succeeds when a non required property is not present`() {
    // given
    val mapDataType = mapDataType(
      properties = setOf("prop", "prop2"),
      requiredProperties = setOf("prop"),
      valueDatatype = integerDataType(),
    )
    // when
    val result = mapDataType.validate(mapOf("prop" to 1))

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validation succeeds when a non required and non nullable property is not present`() {
    // given
    val mapDataType = mapDataType(
      properties = setOf("prop"),
      requiredProperties = setOf("prop"),
      valueDatatype = integerDataType(isNullable = false)
    )
    // when
    val result = mapDataType.validate(mapOf("prop" to 1))

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validation fails when a property is not of the expected type`() {
    // given
    val mapDataType = mapDataType(valueDatatype = stringDataType())

    // when
    val result = mapDataType.validate(mapOf("prop" to true))

    // then
    assert(result.isFailure())
  }


  @Test
  fun `validation fails when a required property is missing`() {
    // given
    val mapDataType = mapDataType(
      properties = setOf("prop"),
      requiredProperties = setOf("prop"),
      valueDatatype = stringDataType()
    )

    // when
    val result = mapDataType.validate(mapOf("prop2" to "value"))

    // then
    assert(result.isFailure())
    assert(result.errors().size == 1)
    assert(listOf("required", "prop").all { result.errors().first().contains(it) })
  }

  @Test
  fun `generate valid random value`() {
    // given
    val mapDataType = mapDataType(valueDatatype = stringDataType())

    // when
    val randomValue = mapDataType.randomValue()

    // then
    assert(randomValue.keys.isNotEmpty())
    assert(randomValue.values.all { it is String })
  }

  @Test
  fun `generate valid random value with required properties`() {
    // given
    val mapDataType = mapDataType(
      properties = setOf("prop"),
      requiredProperties = setOf("prop"),
      valueDatatype = stringDataType()
    )

    // when
    val randomValue = mapDataType.randomValue()

    // then
    assert(randomValue.keys.isNotEmpty())
    assert(randomValue.keys.containsAll(listOf("prop")))
    assert(randomValue.values.all { it is String })
  }


  @Nested
  inner class WithEnum {

    @Test
    fun `creation fails when enum contains a value that does not match value datatype`() {
      // when
      val result = MapDataType.create(name = "object",
                                      valueDataType = integerDataType(),
                                      enum = listOf(
                                        mapOf("prop" to 1, "prop2" to "2"),
                                        mapOf("prop" to 2))
      )

      // then
      assert(result.isFailure())
    }

    @Test
    fun `validation succeeds when the value is included in the enum`() {
      // given
      val mapDataType = mapDataType(valueDatatype = integerDataType(),
                                    enum = listOf(mapOf("prop" to 1), mapOf("prop" to 2)))

      // when
      val result = mapDataType.validate(mapOf("prop" to 1))

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `validation fails when the value is not included in the enum`() {
      // given
      val mapDataType = mapDataType(valueDatatype = integerDataType(),
                                    enum = listOf(mapOf("prop" to 1), mapOf("prop" to 2)))

      // when
      val result = mapDataType.validate(mapOf("john" to 5))

      // then
      assert(result.isFailure())
    }

    @Test
    fun `generates valid random value that matches one of the enumerated values`() {
      // given
      val enum = listOf(mapOf("prop" to "value1"), mapOf("prop" to "value2"))
      val objectDataType = mapDataType(valueDatatype = stringDataType(), enum = enum)

      // when
      val result = objectDataType.randomValue()

      // then
      assert(enum.contains(result))
    }
  }
}
