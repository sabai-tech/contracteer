package tech.sabai.contracteer.core.codec

import tech.sabai.contracteer.core.assertSuccess
import tech.sabai.contracteer.core.dsl.allOfType
import tech.sabai.contracteer.core.dsl.anyOfType
import tech.sabai.contracteer.core.dsl.arrayType
import tech.sabai.contracteer.core.dsl.integerType
import tech.sabai.contracteer.core.dsl.objectType
import tech.sabai.contracteer.core.dsl.stringType
import tech.sabai.contracteer.core.rgbObjectType
import kotlin.test.Test

class MatrixParameterCodecTest {

  @Test
  fun `encode primitive`() {
    assert(MatrixParameterCodec("color", explode = false).encode("blue") == listOf("color" to ";color=blue"))
  }

  @Test
  fun `encode array with explode false`() {
    val result = MatrixParameterCodec("color", explode = false).encode(listOf("blue", "black", "brown"))
    assert(result == listOf("color" to ";color=blue,black,brown"))
  }

  @Test
  fun `encode array with explode true`() {
    val result = MatrixParameterCodec("color", explode = true).encode(listOf("blue", "black", "brown"))
    assert(result == listOf("color" to ";color=blue;color=black;color=brown"))
  }

  @Test
  fun `encode object with explode false`() {
    val result = MatrixParameterCodec("color", explode = false).encode(mapOf("R" to 100, "G" to 200, "B" to 150))
    assert(result == listOf("color" to ";color=R,100,G,200,B,150"))
  }

  @Test
  fun `encode object with explode true`() {
    val result = MatrixParameterCodec("color", explode = true).encode(mapOf("R" to 100, "G" to 200, "B" to 150))
    assert(result == listOf("color" to ";R=100;G=200;B=150"))
  }

  @Test
  fun `decode primitive`() {
    // given
    val values = mapOf("color" to listOf(";color=blue"))

    // when
    val result = MatrixParameterCodec("color", explode = false).decode(values, stringType())

    // then
    assert(result.assertSuccess() =="blue")
  }

  @Test
  fun `decode array with explode false`() {
    // given
    val values = mapOf("color" to listOf(";color=blue,black,brown"))

    // when
    val result = MatrixParameterCodec("color", explode = false).decode(values, arrayType(items = stringType()))

    // then
    assert(result.assertSuccess() ==listOf("blue", "black", "brown"))
  }

  @Test
  fun `decode array with explode true`() {
    // given
    val values = mapOf("color" to listOf(";color=blue;color=black;color=brown"))

    // when
    val result = MatrixParameterCodec("color", explode = true).decode(values, arrayType(items = stringType()))

    // then
    assert(result.assertSuccess() ==listOf("blue", "black", "brown"))
  }

  @Test
  fun `decode object with explode false`() {
    // given
    val values = mapOf("color" to listOf(";color=R,100,G,200,B,150"))

    // when
    val result = MatrixParameterCodec("color", explode = false).decode(values, rgbObjectType())

    // then
    val obj = result.assertSuccess() as Map<*, *>
    assert(obj["R"] == 100.toBigDecimal())
    assert(obj["G"] == 200.toBigDecimal())
    assert(obj["B"] == 150.toBigDecimal())
  }

  @Test
  fun `decode object with explode true`() {
    // given
    val values = mapOf("color" to listOf(";R=100;G=200;B=150"))

    // when
    val result = MatrixParameterCodec("color", explode = true).decode(values, rgbObjectType())

    // then
    val obj = result.assertSuccess() as Map<*, *>
    assert(obj["R"] == 100.toBigDecimal())
    assert(obj["G"] == 200.toBigDecimal())
    assert(obj["B"] == 150.toBigDecimal())
  }

  @Test
  fun `decode returns null when value is absent`() {
    // when
    val result = MatrixParameterCodec("color", explode = false).decode(emptyMap(), stringType())

    // then
    assert(result.assertSuccess() ==null)
  }

  @Test
  fun `decode fails when value does not start with semicolon`() {
    // given
    val values = mapOf("color" to listOf("color=blue"))

    // when
    val result = MatrixParameterCodec("color", explode = false).decode(values, stringType())

    // then
    assert(result.isFailure())
  }

  @Test
  fun `decode allOf of objects with explode false`() {
    // given
    val schema = allOfType {
      subType(objectType("base") { properties { "name" to stringType() } })
      subType(objectType("extra") { properties { "age" to integerType() } })
    }
    val values = mapOf("user" to listOf(";user=name,ada,age,36"))

    // when
    val result = MatrixParameterCodec("user", explode = false).decode(values, schema)

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
    val values = mapOf("user" to listOf(";name=ada;age=36"))

    // when
    val result = MatrixParameterCodec("user", explode = true).decode(values, schema)

    // then
    val obj = result.assertSuccess() as Map<*, *>
    assert(obj["name"] == "ada")
    assert(obj["age"] == 36.toBigDecimal())
  }

  @Test
  fun `decode anyOf of arrays with explode false`() {
    // given
    val schema = anyOfType {
      subType(arrayType(items = stringType()))
      subType(arrayType(items = stringType()))
    }
    val values = mapOf("color" to listOf(";color=blue,black,brown"))

    // when
    val result = MatrixParameterCodec("color", explode = false).decode(values, schema)

    // then
    assert(result.assertSuccess() == listOf("blue", "black", "brown"))
  }

  @Test
  fun `decode anyOf of arrays with explode true`() {
    // given
    val schema = anyOfType {
      subType(arrayType(items = stringType()))
      subType(arrayType(items = stringType()))
    }
    val values = mapOf("color" to listOf(";color=blue;color=black;color=brown"))

    // when
    val result = MatrixParameterCodec("color", explode = true).decode(values, schema)

    // then
    assert(result.assertSuccess() == listOf("blue", "black", "brown"))
  }
}
