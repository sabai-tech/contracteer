package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.dsl.allOfType
import tech.sabai.contracteer.core.dsl.booleanType
import tech.sabai.contracteer.core.dsl.integerType
import tech.sabai.contracteer.core.dsl.objectType
import tech.sabai.contracteer.core.dsl.stringType
import tech.sabai.contracteer.core.normalize

class AllOfDataTypeTest {
  private val pet = objectType(name = "pet") {
    properties {
      "name" to stringType()
      "petType" to stringType()
    }
    required("name", "petType")
  }

  private val cat = objectType(name = "cat") {
    properties {
      "hunts" to booleanType()
      "age" to integerType()
    }
    required("hunts", "age")
  }

  @Test
  fun `validation fails when any of the sub datatypes validation fails`() {
    // given
    val allOfDataType = allOfType { subType(pet); subType(cat) }

    // when
    val result = allOfDataType.validate(mapOf("petType" to "cat", "hunts" to true, "age" to 3, "barks" to true))

    // then
    assert(result.isFailure())
    assert(Regex("(?s)(?=.*pet)(?=.*name)").containsMatchIn(result.errors().first()))
  }

  @Test
  fun `validation succeeds when all sub datatypes validation succeed`() {
    // given
    val allOfDataType = allOfType { subType(pet); subType(cat) }

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
    val allOfDataType = allOfType { subType(pet); subType(cat) }

    // when
    val randomValue = allOfDataType.randomValue()

    // then
    assert(allOfDataType.validate(randomValue).isSuccess())
  }

  @Nested
  inner class WithAdditionalPropertiesFalse {
    private val animal = allOfType {
      subType(objectType(name = "Pet", allowAdditionalProperties = false) {
        properties { "name" to stringType() }
        required("name")
      })
      subType(objectType(name = "Hunts", allowAdditionalProperties = false) {
        properties { "hunts" to booleanType() }
        required("hunts")
      })
    }

    private val cat = allOfType {
      subType(animal)
      subType(objectType(name = "allOf #1", allowAdditionalProperties = false) {
        properties { "indoor" to booleanType() }
      })
    }

    @Test
    fun `validation succeeds when additionalProperties is false`() {
      // when
      val result = cat.validate(mapOf("name" to "kitty", "hunts" to true, "indoor" to true))

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `validation succeeds when sub-types share overlapping properties`() {
      // given
      val allOf = allOfType {
        subType(objectType(name = "Base", allowAdditionalProperties = false) {
          properties {
            "name" to stringType()
            "age" to integerType()
          }
          required("name", "age")
        })
        subType(objectType(name = "Extension", allowAdditionalProperties = false) {
          properties {
            "name" to stringType()
            "hunts" to booleanType()
          }
          required("name", "hunts")
        })
      }

      // when
      val result = allOf.validate(mapOf("name" to "kitty", "age" to 3, "hunts" to true))

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `validation fails when value contains properties not declared in any nested sub-type`() {
      // when
      val result = cat.validate(mapOf("name" to "kitty", "hunts" to true, "indoor" to true, "color" to "black"))

      // then
      assert(result.isFailure())
    }
  }

  @Nested
  inner class WithEnum {

    @Test
    fun `creation fails when enum contains a value that does not match any provided sub datatype`() {
      // when
      val result = AllOfDataType.create(
        name = "allOf",
        subTypes = listOf(pet, cat),
        enum = listOf(
          mapOf("hunts" to true, "name" to "kitty", "age" to 1, "petType" to "cat"),
          mapOf("hunts" to "hunts", "name" to "kitten", "age" to 2, "petType" to "cat")
        ))

      // then
      assert(result.isFailure())
    }

    @Test
    fun `validation succeeds when the value is included in the enum`() {
      // given
      val allOfDataType = allOfType(
        enum = listOf(
          mapOf("hunts" to true, "name" to "kitty", "age" to 1, "petType" to "cat"),
          mapOf("hunts" to false, "age" to 2, "name" to "lizard", "petType" to "lizard"),
        )
      ) { subType(pet); subType(cat) }

      // when
      val result = allOfDataType.validate(mapOf("hunts" to true, "age" to 1, "name" to "kitty", "petType" to "cat"))

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `validation fails when the value is not included in the enum`() {
      // given
      val allOfDataType = allOfType(
        enum = listOf(
          mapOf("hunts" to true, "name" to "kitty", "age" to 1, "petType" to "cat"),
          mapOf("hunts" to false, "age" to 2, "name" to "lizard", "petType" to "lizard"),
        )
      ) { subType(pet); subType(cat) }


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

      val allOfDataType = allOfType(enum = enum) { subType(pet); subType(cat) }

      // when
      val result = allOfDataType.randomValue()

      // then
      assert(enum.map { obj -> obj.map { it.key to it.value.normalize() }.toMap() }.contains(result))
    }
  }

  @Nested
  inner class WithSinglePrimitiveSubType {

    @Test
    fun `creation succeeds with a single non-structured sub-type`() {
      // when
      val result = AllOfDataType.create("myAllOf", subTypes = listOf(stringType()))

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `validation succeeds when value matches the sub-type`() {
      // given
      val allOfDataType = allOfType { subType(stringType()) }

      // when / then
      assert(allOfDataType.validate("hello").isSuccess())
    }

    @Test
    fun `validation fails when value does not match the sub-type`() {
      // given
      val allOfDataType = allOfType { subType(stringType()) }

      // when / then
      assert(allOfDataType.validate(42).isFailure())
    }

    @Test
    fun `generates a valid random value`() {
      // given
      val allOfDataType = allOfType { subType(stringType()) }

      // when
      val randomValue = allOfDataType.randomValue()

      // then
      assert(allOfDataType.validate(randomValue).isSuccess())
    }

    @Test
    fun `is not fully structured`() {
      // given
      val allOfDataType = allOfType { subType(stringType()) }

      // then
      assert(!allOfDataType.isFullyStructured())
    }

  }

  @Test
  fun `creation fails for multi-element allOf with non-structured sub-types`() {
    // when
    val result = AllOfDataType.create("myAllOf", subTypes = listOf(stringType(), stringType()))

    // then
    assert(result.isFailure())
  }

  @Test
  fun `creation fails for multi-element allOf with mixed structured and non-structured sub-types`() {
    // when
    val result = AllOfDataType.create("myAllOf", subTypes = listOf(pet, stringType()))

    // then
    assert(result.isFailure())
  }

  @Nested
  inner class WithDiscriminator {

    @Test
    fun `creation fails when discriminator property appears in multiple sub types`() {
      // when
      val result = AllOfDataType.create(
        "cat",
        subTypes = listOf(pet,
                          objectType {
                            properties {
                              "petType" to stringType()
                              "hunts" to booleanType()
                              "age" to integerType()
                            }
                            required("hunts", "age", "petType")
                          }),
        discriminator = Discriminator("petType"))

      // then
      assert(result.isFailure())
    }

    @Test
    fun `validation succeeds using discriminator mapping`() {
      // given
      val allOfDataType = allOfType("cat") {
        subType(pet)
        subType(objectType {
          properties {
            "hunts" to booleanType()
            "age" to integerType()
          }
          required("hunts", "age")
        })
        discriminator("petType") { mapping("CAT", "cat") }
      }

      // when
      val result = allOfDataType.validate(mapOf("hunts" to true, "name" to "kitty", "age" to 1, "petType" to "CAT"))

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `validation fails when discriminator property is not defined in the sub data types`() {
      // given
      val allOfDataType = allOfType("cat") {
        subType(pet)
        subType(objectType {
          properties {
            "hunts" to booleanType()
            "age" to integerType()
          }
          required("hunts", "age")
        })
        discriminator("petType") { mapping("CAT", "cat") }
      }

      // when
      val result =
        allOfDataType.validate(mapOf("hunts" to false, "age" to 2, "name" to "lizard", "petType" to "lizard"))

      // then
      assert(result.isFailure())
    }

    @Test
    fun `validation does not hard-fail on absent discriminator when sub types accept the payload`() {
      // given
      val allOfDataType = allOfType("cat") {
        subType(objectType(name = "pet") {
          properties {
            "name" to stringType()
            "petType" to stringType()
          }
          required("name")
        })
        subType(objectType {
          properties {
            "hunts" to booleanType()
            "age" to integerType()
          }
          required("hunts", "age")
        })
        discriminator("petType") { mapping("CAT", "cat") }
      }

      // when
      val result = allOfDataType.validate(mapOf("name" to "Misty", "hunts" to true, "age" to 3))

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `validation does not hard-fail on non-string discriminator and delegates to sub types`() {
      // given
      val allOfDataType = allOfType("cat") {
        subType(pet)
        subType(objectType {
          properties {
            "hunts" to booleanType()
            "age" to integerType()
          }
          required("hunts", "age")
        })
        discriminator("petType") { mapping("CAT", "cat") }
      }

      // when
      val result = allOfDataType.validate(mapOf("name" to "Misty", "petType" to 42, "hunts" to true, "age" to 3))

      // then
      assert(result.isFailure())
      assert(result.errors().first().contains("No matching schema"))
    }

    @Test
    fun `generates valid random value that includes the correct discriminator property value`() {
      // given
      val allOfDataType = allOfType("cat") {
        subType(pet)
        subType(objectType {
          properties {
            "hunts" to booleanType()
            "age" to integerType()
          }
          required("hunts", "age")
        })
        discriminator("petType") { mapping("CAT", "cat") }
      }

      // when
      val randomValue = allOfDataType.randomValue()

      // then
      assert(allOfDataType.validate(randomValue).isSuccess())
      assert((randomValue as Map<*, *>)["petType"] == "CAT")
    }
  }
}
