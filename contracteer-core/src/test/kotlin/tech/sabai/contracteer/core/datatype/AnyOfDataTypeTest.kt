package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.DataTypeFixture.anyOfDataType
import tech.sabai.contracteer.core.DataTypeFixture.booleanDataType
import tech.sabai.contracteer.core.DataTypeFixture.integerDataType
import tech.sabai.contracteer.core.DataTypeFixture.objectDataType
import tech.sabai.contracteer.core.DataTypeFixture.stringDataType
import tech.sabai.contracteer.core.normalize

class AnyOfDataTypeTest {

  private val dog = objectDataType(name = "dog",
                                   properties = mapOf("barks" to booleanDataType(),
                                                      "age" to integerDataType(),
                                                      "type" to stringDataType()),
                                   requiredProperties = setOf("type", "barks"))
  private val cat = objectDataType(name = "cat",
                                   properties = mapOf("hunts" to booleanDataType(),
                                                      "age" to integerDataType(),
                                                      "type" to stringDataType()),
                                   requiredProperties = setOf("type", "hunts"))

  private val quantity = integerDataType(name = "quantity")

  private val name = stringDataType(name = "name")

  @Test
  fun `validation fails when none of sub datatype validates the value`() {
    // given
    val anyOfDataType = anyOfDataType(subTypes = listOf(dog, cat, quantity, name))

    // when
    val result = anyOfDataType.validate(true)

    // then
    assert(result.isFailure())
    assert(result.errors().first().contains("dog"))
    assert(result.errors().first().contains("cat"))
    assert(result.errors().first().contains("quantity"))
    assert(result.errors().first().contains("name"))
  }

  @Test
  fun `validation succeeds when multiple sub datatypes validate the value`() {
    // given
    val anyOfDataType = anyOfDataType(subTypes = listOf(quantity, name, integerDataType()))

    // when
    val result = anyOfDataType.validate(2)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `generates a valid random value`() {
    // given
    val anyOfDataType = anyOfDataType(subTypes = listOf(dog, cat, quantity, name))

    // when
    val randomValue = anyOfDataType.randomValue()

    // then
    assert(anyOfDataType.validate(randomValue).isSuccess())
  }

  @Nested
  inner class WithEnum {

    @Test
    fun `creation fails when enum contains a value that does not match any provided sub datatype`() {
      // when
      val result = AnyOfDataType.create(
        subTypes = listOf(dog, cat, quantity, name),
        enum = listOf("John", 42, true)
      )

      // then
      assert(result.isFailure())
    }

    @Test
    fun `validation succeeds when the value is included in the enum`() {
      // given
      val anyOfDataType = anyOfDataType(
        subTypes = listOf(dog, cat, quantity, name),
        enum = listOf(mapOf("barks" to true, "age" to 3, "type" to "dog"),
                      mapOf("hunts" to true, "age" to 2, "type" to "cat"),
                      42))

      // when
      val result = anyOfDataType.validate(42)

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `validation fails when the value is not included in the enum`() {
      // given
      val anyOfDataType = anyOfDataType(
        subTypes = listOf(dog, cat, quantity, name),
        enum = listOf(mapOf("barks" to true, "age" to 3, "type" to "dog"),
                      mapOf("hunts" to true, "age" to 2, "type" to "cat"),
                      42))

      // when
      val result = anyOfDataType.validate(mapOf("barks" to false, "age" to 2))

      // then
      assert(result.isFailure())
    }

    @Test
    fun `generates valid random value that matches one of the enumerated values`() {
      // given
      val enum = listOf(mapOf("barks" to true, "age" to 3, "type" to "dog"),
                        mapOf("hunts" to true, "age" to 2, "type" to "cat"),
                        42,
                        "John")
      val anyOfDataType = anyOfDataType(subTypes = listOf(dog, cat, quantity, name), enum = enum)

      // when
      val result = anyOfDataType.randomValue()

      // then
      assert(enum.map { obj -> obj.normalize() }.contains(result))
    }
  }

  @Nested
  inner class WithDiscriminator {

    @Test
    fun `creation fails when discriminator property is missing`() {
      // when
      val result = AnyOfDataType.create(
        subTypes = listOf(dog,
                          cat,
                          objectDataType(
                            name = "lizard",
                            properties = mapOf("age" to integerDataType()),
                            requiredProperties = setOf("age")
                          )),
        discriminator = Discriminator("type")
      )

      // then
      assert(result.isFailure())
    }

    @Test
    fun `creation fails when discriminator property is not a string`() {
      // when
      val result = AnyOfDataType.create(
        subTypes = listOf(dog,
                          cat,
                          objectDataType(properties = mapOf("type" to integerDataType()),
                                         requiredProperties = setOf("type"))),
        discriminator = Discriminator("type")
      )

      // then
      assert(result.isFailure())
    }

    @Test
    fun `creation fails when discriminator property is not a required property`() {
      // when
      val result = AnyOfDataType.create(
        subTypes = listOf(cat,
                          objectDataType(properties = mapOf("type" to stringDataType(),
                                                            "hunts" to booleanDataType(),
                                                            "age" to integerDataType()),
                                         requiredProperties = setOf("hunts",
                                                                    "age"))),
        discriminator = Discriminator("type"))
      // then
      assert(result.isFailure())
    }

    @Test
    fun `creation fails when discriminator mapping references unknown sub datatype`() {
      // when
      val result = AnyOfDataType.create(
        subTypes = listOf(dog, cat),
        discriminator = Discriminator("type",
                                      mapOf("DOG" to "lizard")
        )
      )
      // then
      assert(result.isFailure())
    }

    @Test
    fun `creation fails when discriminator is provided and not all sub datatypes are composite structured`() {
      // when
      val result = AnyOfDataType.create(
        subTypes = listOf(dog, cat, quantity, name),
        discriminator = Discriminator("type")
      )

      // then
      assert(result.isFailure())
    }

    @Test
    fun `creation succeeds when sub datatypes are a composite structured datatype`() {
      // when
      val result = AnyOfDataType.create(
        subTypes = listOf(
          dog,
          anyOfDataType(
            name = "other",
            subTypes = listOf(
              cat,
              objectDataType(
                name = "lizard",
                properties = mapOf("age" to integerDataType(), "type" to stringDataType()),
                requiredProperties = setOf("age", "type")
              )))),
        discriminator = Discriminator("type")
      )

      // then
      assert(result.isSuccess())

    }

    @Test
    fun `validation succeeds with discriminator mapping`() {
      // given
      val anyOfDataType = anyOfDataType(
        subTypes = listOf(dog, cat),
        discriminator = Discriminator("type", mapOf("DOG" to "dog")))

      // when
      val result = anyOfDataType.validate(mapOf("type" to "DOG", "barks" to true))

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `generates random valid value with discriminator`() {
      // given
      val anyOfDataType = anyOfDataType(
        subTypes = listOf(dog, cat),
        discriminator = Discriminator("type"))

      // when
      val randomValue = anyOfDataType.randomValue() as Map<*, *>

      assert(
        (dog.validate(randomValue).isSuccess() && randomValue["type"] == "dog") ||
        (cat.validate(randomValue).isSuccess() && randomValue["type"] == "cat")
      )
    }

    @Test
    fun `generates random valid value for composite structured sub datatypes`() {
      // given
      val anyOfDataType = anyOfDataType(
        subTypes = listOf(
          dog,
          anyOfDataType(
            name = "other",
            subTypes = listOf(
              cat,
              objectDataType(
                name = "lizard",
                properties = mapOf("age" to integerDataType(), "type" to stringDataType()),
                requiredProperties = setOf("age", "type")
              )))),
        discriminator = Discriminator("type")
      )

      // when
      val randomValue = anyOfDataType.randomValue()

      // then
      assert(anyOfDataType.validate(randomValue).isSuccess())
      assert((randomValue as Map<*, *>)["type"] == "dog" || (randomValue["type"] == "other"))
    }
  }
}
