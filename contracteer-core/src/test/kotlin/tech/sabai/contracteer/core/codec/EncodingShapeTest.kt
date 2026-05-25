package tech.sabai.contracteer.core.codec

import java.math.BigDecimal
import tech.sabai.contracteer.core.assertFailure
import tech.sabai.contracteer.core.assertSuccess
import tech.sabai.contracteer.core.codec.EncodingShape.Array
import tech.sabai.contracteer.core.codec.EncodingShape.Object
import tech.sabai.contracteer.core.codec.EncodingShape.Scalar
import tech.sabai.contracteer.core.datatype.AllOfDataType
import tech.sabai.contracteer.core.datatype.AnyDataType
import tech.sabai.contracteer.core.datatype.AnyOfDataType
import tech.sabai.contracteer.core.datatype.ProxyDataType
import tech.sabai.contracteer.core.dsl.allOfType
import tech.sabai.contracteer.core.serde.PlainTextSerde
import tech.sabai.contracteer.core.dsl.anyOfType
import tech.sabai.contracteer.core.dsl.arrayType
import tech.sabai.contracteer.core.dsl.integerType
import tech.sabai.contracteer.core.dsl.objectType
import tech.sabai.contracteer.core.dsl.oneOfType
import tech.sabai.contracteer.core.dsl.stringType
import kotlin.test.Test

class EncodingShapeTest {

  // === Scalars ===

  @Test
  fun `of plain StringDataType returns Scalar`() {
    // when
    val shape = EncodingShape.of(stringType()).assertSuccess()

    // then
    assert(shape === Scalar)
  }

  @Test
  fun `of plain IntegerDataType returns Scalar`() {
    // when
    val shape = EncodingShape.of(integerType()).assertSuccess()

    // then
    assert(shape === Scalar)
  }

  @Test
  fun `of AnyDataType returns Scalar`() {
    // when
    val shape = EncodingShape.of(AnyDataType).assertSuccess()

    // then
    assert(shape === Scalar)
  }

  // === Object ===

  @Test
  fun `of plain ObjectDataType returns Object with properties wrapped in DecodeView`() {
    // given
    val name = stringType()
    val age = integerType()
    val person = objectType {
      properties {
        "name" to name
        "age" to age
      }
    }

    // when
    val shape = EncodingShape.of(person).assertSuccess()

    // then
    val obj = shape as Object
    assert(obj.properties == mapOf("name" to DecodeView.of(name), "age" to DecodeView.of(age)))
  }

  @Test
  fun `of plain ObjectDataType returns Object with additional-properties policy`() {
    // given
    val extra = stringType("extra")
    val obj = objectType(allowAdditionalProperties = true, additionalPropertiesDataType = extra)

    // when
    val shape = EncodingShape.of(obj).assertSuccess()

    // then
    val o = shape as Object
    assert(o.additionalProperties.allowed)
    assert(o.additionalProperties.itemType == DecodeView.of(extra))
  }

  // === Array ===

  @Test
  fun `of plain ArrayDataType returns Array with itemType wrapped in DecodeView`() {
    // given
    val item = stringType("item")
    val array = arrayType(item)

    // when
    val shape = EncodingShape.of(array).assertSuccess()

    // then
    val arr = shape as Array
    assert(arr.itemType == DecodeView.of(item))
  }

  // === Composite — Object merging ===

  @Test
  fun `of allOf of disjoint object branches returns Object with union of properties`() {
    // given
    val name = stringType()
    val age = integerType()
    val email = stringType("email")
    val base = objectType("base") {
      properties {
        "name" to name
        "age" to age
      }
    }
    val extension = objectType("extension") {
      properties {
        "email" to email
      }
    }
    val composed = allOfType {
      subType(base)
      subType(extension)
    }

    // when
    val shape = EncodingShape.of(composed).assertSuccess()

    // then
    val obj = shape as Object
    assert(obj.properties == mapOf(
      "name" to DecodeView.of(name),
      "age" to DecodeView.of(age),
      "email" to DecodeView.of(email)
    ))
  }

  @Test
  fun `of allOf with same-name primitive collision merges value via anyOf`() {
    // given
    val stringValue = stringType("stringValue")
    val integerValue = integerType("integerValue")
    val branchA = objectType("a") { properties { "value" to stringValue } }
    val branchB = objectType("b") { properties { "value" to integerValue } }
    val composed = allOfType {
      subType(branchA)
      subType(branchB)
    }

    // when
    val shape = EncodingShape.of(composed).assertSuccess()

    // then
    val obj = shape as Object
    val mergedSource = obj.properties["value"]!!.source
    assert(mergedSource is AnyOfDataType)
    val subTypes = (mergedSource as AnyOfDataType).subTypes
    assert(subTypes.size == 2)
    assert(stringValue in subTypes && integerValue in subTypes)
  }

  @Test
  fun `of allOf with same-name object collision merges nested objects recursively`() {
    // given
    val street = stringType("street")
    val city = stringType("city")
    val branchA = objectType("a") {
      properties { "address" to objectType("addressA") { properties { "street" to street } } }
    }
    val branchB = objectType("b") {
      properties { "address" to objectType("addressB") { properties { "city" to city } } }
    }
    val composed = allOfType {
      subType(branchA)
      subType(branchB)
    }

    // when
    val shape = EncodingShape.of(composed).assertSuccess()

    // then
    val obj = shape as Object
    val addressShape = EncodingShape.of(obj.properties["address"]!!.source).assertSuccess() as Object
    assert(addressShape.properties == mapOf(
      "street" to DecodeView.of(street),
      "city" to DecodeView.of(city)
    ))
  }

  @Test
  fun `of allOf with AnyDataType wildcard property merges via anyOf`() {
    // given
    val nested = objectType("nested") { properties { "y" to stringType() } }
    val branchAny = objectType("anyBranch") { properties { "x" to AnyDataType } }
    val branchObj = objectType("objBranch") { properties { "x" to nested } }
    val composed = allOfType {
      subType(branchAny)
      subType(branchObj)
    }

    // when
    val shape = EncodingShape.of(composed).assertSuccess()

    // then
    val obj = shape as Object
    val mergedSource = obj.properties["x"]!!.source
    assert(mergedSource is AnyOfDataType)
    val subTypes = (mergedSource as AnyOfDataType).subTypes
    assert(subTypes.size == 2)
    assert(AnyDataType in subTypes && nested in subTypes)
  }

  @Test
  fun `of allOf with kind-divergent same-name property fails with property error`() {
    // given
    val asString = stringType("asString")
    val asObject = objectType("asObject") { properties { "nested" to stringType() } }
    val branchA = objectType("a") { properties { "value" to asString } }
    val branchB = objectType("b") { properties { "value" to asObject } }
    val composed = allOfType {
      subType(branchA)
      subType(branchB)
    }

    // when
    val errors = EncodingShape.of(composed).assertFailure()

    // then
    assert(errors.size == 1)
    assert("value" in errors[0])
    assert("'primitive' vs 'object'" in errors[0])
  }

  @Test
  fun `of allOf with same-name array collision merges items via anyOf`() {
    // given
    val arrayA = arrayType(stringType("itemA"))
    val arrayB = arrayType(integerType("itemB"))
    val branchA = objectType("a") { properties { "tags" to arrayA } }
    val branchB = objectType("b") { properties { "tags" to arrayB } }
    val composed = allOfType {
      subType(branchA)
      subType(branchB)
    }

    // when
    val shape = EncodingShape.of(composed).assertSuccess()

    // then
    val obj = shape as Object
    val mergedSource = obj.properties["tags"]!!.source
    assert(mergedSource is AnyOfDataType)
    val subTypes = (mergedSource as AnyOfDataType).subTypes
    assert(arrayA in subTypes && arrayB in subTypes)
  }

  @Test
  fun `of oneOf of disjoint object branches returns Object with union`() {
    // given
    val name = stringType()
    val email = stringType("email")
    val branchA = objectType("a") { properties { "name" to name } }
    val branchB = objectType("b") { properties { "email" to email } }
    val composed = oneOfType {
      subType(branchA)
      subType(branchB)
    }

    // when
    val shape = EncodingShape.of(composed).assertSuccess()

    // then
    val obj = shape as Object
    assert(obj.properties == mapOf("name" to DecodeView.of(name), "email" to DecodeView.of(email)))
  }

  @Test
  fun `of anyOf of disjoint object branches returns Object with union`() {
    // given
    val name = stringType()
    val email = stringType("email")
    val branchA = objectType("a") { properties { "name" to name } }
    val branchB = objectType("b") { properties { "email" to email } }
    val composed = anyOfType {
      subType(branchA)
      subType(branchB)
    }

    // when
    val shape = EncodingShape.of(composed).assertSuccess()

    // then
    val obj = shape as Object
    assert(obj.properties == mapOf("name" to DecodeView.of(name), "email" to DecodeView.of(email)))
  }

  // === Composite — Array merging ===

  @Test
  fun `of composite of single array unwraps to that array's itemType`() {
    // given
    val item = stringType("item")
    val composed = anyOfType {
      subType(arrayType(item))
    }

    // when
    val shape = EncodingShape.of(composed).assertSuccess()

    // then
    val arr = shape as Array
    assert(arr.itemType == DecodeView.of(item))
  }

  @Test
  fun `of composite of arrays merges items via anyOf`() {
    // given
    val itemA = stringType("itemA")
    val itemB = integerType("itemB")
    val composed = anyOfType {
      subType(arrayType(itemA))
      subType(arrayType(itemB))
    }

    // when
    val shape = EncodingShape.of(composed).assertSuccess()

    // then
    val arr = shape as Array
    val mergedSource = arr.itemType.source
    assert(mergedSource is AnyOfDataType)
    val subTypes = (mergedSource as AnyOfDataType).subTypes
    assert(itemA in subTypes && itemB in subTypes)
  }

  // === Composite — Empty and Scalar ===

  @Test
  fun `of empty allOf returns Object with empty properties and permissive AP`() {
    // given
    val composed = allOfType {}

    // when
    val shape = EncodingShape.of(composed).assertSuccess()

    // then
    val obj = shape as Object
    assert(obj.properties.isEmpty())
    assert(obj.additionalProperties.allowed)
    assert(obj.additionalProperties.itemType == null)
  }

  @Test
  fun `of empty oneOf returns Object with empty properties and restrictive AP`() {
    // given
    val composed = oneOfType {}

    // when
    val shape = EncodingShape.of(composed).assertSuccess()

    // then
    val obj = shape as Object
    assert(obj.properties.isEmpty())
    assert(!obj.additionalProperties.allowed)
    assert(obj.additionalProperties.itemType == null)
  }

  @Test
  fun `of composite of all-scalar branches returns Scalar`() {
    // given
    val composed = anyOfType {
      subType(stringType("a"))
      subType(integerType("b"))
    }

    // when
    val shape = EncodingShape.of(composed).assertSuccess()

    // then
    assert(shape === Scalar)
  }

  // === Composite — AdditionalProperties policy ===

  @Test
  fun `of allOf is AND across branches for AP allowed`() {
    // given
    val branchPermissive = objectType("permissive", allowAdditionalProperties = true)
    val branchStrict = objectType("strict", allowAdditionalProperties = false)
    val composed = allOfType {
      subType(branchPermissive)
      subType(branchStrict)
    }

    // when
    val shape = EncodingShape.of(composed).assertSuccess()

    // then
    val obj = shape as Object
    assert(!obj.additionalProperties.allowed)
  }

  @Test
  fun `of allOf with two primitive AP itemTypes returns null itemType`() {
    // given
    val schemaA = stringType("schemaA")
    val schemaB = integerType("schemaB")
    val branchA = objectType("a", allowAdditionalProperties = true, additionalPropertiesDataType = schemaA)
    val branchB = objectType("b", allowAdditionalProperties = true, additionalPropertiesDataType = schemaB)
    val composed = allOfType {
      subType(branchA)
      subType(branchB)
    }

    // when
    val shape = EncodingShape.of(composed).assertSuccess()

    // then
    val obj = shape as Object
    assert(obj.additionalProperties.allowed)
    assert(obj.additionalProperties.itemType == null)
  }

  @Test
  fun `of allOf with two object AP itemTypes intersects via AllOf`() {
    // given
    val objectA = objectType("objectA") { properties { "x" to stringType() } }
    val objectB = objectType("objectB") { properties { "y" to stringType() } }
    val branchA = objectType("a", allowAdditionalProperties = true, additionalPropertiesDataType = objectA)
    val branchB = objectType("b", allowAdditionalProperties = true, additionalPropertiesDataType = objectB)
    val composed = allOfType {
      subType(branchA)
      subType(branchB)
    }

    // when
    val shape = EncodingShape.of(composed).assertSuccess()

    // then
    val obj = shape as Object
    assert(obj.additionalProperties.allowed)
    val mergedSource = obj.additionalProperties.itemType!!.source
    assert(mergedSource is AllOfDataType)
    val subTypes = (mergedSource as AllOfDataType).subTypes
    assert(objectA in subTypes && objectB in subTypes)
  }

  @Test
  fun `of allOf with two array AP itemTypes falls back to null itemType`() {
    // given
    val arrayA = arrayType(stringType("itemA"))
    val arrayB = arrayType(integerType("itemB"))
    val branchA = objectType("a", allowAdditionalProperties = true, additionalPropertiesDataType = arrayA)
    val branchB = objectType("b", allowAdditionalProperties = true, additionalPropertiesDataType = arrayB)
    val composed = allOfType {
      subType(branchA)
      subType(branchB)
    }

    // when
    val shape = EncodingShape.of(composed).assertSuccess()

    // then
    val obj = shape as Object
    assert(obj.additionalProperties.allowed)
    assert(obj.additionalProperties.itemType == null)
  }

  @Test
  fun `of allOf with one null-itemType branch keeps the non-null itemType`() {
    // given
    val constraintSchema = stringType("constraintSchema")
    val branchPermissive = objectType("permissive", allowAdditionalProperties = true)
    val branchConstrained = objectType("constrained", allowAdditionalProperties = true, additionalPropertiesDataType = constraintSchema)
    val composed = allOfType {
      subType(branchPermissive)
      subType(branchConstrained)
    }

    // when
    val shape = EncodingShape.of(composed).assertSuccess()

    // then
    val obj = shape as Object
    assert(obj.additionalProperties.allowed)
    assert(obj.additionalProperties.itemType!!.source === constraintSchema)
  }

  @Test
  fun `of oneOf is OR across branches for AP allowed`() {
    // given
    val branchPermissive = objectType("permissive", allowAdditionalProperties = true)
    val branchStrict = objectType("strict", allowAdditionalProperties = false)
    val composed = oneOfType {
      subType(branchPermissive)
      subType(branchStrict)
    }

    // when
    val shape = EncodingShape.of(composed).assertSuccess()

    // then
    val obj = shape as Object
    assert(obj.additionalProperties.allowed)
  }

  @Test
  fun `of anyOf is OR across branches for AP allowed`() {
    // given
    val branchPermissive = objectType("permissive", allowAdditionalProperties = true)
    val branchStrict = objectType("strict", allowAdditionalProperties = false)
    val composed = anyOfType {
      subType(branchPermissive)
      subType(branchStrict)
    }

    // when
    val shape = EncodingShape.of(composed).assertSuccess()

    // then
    val obj = shape as Object
    assert(obj.additionalProperties.allowed)
  }

  @Test
  fun `of oneOf with one permissive null-itemType branch returns null itemType`() {
    // given
    val constraintSchema = stringType("constraintSchema")
    val branchPermissive = objectType("permissive", allowAdditionalProperties = true)
    val branchConstrained = objectType("constrained", allowAdditionalProperties = true, additionalPropertiesDataType = constraintSchema)
    val composed = oneOfType {
      subType(branchPermissive)
      subType(branchConstrained)
    }

    // when
    val shape = EncodingShape.of(composed).assertSuccess()

    // then
    val obj = shape as Object
    assert(obj.additionalProperties.allowed)
    assert(obj.additionalProperties.itemType == null)
  }

  @Test
  fun `of anyOf with one permissive null-itemType branch returns null itemType`() {
    // given
    val constraintSchema = stringType("constraintSchema")
    val branchPermissive = objectType("permissive", allowAdditionalProperties = true)
    val branchConstrained = objectType("constrained", allowAdditionalProperties = true, additionalPropertiesDataType = constraintSchema)
    val composed = anyOfType {
      subType(branchPermissive)
      subType(branchConstrained)
    }

    // when
    val shape = EncodingShape.of(composed).assertSuccess()

    // then
    val obj = shape as Object
    assert(obj.additionalProperties.allowed)
    assert(obj.additionalProperties.itemType == null)
  }

  // === Proxy ===

  @Test
  fun `of resolved non-cyclic proxy delegates to target shape`() {
    // given
    val name = stringType()
    val target = objectType("target") { properties { "name" to name } }
    val proxy = ProxyDataType("targetProxy").apply { delegate = target }

    // when
    val shape = EncodingShape.of(proxy).assertSuccess()

    // then
    val obj = shape as Object
    assert(obj.properties == mapOf("name" to DecodeView.of(name)))
  }

  @Test
  fun `of unresolved proxy returns Scalar`() {
    // given
    val proxy = ProxyDataType("unresolved")

    // when
    val shape = EncodingShape.of(proxy).assertSuccess()

    // then
    assert(shape === Scalar)
  }

  // === Cycle handling ===

  @Test
  fun `of self-referential composite returns Object with empty properties`() {
    // given
    val proxy = ProxyDataType("User")
    val outer = AllOfDataType.create("User", listOf(proxy)).assertSuccess()
    proxy.delegate = outer

    // when
    val shape = EncodingShape.of(outer).assertSuccess()

    // then
    val obj = shape as Object
    assert(obj.properties.isEmpty())
    assert(obj.additionalProperties.allowed)
    assert(obj.additionalProperties.itemType == null)
  }

  @Test
  fun `of indirect cycle through composite returns Object with empty properties`() {
    // given
    val proxy = ProxyDataType("User")
    val middle = AllOfDataType.create("middle", listOf(proxy)).assertSuccess()
    val outer = AllOfDataType.create("outer", listOf(middle)).assertSuccess()
    proxy.delegate = outer

    // when
    val shape = EncodingShape.of(outer).assertSuccess()

    // then
    val obj = shape as Object
    assert(obj.properties.isEmpty())
  }

  // === Round-trip via DecodeView ===

  @Test
  fun `merged scalar property deserializes via PlainTextSerde`() {
    // given
    val name = stringType()
    val person = objectType { properties { "name" to name } }

    // when
    val shape = EncodingShape.of(person).assertSuccess()

    // then
    val obj = shape as Object
    assert(PlainTextSerde.deserialize("Alice", obj.properties["name"]!!).assertSuccess() == "Alice")
  }

  @Test
  fun `merged AP itemType deserializes via PlainTextSerde`() {
    // given
    val extra = integerType("extra")
    val obj = objectType(allowAdditionalProperties = true, additionalPropertiesDataType = extra)

    // when
    val shape = EncodingShape.of(obj).assertSuccess()

    // then
    val o = shape as Object
    assert(PlainTextSerde.deserialize("42", o.additionalProperties.itemType!!).assertSuccess() == BigDecimal("42"))
  }
}
