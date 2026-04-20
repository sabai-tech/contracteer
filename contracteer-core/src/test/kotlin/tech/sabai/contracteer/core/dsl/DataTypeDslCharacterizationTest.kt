package tech.sabai.contracteer.core.dsl

import java.math.BigDecimal
import tech.sabai.contracteer.core.TestFixture.allOfDataType
import tech.sabai.contracteer.core.TestFixture.anyOfDataType
import tech.sabai.contracteer.core.TestFixture.arrayDataType
import tech.sabai.contracteer.core.TestFixture.booleanDataType
import tech.sabai.contracteer.core.TestFixture.integerDataType
import tech.sabai.contracteer.core.TestFixture.objectDataType
import tech.sabai.contracteer.core.TestFixture.oneOfDataType
import tech.sabai.contracteer.core.TestFixture.stringDataType
import tech.sabai.contracteer.core.datatype.AllOfDataType
import tech.sabai.contracteer.core.datatype.AnyOfDataType
import tech.sabai.contracteer.core.datatype.ArrayDataType
import tech.sabai.contracteer.core.datatype.DataType
import tech.sabai.contracteer.core.datatype.Discriminator
import tech.sabai.contracteer.core.datatype.IntegerDataType
import tech.sabai.contracteer.core.datatype.ObjectDataType
import tech.sabai.contracteer.core.datatype.OneOfDataType
import tech.sabai.contracteer.core.datatype.StringDataType
import kotlin.test.Test

class DataTypeDslCharacterizationTest {

  @Test
  fun `stringType with pattern and length`() {
    val flat = stringDataType(pattern = "\\d{5}", minLength = 5, maxLength = 5)
    val dsl = stringType(pattern = "\\d{5}", minLength = 5, maxLength = 5)
    assertSameShape(flat, dsl)
  }

  @Test
  fun `integerType with range and multipleOf`() {
    val flat = integerDataType(
      minimum = BigDecimal(1),
      maximum = BigDecimal(100),
      multipleOf = BigDecimal(5)
    )
    val dsl = integerType(
      minimum = BigDecimal(1),
      maximum = BigDecimal(100),
      multipleOf = BigDecimal(5)
    )
    assertSameShape(flat, dsl)
  }

  @Test
  fun `arrayType minimal`() {
    val flat = arrayDataType(stringDataType())
    val dsl = arrayType(stringType())
    assertSameShape(flat, dsl)
  }

  @Test
  fun `arrayType with constraints`() {
    val flat = arrayDataType(integerDataType(), minItems = 1, maxItems = 10, uniqueItems = true)
    val dsl = arrayType(integerType(), minItems = 1, maxItems = 10, uniqueItems = true)
    assertSameShape(flat, dsl)
  }

  @Test
  fun `nested objectType with properties`() {
    val flatAddress = objectDataType(
      name = "address",
      properties = mapOf(
        "street" to stringDataType(),
        "city" to stringDataType()
      ),
      requiredProperties = setOf("street", "city")
    )
    val flat = objectDataType(
      name = "user",
      properties = mapOf(
        "id" to integerDataType(),
        "name" to stringDataType(),
        "address" to flatAddress
      ),
      requiredProperties = setOf("id", "name")
    )

    val dsl = objectType(name = "user") {
      properties {
        "id" to integerType()
        "name" to stringType()
        "address" to objectType(name = "address") {
          properties {
            "street" to stringType()
            "city" to stringType()
          }
          required("street", "city")
        }
      }
      required("id", "name")
    }

    assertSameShape(flat, dsl)
  }

  @Test
  fun `allOfType with subtypes and discriminator`() {
    val pet = objectDataType(
      name = "pet",
      properties = mapOf("petType" to stringDataType()),
      requiredProperties = setOf("petType")
    )
    val catBase = objectDataType(
      name = "catBase",
      properties = mapOf("hunts" to booleanDataType(), "age" to integerDataType()),
      requiredProperties = setOf("hunts", "age")
    )

    val flat = allOfDataType(
      name = "cat",
      subTypes = listOf(pet, catBase),
      discriminator = Discriminator("petType", mapOf("CAT" to "cat"))
    )
    val dsl = allOfType("cat") {
      subType(pet)
      subType(catBase)
      discriminator("petType") {
        mapping("CAT", "cat")
      }
    }

    assertSameShape(flat, dsl)
  }

  @Test
  fun `oneOfType with discriminator and multiple mappings`() {
    val cat = objectDataType(
      name = "cat",
      properties = mapOf("petType" to stringDataType()),
      requiredProperties = setOf("petType")
    )
    val dog = objectDataType(
      name = "dog",
      properties = mapOf("petType" to stringDataType()),
      requiredProperties = setOf("petType")
    )

    val flat = oneOfDataType(
      name = "animal",
      subTypes = listOf(cat, dog),
      discriminator = Discriminator("petType", mapOf("CAT" to "cat", "DOG" to "dog"))
    )
    val dsl = oneOfType("animal") {
      subType(cat)
      subType(dog)
      discriminator("petType") {
        mapping("CAT", "cat")
        mapping("DOG", "dog")
      }
    }

    assertSameShape(flat, dsl)
  }

  @Test
  fun `anyOfType without discriminator`() {
    val stringOption = stringDataType(name = "stringOption")
    val intOption = integerDataType(name = "intOption")

    val flat = anyOfDataType(name = "choice", subTypes = listOf(stringOption, intOption))
    val dsl = anyOfType("choice") {
      subType(stringOption)
      subType(intOption)
    }

    assertSameShape(flat, dsl)
  }

  // --- helpers -----------------------------------------------------------

  // DataTypes are not data classes and have no structural equality.
  // Compare the fields that carry the shape for each subtype.
  private fun assertSameShape(expected: DataType<out Any>, actual: DataType<out Any>) {
    assert(expected::class == actual::class)
    assert(expected.name == actual.name)
    assert(expected.openApiType == actual.openApiType)
    assert(expected.isNullable == actual.isNullable)
    when {
      expected is ObjectDataType && actual is ObjectDataType  -> assertObjectShape(expected, actual)
      expected is ArrayDataType && actual is ArrayDataType    -> assertArrayShape(expected, actual)
      expected is AllOfDataType && actual is AllOfDataType    -> assertCompositeShape(expected.subTypes, actual.subTypes, expected.discriminator, actual.discriminator)
      expected is OneOfDataType && actual is OneOfDataType    -> assertCompositeShape(expected.subTypes, actual.subTypes, expected.discriminator, actual.discriminator)
      expected is AnyOfDataType && actual is AnyOfDataType    -> assertCompositeShape(expected.subTypes, actual.subTypes, expected.discriminator, actual.discriminator)
      expected is StringDataType && actual is StringDataType  -> assertStringShape(expected, actual)
      expected is IntegerDataType && actual is IntegerDataType -> assertIntegerShape(expected, actual)
    }
  }

  private fun assertObjectShape(expected: ObjectDataType, actual: ObjectDataType) {
    assert(expected.properties.keys == actual.properties.keys)
    expected.properties.forEach { (key, expectedProp) ->
      assertSameShape(expectedProp, actual.properties[key]!!)
    }
    assert(expected.requiredProperties == actual.requiredProperties)
    assert(expected.allowAdditionalProperties == actual.allowAdditionalProperties)
    assert(expected.minProperties == actual.minProperties)
    assert(expected.maxProperties == actual.maxProperties)
  }

  private fun assertArrayShape(expected: ArrayDataType, actual: ArrayDataType) {
    assertSameShape(expected.itemDataType, actual.itemDataType)
    assert(expected.minItems == actual.minItems)
    assert(expected.maxItems == actual.maxItems)
    assert(expected.uniqueItems == actual.uniqueItems)
  }

  private fun assertCompositeShape(
    expectedSubTypes: List<DataType<out Any>>,
    actualSubTypes: List<DataType<out Any>>,
    expectedDiscriminator: Discriminator?,
    actualDiscriminator: Discriminator?
  ) {
    assert(expectedSubTypes.size == actualSubTypes.size)
    expectedSubTypes.zip(actualSubTypes).forEach { (e, a) -> assertSameShape(e, a) }
    assert(expectedDiscriminator == actualDiscriminator)
  }

  private fun assertStringShape(expected: StringDataType, actual: StringDataType) {
    assert(expected.lengthRange.toString() == actual.lengthRange.toString())
  }

  private fun assertIntegerShape(expected: IntegerDataType, actual: IntegerDataType) {
    assert(expected.range.toString() == actual.range.toString())
    assert(expected.multipleOf == actual.multipleOf)
  }
}
