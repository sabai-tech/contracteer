package dev.blitzcraft.contracts.core.datatype

import dev.blitzcraft.contracts.core.Property
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.StringSchema
import java.math.BigInteger
import kotlin.test.Test

class ObjectDataTypeTest {

  @Test
  fun `generate value with nested object`() {
    // given
    val objectSchema = ObjectSchema().properties(mapOf(
      "id" to IntegerSchema(),
      "user" to ObjectSchema().properties(mapOf(
        "name" to StringSchema())))
    ) as ObjectSchema

    // when
    val value = ObjectDataType(objectSchema).nextValue()

    // then
    assert(value["id"] is BigInteger)
    assert(value["user"] is Map<*, *>)
    assert((value["user"] as Map<*, *>)["name"] is String)
  }

  @Test
  fun `does not validate when value is not of type Object`() {
    // given
    val objectDataType = ObjectDataType(listOf(Property("id", IntegerDataType())))

    // when
    val result = objectDataType.validateValue("a string")

    // expect
    assert(result.isSuccess().not())
  }

  @Test
  fun `does not validate Object when one property is not of the right type`() {
    // given
    val objectDataType = ObjectDataType(listOf(
      Property("id", IntegerDataType()),
      Property("userId", StringDataType())
    ))

    // when
    val result = objectDataType.validateValue(mapOf(
      "id" to "myId",
      "userId" to "myUserId"
    ))

    // expect
    assert(result.isSuccess().not())
    assert(result.errors().size == 1)
    assert(result.errors().first().startsWith("id"))
  }

  @Test
  fun `does not validate Object with a missing required property`() {
    // given
    val objectDataType = ObjectDataType(listOf(
      Property("id", IntegerDataType()),
      Property("userId", StringDataType(), required = true)
    ))

    // when
    val result = objectDataType.validateValue(mapOf("id" to 123))

    // expect
    assert(result.isSuccess().not())
    assert(result.errors().size == 1)
    assert(result.errors().first().startsWith("userId"))
  }
}