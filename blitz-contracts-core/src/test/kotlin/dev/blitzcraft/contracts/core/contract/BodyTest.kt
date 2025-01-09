package dev.blitzcraft.contracts.core.contract

import dev.blitzcraft.contracts.core.datatype.ArrayDataType
import dev.blitzcraft.contracts.core.datatype.IntegerDataType
import dev.blitzcraft.contracts.core.datatype.ObjectDataType
import dev.blitzcraft.contracts.core.datatype.StringDataType
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.Test
import kotlin.test.assertFailsWith

class BodyTest {

  @Test
  fun `fails when DataType is not of type Object or Array for Json Content-Type`() {
    // expect
    assertFailsWith(IllegalArgumentException::class) { Body("application/json", IntegerDataType()) }
  }

  @Test
  fun `fails when Example value is not of type Map or Array for Json Content-Type`() {
    // expect
    assertFailsWith(IllegalArgumentException::class) {
      Body("application/json", ObjectDataType(properties = mapOf("id" to IntegerDataType())), Example(42))
    }
  }

  @Test
  fun `accepts ObjectDataType as Body content`() {
    // expect
    assertDoesNotThrow {
      Body("application/json", ObjectDataType(properties = mapOf("id" to IntegerDataType())))
    }
  }

  @Test
  fun `accepts ArrayDataType as Body content`() {
    // expect
    assertDoesNotThrow { Body("application/json", ArrayDataType(itemDataType = IntegerDataType())) }
  }

  @Test
  fun `accepts Map as Body content Example`() {
    // expect
    assertDoesNotThrow {
      Body("application/json",
           ObjectDataType(properties = mapOf("id" to IntegerDataType())),
           Example(mapOf("id" to 42)))
    }
  }

  @Test
  fun `accepts Array as Body content Example`() {
    // expect
    assertDoesNotThrow {
      Body("application/json", ObjectDataType(properties = mapOf("id" to IntegerDataType())), Example(arrayOf(1, 2, 3)))
    }
  }

  @Test
  fun `null string matches nullable Body`() {
    // given
    val body = Body(
      contentType = "application/json",
      dataType = ObjectDataType(
        properties = mapOf("id" to IntegerDataType()),
        isNullable = true
      ),
    )

    // when
    val result = null.matches(body)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `null string does not match non nullable Body`() {
    // given
    val body = Body(
      contentType = "application/json",
      dataType = ObjectDataType(
        isNullable = false,
        properties = mapOf("id" to IntegerDataType())))

    // when
    val result = null.matches(body)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `string does not match when content-type is not json`() {
    // given
    val body = Body(
      contentType = "text/plain",
      dataType = IntegerDataType()
    )

    // when
    val result = "42".matches(body)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `Json Object string matches body when all properties match`() {
    // given
    val body = Body(
      contentType = "application/json",
      dataType = ObjectDataType(
        properties = mapOf(
          "foo" to IntegerDataType(),
          "bar" to IntegerDataType()
        )
      )
    )

    // when
    val result = """{"foo": 42, "bar": 99}""".trimIndent().matches(body)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `Json Object string does not match body when some properties do not match`() {
    // given
    val body = Body(
      contentType = "application/json",
      dataType = ObjectDataType(
        properties = mapOf(
          "foo" to IntegerDataType(),
          "bar" to IntegerDataType()
        )
      )
    )

    // when
    val result = """{"foo": 'John', "bar": true}""".trimIndent().matches(body)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `Json Array matches when all items match`() {
    // given
    val body = Body(
      contentType = "application/json",
      dataType = ArrayDataType(itemDataType = IntegerDataType())
    )

    // when
    val result = """[42, 99]""".trimIndent().matches(body)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `Json Array does not match when some items do not match`() {
    // given
    val body = Body(
      contentType = "application/json",
      dataType = ArrayDataType(itemDataType = IntegerDataType())
    )

    // when
    val result = """[42, "John", 100]""".trimIndent().matches(body)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `null string matches example body with null value`() {
    // given
    val body = Body(
      contentType = "application/json",
      dataType = ObjectDataType(
        properties = mapOf("name" to StringDataType()),
        isNullable = true
      ),
      example = Example(null)
    )
    // when
    val result = null.matchesExample(body)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `null string does match example body with non null value`() {
    // given
    val body = Body(
      contentType = "application/json",
      dataType = ObjectDataType(
        properties = mapOf("name" to StringDataType())
      ),
      example = Example(mapOf("name" to "John"))
    )
    // when
    val result = null.matchesExample(body)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `Json Object string matches Body example value`() {
    // given
    val body = Body(
      contentType = "application/json",
      dataType = ObjectDataType(
        properties = mapOf("name" to StringDataType())
      ),
      example = Example(mapOf("name" to "John"))
    )
    // when
    val result = """{"name":"John"}""".matchesExample(body)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `Json Object string does not match when some property are equals to example value`() {
    // given
    val body = Body(
      contentType = "application/json",
      dataType = ObjectDataType(
        properties = mapOf(
          "name" to StringDataType(),
          "age" to IntegerDataType()
        )
      ),
      example = Example(mapOf("name" to "John", "age" to 20))

    )
    // when
    val result = """{"name":"John","age":42}""".matchesExample(body)

    // then
    assert(result.isFailure())
    assert(result.errors().size == 1)
    listOf("age", "20", "42").all { result.errors().first().contains(it) }
  }

  @Test
  fun `Json Object string does not match when some example properties are missing`() {
    // given
    val body = Body(
      contentType = "application/json",
      dataType = ObjectDataType(
        properties = mapOf(
          "name" to StringDataType(),
          "age" to IntegerDataType()
        )
      ),
      example = Example(mapOf("name" to "John", "age" to 42))
    )
    // when
    val result = """{"name":"John"}""".matchesExample(body)

    // then
    assert(result.isFailure())
    assert(result.errors().size == 1)
    listOf("age", "is missing").all { result.errors().first().contains(it) }
  }

  @Test
  fun `Json Array string matches body example value`() {
    // given
    val body = Body(
      contentType = "application/json",
      dataType = ArrayDataType(itemDataType = IntegerDataType()),
      example = Example(arrayOf(20, 42))
    )
    // when
    val result = """[20,42]""".matchesExample(body)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `Json Array string does not match body example value`() {
    // given
    val body = Body(
      contentType = "application/json",
      dataType = ArrayDataType(itemDataType = IntegerDataType()),
      example = Example(arrayOf(20, 42))
    )

    // when
    val result = """[20,43]""".matchesExample(body)

    // then
    assert(result.isFailure())
    assert(result.errors().size == 1)
    assert(listOf("1", "42", "43").all { result.errors().first().contains(it) })
  }
}
