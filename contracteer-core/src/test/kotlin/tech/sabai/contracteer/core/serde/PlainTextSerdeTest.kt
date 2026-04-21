package tech.sabai.contracteer.core.serde

import tech.sabai.contracteer.core.dsl.anyOfType
import tech.sabai.contracteer.core.dsl.arrayType
import tech.sabai.contracteer.core.dsl.booleanType
import tech.sabai.contracteer.core.dsl.integerType
import tech.sabai.contracteer.core.dsl.numberType
import tech.sabai.contracteer.core.dsl.objectType
import tech.sabai.contracteer.core.dsl.oneOfType
import tech.sabai.contracteer.core.dsl.stringType
import kotlin.test.Test

class PlainTextSerdeTest {

  @Test
  fun `successfully deserializes null value`() {
    // when
    val result = PlainTextSerde.deserialize(null, integerType())

    // then
    assert(result.isSuccess())

  }

  @Test
  fun `fails to deserialize to ArrayDatatype`() {
    // when
    val result = PlainTextSerde.deserialize("[1,2,3]", arrayType(items = integerType()))

    // then
    assert(result.isFailure())
  }

  @Test
  fun `fails to deserialize to ObjectDatatype`() {
    // when
    val result =
      PlainTextSerde.deserialize("{\"prop1\":42}", objectType { properties { "prop1" to integerType() } })

    // then
    assert(result.isFailure())
  }


  @Test
  fun `successfully deserializes to IntegerDatatype`() {
    // when
    val result = PlainTextSerde.deserialize("42", integerType())

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `successfully deserializes to NumberDatatype`() {
    // when
    val result = PlainTextSerde.deserialize("42.5", numberType())

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `successfully deserializes to StringDatatype`() {
    // when
    val result = PlainTextSerde.deserialize("test", stringType())

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `successfully deserializes to BooleanDatatype`() {
    // when
    val result = PlainTextSerde.deserialize("true", booleanType())

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `fails to deserialize invalid value to BooleanDatatype`() {
    // when
    val result = PlainTextSerde.deserialize("notABoolean", booleanType())

    // then
    assert(result.isFailure())
  }

  @Test
  fun `fails to deserialize invalid value to IntegerDatatype`() {
    // when
    val result = PlainTextSerde.deserialize("notAnInteger", integerType())

    // then
    assert(result.isFailure())
  }

  @Test
  fun `fails to deserialize invalid value to NumberDatatype`() {
    // when
    val result = PlainTextSerde.deserialize("notANumber", numberType())

    // then
    assert(result.isFailure())
  }


  @Test
  fun `successfully serializes Integer`() {
    // when
    val result = PlainTextSerde.serialize(42)

    // then
    assert(result == "42")
  }

  @Test
  fun `successfully serializes String`() {
    // when
    val result = PlainTextSerde.serialize("test")

    // then
    assert(result == "test")
  }

  @Test
  fun `successfully serializes BigDecimal`() {
    // when
    val result = PlainTextSerde.serialize(42.5.toBigDecimal())

    // then
    assert(result == "42.5")
  }

  @Test
  fun `successfully serializes Boolean`() {
    // when
    val result = PlainTextSerde.serialize(true)

    // then
    assert(result == "true")
  }

  @Test
  fun `successfully deserializes oneOf with primitive subtypes`() {
    // when
    val result = PlainTextSerde.deserialize("123", oneOfType {
      subType(stringType())
      subType(integerType())
    })

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `successfully deserializes anyOf with primitive subtypes`() {
    // when
    val result = PlainTextSerde.deserialize("true", anyOfType {
      subType(stringType())
      subType(booleanType())
    })

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `successfully deserializes composite with mixed object and primitive subtypes`() {
    // when
    val result = PlainTextSerde.deserialize(
      "hello",
      oneOfType {
        subType(objectType { properties { "a" to stringType() } })
        subType(stringType())
      }
    )

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `fails to deserialize composite with only object subtypes`() {
    // when
    val result = PlainTextSerde.deserialize(
      "test",
      oneOfType {
        subType(objectType { properties { "a" to stringType() } })
        subType(objectType { properties { "b" to integerType() } })
      }
    )

    // then
    assert(result.isFailure())
  }
}
