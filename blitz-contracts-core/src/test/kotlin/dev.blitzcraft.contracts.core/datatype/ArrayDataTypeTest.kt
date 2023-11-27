package dev.blitzcraft.contracts.core.datatype

import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.StringSchema
import java.math.BigInteger
import kotlin.test.Test

class ArrayDataTypeTest {


  @Test
  fun `generate array of object`() {
    // given
    val arraySchema = ArraySchema().items(ObjectSchema().properties(mapOf(
      "id" to IntegerSchema(),
      "user" to ObjectSchema().properties(mapOf(
        "name" to StringSchema(),
        "products" to ArraySchema().items(IntegerSchema())
      )))))

    // when
    val value = ArrayDataType(arraySchema).nextValue()

    // then
    assert(value.isNotEmpty())
    value.forEach {
      assert(it is Map<*, *>)
      assert((it as Map<*, *>)["id"] is BigInteger)
      assert(it["user"] is Map<*, *>)
      assert((it["user"] as Map<*, *>)["name"] is String)
      assert((it["user"] as Map<*, *>)["products"] is Array<*>)
    }
  }
}
