package dev.blitzcraft.contracts.core

import dev.blitzcraft.contracts.core.datatype.ArrayDataType
import dev.blitzcraft.contracts.core.datatype.BooleanDataType
import dev.blitzcraft.contracts.core.datatype.IntegerDataType
import dev.blitzcraft.contracts.core.datatype.ObjectDataType
import kotlin.test.Test
import kotlin.test.assertFailsWith

class JsonPathMatcherTest {

  @Test
  fun `Regex Matcher fails when root element is not Object nor Array`() {
    assertFailsWith(IllegalArgumentException::class) { JsonPathMatcher.regexMatchers(IntegerDataType()) }
  }

  @Test
  fun `generate Json Path Regex Matchers for Object with simple property and nested object`() {
    // given
    val objectDataType = ObjectDataType(mapOf(
      "productId" to Property(IntegerDataType()),
      "user" to Property(ObjectDataType(mapOf(
        "id" to Property(IntegerDataType()),
        "enabled" to Property(BooleanDataType())))))
    )

    // when
    val jsonPaths = JsonPathMatcher.regexMatchers(objectDataType)

    // then
    assert(jsonPaths.size == 3)
    assert(jsonPaths.containsAll(listOf(
      "$[?(@['productId'] =~ /-?(\\d+)/)]",
      "$['user'][?(@['id'] =~ /-?(\\d+)/)]",
      "$['user'][?(@['enabled'] =~ /(true|false)/)]"
    )))
  }

  @Test
  fun `generate Json Path Regex Matchers for Object with property of type Array of Object`() {
    // given
    val objectDataType = ObjectDataType(mapOf(
      "users" to Property(ArrayDataType(ObjectDataType(mapOf(
        "id" to Property(IntegerDataType())
      ))))))

    // when
    val jsonPaths = JsonPathMatcher.regexMatchers(objectDataType)

    // then
    assert(jsonPaths.size == 1)
    assert(jsonPaths[0] == "$['users'][*][?(@['id'] =~ /-?(\\d+)/)]")
  }

  @Test
  fun `generate Json Path Regex Matchers for Object with property of type Array of Object with a property of type Array`() {
    // given
    val objectDataType = ObjectDataType(mapOf(
      "users" to Property(ArrayDataType(ObjectDataType(mapOf(
        "productIds" to Property(ArrayDataType(IntegerDataType())))))))
    )

    // when
    val jsonPaths = JsonPathMatcher.regexMatchers(objectDataType)

    // then
    assert(jsonPaths.size == 1)
    assert(jsonPaths[0] == "$['users'][*]['productIds'][?(@ =~ /-?(\\d+)/)]")
  }

  @Test
  fun `generate Json Path Regex Matchers for Object with a property of type Array of Object with property of type Array of Array`() {
    // given
    val objectDataType = ObjectDataType(mapOf(
      "users" to Property(ArrayDataType(ObjectDataType(mapOf(
        "productIds" to Property(ArrayDataType(ArrayDataType( IntegerDataType())))))))))

    // when
    val jsonPaths = JsonPathMatcher.regexMatchers(objectDataType)

    // then
    assert(jsonPaths.size == 1)
    assert(jsonPaths[0] == "$['users'][*]['productIds'][*][?(@ =~ /-?(\\d+)/)]")
  }

  @Test
  fun `generate Json Path Regex Matchers for Array of Integer`() {
    // given
    val arrayDataType = ArrayDataType(IntegerDataType())

    // when
    val jsonPaths = JsonPathMatcher.regexMatchers(arrayDataType)

    // then
    assert(jsonPaths.size == 1)
    assert(jsonPaths[0] == "$[?(@ =~ /-?(\\d+)/)]")
  }

  @Test
  fun `generate Json Path Regex Matchers for Array of Object`() {
    // given
    val arrayDataType = ArrayDataType(ObjectDataType(mapOf("id" to Property(IntegerDataType()))))

    // when
    val jsonPaths = JsonPathMatcher.regexMatchers(arrayDataType)

    // then
    assert(jsonPaths.size == 1)
    assert(jsonPaths[0] == "$[*][?(@['id'] =~ /-?(\\d+)/)]")
  }

  @Test
  fun `generate Json Path Regex Matchers for Array of Array`() {
    // given
    val arrayDataType = ArrayDataType(ArrayDataType(IntegerDataType()))

    // when
    val jsonPaths = JsonPathMatcher.regexMatchers(arrayDataType)

    // then
    assert(jsonPaths.size == 1)
    assert(jsonPaths[0] == "$[*][?(@ =~ /-?(\\d+)/)]")
  }

  @Test
  fun `Value Matcher fails when root element is not Object nor Array`() {
    assertFailsWith(IllegalArgumentException::class) { JsonPathMatcher.exampleMatchers(2) }
  }

  @Test
  fun `generate Json Path Value Matcher for Object with simple property and nested object`() {
    // given
    val anObject = mapOf(
      "productId" to 99,
      "user" to mapOf(
        "id" to 42,
        "enabled" to false
      )
    )

    // when
    val jsonPaths = JsonPathMatcher.exampleMatchers(anObject)

    // then
    assert(jsonPaths.size == 3)
    assert(jsonPaths.containsAll(listOf(
      "$[?(@['productId'] == 99)]",
      "$['user'][?(@['id'] == 42)]",
      "$['user'][?(@['enabled'] == false)]"
    )))
  }

  @Test
  fun `generate Json Path Value Matcher for Object with a property of type Array`() {
    // given
    val anObject = mapOf(
      "user" to mapOf(
        "products" to arrayOf(1, 2, 3)
      )
    )

    // when
    val jsonPaths = JsonPathMatcher.exampleMatchers(anObject)

    // then
    assert(jsonPaths.size == 3)
    assert(jsonPaths.containsAll(listOf(
      "$['user']['products'][?(@ == 1)]",
      "$['user']['products'][?(@ == 2)]",
      "$['user']['products'][?(@ == 3)]"
    )))
  }

  @Test
  fun `generate Json Path Value Matcher for Object with a property of type Array of Object`() {
    // given
    val anObject = mapOf(
      "user" to mapOf(
        "products" to arrayOf(
          mapOf("id" to 1),
          mapOf("id" to 2),
          mapOf("id" to 3)
        )
      )
    )

    // when
    val jsonPaths = JsonPathMatcher.exampleMatchers(anObject)

    // then
    assert(jsonPaths.size == 3)
    assert(jsonPaths.containsAll(listOf(
      "$['user']['products'][*][?(@['id'] == 1)]",
      "$['user']['products'][*][?(@['id'] == 2)]",
      "$['user']['products'][*][?(@['id'] == 3)]"
    )))
  }

  @Test
  fun `generate Json Path Value Matcher for Object with a property of type Array of Array`() {
    // given
    val anObject = mapOf(
      "user" to mapOf(
        "products" to arrayOf(
          arrayOf(1, 2),
          arrayOf(3, 4)
        )
      )
    )

    // when
    val jsonPaths = JsonPathMatcher.exampleMatchers(anObject)

    // then
    assert(jsonPaths.size == 4)
    assert(jsonPaths.containsAll(listOf(
      "$['user']['products'][*][?(@ == 1)]",
      "$['user']['products'][*][?(@ == 2)]",
      "$['user']['products'][*][?(@ == 3)]",
      "$['user']['products'][*][?(@ == 4)]"
    )))
  }

  @Test
  fun `generate Json Path Value Matcher for Array`() {
    // given
    val anObject = arrayOf(1, 2, 3)

    // when
    val jsonPaths = JsonPathMatcher.exampleMatchers(anObject)

    // then
    assert(jsonPaths.size == 3)
    assert(jsonPaths.containsAll(listOf(
      "$[?(@ == 1)]",
      "$[?(@ == 2)]",
      "$[?(@ == 3)]"
    )))
  }
  @Test
  fun `generate Json Path Value Matcher for Array of Array`() {
    // given
    val anObject = arrayOf(arrayOf(1,2), arrayOf(3, 4))

    // when
    val jsonPaths = JsonPathMatcher.exampleMatchers(anObject)

    // then
    assert(jsonPaths.size == 4)
    assert(jsonPaths.containsAll(listOf(
      "$[*][?(@ == 1)]",
      "$[*][?(@ == 2)]",
      "$[*][?(@ == 3)]",
      "$[*][?(@ == 4)]"
    )))
  }
  @Test
  fun `generate Json Path Value Matcher for Array of Object`() {
    // given
    val anObject = arrayOf(
      mapOf("id" to 1),
      mapOf("id" to 2),
      mapOf("id" to 3)
    )

    // when
    val jsonPaths = JsonPathMatcher.exampleMatchers(anObject)

    // then
    assert(jsonPaths.size == 3)
    assert(jsonPaths.containsAll(listOf(
      "$[*][?(@['id'] == 1)]",
      "$[*][?(@['id'] == 2)]",
      "$[*][?(@['id'] == 3)]"
    )))
  }
}

