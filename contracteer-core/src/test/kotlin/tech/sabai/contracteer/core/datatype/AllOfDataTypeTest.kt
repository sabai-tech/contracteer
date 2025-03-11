package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.DataTypeFixture.allOfDataType
import tech.sabai.contracteer.core.DataTypeFixture.booleanDataType
import tech.sabai.contracteer.core.DataTypeFixture.integerDataType
import tech.sabai.contracteer.core.DataTypeFixture.objectDataType
import tech.sabai.contracteer.core.DataTypeFixture.stringDataType
import tech.sabai.contracteer.core.normalize

class AllOfDataTypeTest {
  private val pet = objectDataType(name = "pet",
                                   properties = mapOf(
                                     "name" to stringDataType(),
                                     "petType" to stringDataType()),
                                   requiredProperties = setOf("name", "petType"))

  private val cat = objectDataType(name = "cat",
                                   properties = mapOf("hunts" to booleanDataType(),
                                                      "age" to integerDataType()),
                                   requiredProperties = setOf("hunts", "age"))

  @Test
  fun `fails validation when any the sub datatypes validation fails`() {
    // given
    val allOfDataType = allOfDataType(subTypes = listOf(pet, cat))

    // when
    val result = allOfDataType.validate(mapOf("petType" to "cat", "hunts" to true, "age" to 3, "barks" to true))

    // then
    assert(result.isFailure())
    assert(Regex("(?s)(?=.*pet)(?=.*name)").containsMatchIn(result.errors().first()))
  }

  @Test
  fun `succeeds validation when all sub datatypes validation succeed`() {
    // given
    val allOfDataType = allOfDataType(subTypes = listOf(pet, cat))

    // when
    val result = allOfDataType.validate(mapOf(
      "petType" to "cat",
      "name" to "kitty",
      "hunts" to true,
      "age" to 1)
    )

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `generates a valid random value`() {
    // given
    val allOfDataType = allOfDataType(subTypes = listOf(pet, cat))

    // when
    val randomValue = allOfDataType.randomValue()

    // then
    assert(allOfDataType.validate(randomValue).isSuccess())
  }

  @Nested
  inner class WithEnum {

    @Test
    fun `fails creation when enum contains value not matching`() {
      // when
      val result = AllOfDataType.create(
        subTypes = listOf(pet, cat),
        enum = listOf(
          mapOf("hunts" to true, "name" to "kitty", "age" to 1, "petType" to "cat"),
          mapOf("hunts" to "hunts", "name" to "kitten", "age" to 2, "petType" to "cat")
        ))

      // then
      assert(result.isFailure())
    }


    @Test
    fun `succeeds validation when value matches one of the enumerated values`() {
      // given
      val allOfDataType = allOfDataType(
        subTypes = listOf(pet, cat),
        enum = listOf(
          mapOf("hunts" to true, "name" to "kitty", "age" to 1, "petType" to "cat"),
          mapOf("hunts" to false, "age" to 2, "name" to "lizard", "petType" to "lizard"),
        ))

      // when
      val result = allOfDataType.validate(mapOf("hunts" to true, "age" to 1, "name" to "kitty", "petType" to "cat"))

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `fails validation when the value is not one of the enumerated values `() {
      // given
      val allOfDataType = allOfDataType(
        subTypes = listOf(pet, cat),
        enum = listOf(
          mapOf("hunts" to true, "name" to "kitty", "age" to 1, "petType" to "cat"),
          mapOf("hunts" to false, "age" to 2, "name" to "lizard", "petType" to "lizard"),
        ))


      // when
      val result = allOfDataType.validate(mapOf("hunts" to true, "age" to 3, "name" to "kitty", "petType" to "cat"))

      // then
      assert(result.isFailure())
    }

    @Test
    fun `generates valid random value that matches one of the enumerated values`() {

      // given
      val enum = listOf(
        mapOf("hunts" to true, "name" to "kitty", "age" to 1, "petType" to "cat"),
        mapOf("hunts" to false, "age" to 2, "name" to "lizard", "petType" to "lizard"),
      )

      val dateDataType = allOfDataType(subTypes = listOf(pet, cat), enum = enum)

      // when
      val result = dateDataType.randomValue()

      // then
      assert(enum.map { obj -> obj.map { it.key to it.value.normalize() }.toMap() }.contains(result))
    }
  }

  @Nested
  inner class WithDiscriminator {

    @Test
    fun `fails creation when discriminator property is missing`() {
      // when
      val result = AllOfDataType.create(
        subTypes = listOf(pet, cat),
        discriminator = Discriminator("unknown")
      )
      // then
      assert(result.isFailure())
    }

    @Test
    fun `fails creation when discriminator property type is not a string`() {
      // when
      val result = AllOfDataType.create(
        "cat",
        subTypes = listOf(pet,
                          objectDataType(properties = mapOf("hunts" to booleanDataType(),
                                                            "age" to integerDataType()),
                                         requiredProperties = setOf("hunts",
                                                                    "age"))),
        discriminator = Discriminator("age"))
      // then
      assert(result.isFailure())
    }

    @Test
    fun `fails creation when discriminator property is not a required property`() {
      // when
      val result = AllOfDataType.create(
        "cat",
        subTypes = listOf(pet,
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
    fun `fails creation when discriminator property appears in multiple sub types`() {
      // when
      val result = AllOfDataType.create(
        "cat",
        subTypes = listOf(pet,
                          objectDataType(properties = mapOf("petType" to stringDataType(),
                                                            "hunts" to booleanDataType(),
                                                            "age" to integerDataType()),
                                         requiredProperties = setOf("hunts", "age", "petType"))),
        discriminator = Discriminator("petType"))

      // then
      assert(result.isFailure())
    }

    @Test
    fun `succeeds validation using discriminator mapping`() {
      // given
      val allOfDataType = allOfDataType("cat",
                                        subTypes = listOf(pet,
                                                          objectDataType(
                                                            properties = mapOf("hunts" to booleanDataType(),
                                                                               "age" to integerDataType()),
                                                            requiredProperties = setOf("hunts", "age"))),
                                        discriminator = Discriminator("petType", mapOf("CAT" to "cat")))

      // when
      val result = allOfDataType.validate(mapOf("hunts" to true, "name" to "kitty", "age" to 1, "petType" to "CAT"))

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `fails validation when discriminator property is not defined in the sub data types`() {
      // given
      val allOfDataType = allOfDataType("cat",
                                        subTypes = listOf(pet,
                                                          objectDataType(properties = mapOf("hunts" to booleanDataType(),
                                                                                            "age" to integerDataType()),
                                                                         requiredProperties = setOf("hunts", "age"))),
                                        discriminator = Discriminator("petType", mapOf("CAT" to "cat")))

      // when
      val result =
        allOfDataType.validate(mapOf("hunts" to false, "age" to 2, "name" to "lizard", "petType" to "lizard"))

      // then
      assert(result.isFailure())
    }

    @Test
    fun `generates valid random value that includes the correct discriminator property value`() {
      // given
      val allOfDataType = allOfDataType("cat",
                                        subTypes = listOf(pet,
                                                          objectDataType(properties = mapOf("hunts" to booleanDataType(),
                                                                                            "age" to integerDataType()),
                                                                         requiredProperties = setOf("hunts", "age"))),
                                        discriminator = Discriminator("petType", mapOf("CAT" to "cat")))

      // when
      val randomValue = allOfDataType.randomValue()

      // then
      assert(allOfDataType.validate(randomValue).isSuccess())
      assert(randomValue["petType"] == "CAT")
    }
  }
}