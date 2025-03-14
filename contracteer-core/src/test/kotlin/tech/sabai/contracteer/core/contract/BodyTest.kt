package tech.sabai.contracteer.core.contract

import org.junit.jupiter.api.Nested
import tech.sabai.contracteer.core.TestFixture.arrayDataType
import tech.sabai.contracteer.core.TestFixture.body
import tech.sabai.contracteer.core.TestFixture.integerDataType
import tech.sabai.contracteer.core.TestFixture.objectDataType
import tech.sabai.contracteer.core.TestFixture.stringDataType
import kotlin.test.Test

class BodyTest {

  @Test
  fun `creation fails when content-type and datatype are not compatible`() {
    // when
    val result = Body.create(ContentType("application/json"), dataType = integerDataType())

    // then
    assert(result.isFailure())
  }

  @Test
  fun `validation fails for null value when not nullable`() {
    // given
    val body = body(
      contentType = ContentType("application/json"),
      dataType = objectDataType(
        isNullable = false,
        properties = mapOf("id" to integerDataType()))
    )

    // when
    val result = body.validate(null)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `validation succeeds for json object when content-type is json`() {
    // given
    val body = body(
      contentType = ContentType("application/json"),
      dataType = objectDataType(
        properties = mapOf(
          "foo" to integerDataType(),
          "bar" to integerDataType()))
    )

    // when
    val result = body.validate("""{"foo": 42, "bar": 99}""".trimIndent())

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validation succeeds for json array when content-type is json`() {
    // given
    val body = body(
      contentType = ContentType("application/json"),
      dataType = arrayDataType(itemDataType = integerDataType())
    )

    // when
    val result = body.validate("""[42, 99]""".trimIndent())

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validation fails for non json value when content-type is json`() {
    // given
    val body = body(
      contentType = ContentType("application/json"),
      dataType = arrayDataType(itemDataType = integerDataType())
    )

    // when
    val result = body.validate("""John""".trimIndent())

    // then
    assert(result.isFailure())
  }

  @Test
  fun `validation succeeds for string value when content-type is not json `() {
    // given
    val body = body(
      contentType = ContentType("plain/text"),
      dataType = stringDataType()
    )

    // when
    val result = body.validate("""John""".trimIndent())

    // then
    assert(result.isSuccess())
  }

  @Nested
  inner class WithExample {

    @Test
    fun `creation fails when example is not of the expected type`() {
      // when
      val result = Body.create(
        contentType = ContentType("application/json"),
        dataType = objectDataType(properties = mapOf("example" to integerDataType())),
        example = Example(42)
      )

      // then
      assert(result.isFailure())
    }

    @Test
    fun `validation succeeds for null value when body example has null value`() {
      // given
      val body = body(
        contentType = ContentType("application/json"),
        dataType = objectDataType(
          properties = mapOf("name" to stringDataType()),
          isNullable = true
        ),
        example = Example(null)
      )
      // when
      val result = body.validate(null)

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `validation fails for null value when example value is not null`() {
      // given
      val body = body(
        contentType = ContentType("application/json"),
        dataType = objectDataType(
          properties = mapOf("name" to stringDataType())
        ),
        example = Example(mapOf("name" to "John"))
      )
      // when
      val result = body.validate(null)

      // then
      assert(result.isFailure())
    }

    @Test
    fun `validation succeeds when json object is equal to the example value`() {
      // given
      val body = body(
        contentType = ContentType("application/json"),
        dataType = objectDataType(
          properties = mapOf("name" to stringDataType())
        ),
        example = Example(mapOf("name" to "John"))
      )
      // when
      val result = body.validate("""{"name":"John"}""")

      // then
      assert(result.isSuccess())
    }


    @Test
    fun `validation fails when json object value does not match example value`() {
      // given
      val body = body(
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
      val result = body.validate("""{"name":"John","age":42}""")

      // then
      assert(result.isFailure())
      assert(result.errors().size == 1)
      listOf("age", "20", "42").all { result.errors().first().contains(it) }
    }

    @Test
    fun `validation fails when json array does not match the example value`() {
      // given
      val body = body(
        contentType = ContentType("application/json"),
        dataType = arrayDataType(itemDataType = integerDataType()),
        example = Example(listOf(42, 99, 200))
      )

      // when
      val result = body.validate("""[42, 99]""".trimIndent())

      // then
      assert(result.isFailure())
    }

    @Test
    fun `validation succeeds when json array is equal to the example value`() {
      // given
      val body = body(
        contentType = ContentType("application/json"),
        dataType = arrayDataType(itemDataType = integerDataType()),
        example = Example(listOf(42, 99))
      )

      // when
      val result = body.validate("""[42, 99]""".trimIndent())

      // then
      assert(result.isSuccess())
    }
  }
}
