package dev.contracteer.core.codec

import dev.contracteer.core.assertSuccess
import dev.contracteer.core.dsl.allOfType
import dev.contracteer.core.dsl.anyOfType
import dev.contracteer.core.dsl.arrayType
import dev.contracteer.core.dsl.integerType
import dev.contracteer.core.dsl.objectType
import dev.contracteer.core.dsl.stringType
import dev.contracteer.core.rgbObjectType
import kotlin.test.Test

class SimpleParameterCodecTest {

  @Test
  fun `encode primitive`() {
    assert(SimpleParameterCodec("color", explode = false).encode("blue") == listOf("color" to "blue"))
  }

  @Test
  fun `encode array with explode false`() {
    assert(SimpleParameterCodec("color", explode = false).encode(listOf("blue", "black", "brown")) == listOf("color" to "blue,black,brown"))
  }

  @Test
  fun `encode array with explode true`() {
    assert(SimpleParameterCodec("color", explode = true).encode(listOf("blue", "black", "brown")) == listOf("color" to "blue,black,brown"))
  }

  @Test
  fun `encode object with explode false`() {
    assert(SimpleParameterCodec("color", explode = false).encode(mapOf("R" to 100, "G" to 200, "B" to 150)) == listOf("color" to "R,100,G,200,B,150"))
  }

  @Test
  fun `encode object with explode true`() {
    assert(SimpleParameterCodec("color", explode = true).encode(mapOf("R" to 100, "G" to 200, "B" to 150)) == listOf("color" to "R=100,G=200,B=150"))
  }

  @Test
  fun `decode primitive`() {
    // given
    val values = mapOf("color" to listOf("blue"))

    // when
    val result = SimpleParameterCodec("color", explode = false).decode(values, stringType())

    // then
    assert(result.assertSuccess() =="blue")
  }

  @Test
  fun `decode array with explode false`() {
    // given
    val values = mapOf("color" to listOf("blue,black,brown"))

    // when
    val result = SimpleParameterCodec("color", explode = false).decode(values, arrayType(items = stringType()))

    // then
    assert(result.assertSuccess() ==listOf("blue", "black", "brown"))
  }

  @Test
  fun `decode array with explode true`() {
    // given
    val values = mapOf("color" to listOf("blue,black,brown"))

    // when
    val result = SimpleParameterCodec("color", explode = true).decode(values, arrayType(items = stringType()))

    // then
    assert(result.assertSuccess() ==listOf("blue", "black", "brown"))
  }

  @Test
  fun `decode object with explode false`() {
    // given
    val values = mapOf("color" to listOf("R,100,G,200,B,150"))

    // when
    val result = SimpleParameterCodec("color", explode = false).decode(values, rgbObjectType())

    // then
    val obj = result.assertSuccess() as Map<*, *>
    assert(obj["R"] == 100.toBigDecimal())
    assert(obj["G"] == 200.toBigDecimal())
    assert(obj["B"] == 150.toBigDecimal())
  }

  @Test
  fun `decode object with explode true`() {
    // given
    val values = mapOf("color" to listOf("R=100,G=200,B=150"))

    // when
    val result = SimpleParameterCodec("color", explode = true).decode(values, rgbObjectType())

    // then
    val obj = result.assertSuccess() as Map<*, *>
    assert(obj["R"] == 100.toBigDecimal())
    assert(obj["G"] == 200.toBigDecimal())
    assert(obj["B"] == 150.toBigDecimal())
  }

  @Test
  fun `decode returns null when value is absent`() {
    // when
    val result = SimpleParameterCodec("color", explode = false).decode(emptyMap(), stringType())

    // then
    assert(result.assertSuccess() ==null)
  }

  @Test
  fun `decode allOf of objects with explode false`() {
    // given
    val schema = allOfType {
      subType(objectType("base") { properties { "name" to stringType() } })
      subType(objectType("extra") { properties { "age" to integerType() } })
    }
    val values = mapOf("user" to listOf("name,ada,age,36"))

    // when
    val result = SimpleParameterCodec("user", explode = false).decode(values, schema)

    // then
    val obj = result.assertSuccess() as Map<*, *>
    assert(obj["name"] == "ada")
    assert(obj["age"] == 36.toBigDecimal())
  }

  @Test
  fun `decode allOf of objects with explode true`() {
    // given
    val schema = allOfType {
      subType(objectType("base") { properties { "name" to stringType() } })
      subType(objectType("extra") { properties { "age" to integerType() } })
    }
    val values = mapOf("user" to listOf("name=ada,age=36"))

    // when
    val result = SimpleParameterCodec("user", explode = true).decode(values, schema)

    // then
    val obj = result.assertSuccess() as Map<*, *>
    assert(obj["name"] == "ada")
    assert(obj["age"] == 36.toBigDecimal())
  }

  @Test
  fun `decode anyOf of arrays`() {
    // given
    val schema = anyOfType {
      subType(arrayType(items = stringType()))
      subType(arrayType(items = stringType()))
    }
    val values = mapOf("color" to listOf("blue,black,brown"))

    // when
    val result = SimpleParameterCodec("color", explode = false).decode(values, schema)

    // then
    assert(result.assertSuccess() == listOf("blue", "black", "brown"))
  }
}
