package tech.sabai.contracteer.core.contract

import tech.sabai.contracteer.core.DataTypeFixture.arrayDataType
import tech.sabai.contracteer.core.DataTypeFixture.integerDataType
import tech.sabai.contracteer.core.DataTypeFixture.objectDataType
import tech.sabai.contracteer.core.DataTypeFixture.stringDataType
import kotlin.test.Test

class BodyTest {

  @Test
  fun `null string matches nullable Body`() {
    // given
    val body = Body(
      contentType = ContentType("application/json"),
      dataType = objectDataType(properties = mapOf("id" to integerDataType()), isNullable = true)
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
      contentType = ContentType("application/json"),
      dataType = objectDataType(
        isNullable = false,
        properties = mapOf("id" to integerDataType()))
    )

    // when
    val result = null.matches(body)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `string does not match when content-type is not json`() {
    // given
    val body = Body(
      contentType = ContentType("text/plain"),
      dataType = stringDataType()
    )

    // when
    val result = "42".matches(body)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `Json Object string matches body when all properties match`() {
    // given
    val body = Body(
      contentType = ContentType("application/json"),
      dataType = objectDataType(
        properties = mapOf(
          "foo" to integerDataType(),
          "bar" to integerDataType()))
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
      contentType = ContentType("application/json"),
      dataType = objectDataType(
        properties = mapOf(
          "foo" to integerDataType(),
          "bar" to integerDataType()))
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
      contentType = ContentType("application/json"),
      dataType = arrayDataType(itemDataType = integerDataType())
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
      contentType = ContentType("application/json"),
      dataType = arrayDataType(itemDataType = integerDataType())
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
      contentType = ContentType("application/json"),
      dataType = objectDataType(
        properties = mapOf("name" to stringDataType()),
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
      contentType = ContentType("application/json"),
      dataType = objectDataType(
        properties = mapOf("name" to stringDataType())
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
      contentType = ContentType("application/json"),
      dataType = objectDataType(
        properties = mapOf("name" to stringDataType())
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
      contentType = ContentType("application/json"),
      dataType = objectDataType(
        properties = mapOf(
          "name" to stringDataType(),
          "age" to integerDataType()
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
      contentType = ContentType("application/json"),
      dataType = objectDataType(
        properties = mapOf(
          "name" to stringDataType(),
          "age" to integerDataType()
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
      contentType = ContentType("application/json"),
      dataType = arrayDataType(itemDataType = integerDataType()),
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
      contentType = ContentType("application/json"),
      dataType = arrayDataType(itemDataType = integerDataType()),
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
