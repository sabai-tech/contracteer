package dev.blitzcraft.contracts.core.datatype

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
}