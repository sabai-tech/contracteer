package tech.sabai.contracteer.core.serde

import tech.sabai.contracteer.core.TestFixture.anyOfDataType
import tech.sabai.contracteer.core.TestFixture.arrayDataType
import tech.sabai.contracteer.core.TestFixture.booleanDataType
import tech.sabai.contracteer.core.TestFixture.integerDataType
import tech.sabai.contracteer.core.TestFixture.numberDataType
import tech.sabai.contracteer.core.TestFixture.objectDataType
import tech.sabai.contracteer.core.TestFixture.oneOfDataType
import tech.sabai.contracteer.core.TestFixture.stringDataType
import kotlin.test.Test

class PlainTextSerdeTest {

  @Test
  fun `successfully deserializes null value`() {
    // when
    val result = PlainTextSerde.deserialize(null, integerDataType())

    // then
    assert(result.isSuccess())

  }

  @Test
  fun `fails to deserialize to ArrayDatatype`() {
    // when
    val result = PlainTextSerde.deserialize("[1,2,3]", arrayDataType(itemDataType = integerDataType()))

    // then
    assert(result.isFailure())
  }

  @Test
  fun `fails to deserialize to ObjectDatatype`() {
    // when
    val result =
      PlainTextSerde.deserialize("{\"prop1\":42}", objectDataType(properties = mapOf("prop1" to integerDataType())))

    // then
    assert(result.isFailure())
  }


  @Test
  fun `successfully deserializes to IntegerDatatype`() {
    // when
    val result = PlainTextSerde.deserialize("42", integerDataType())

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `successfully deserializes to NumberDatatype`() {
    // when
    val result = PlainTextSerde.deserialize("42.5", numberDataType())

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `successfully deserializes to StringDatatype`() {
    // when
    val result = PlainTextSerde.deserialize("test", stringDataType())

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `successfully deserializes to BooleanDatatype`() {
    // when
    val result = PlainTextSerde.deserialize("true", booleanDataType())

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `fails to deserialize invalid value to BooleanDatatype`() {
    // when
    val result = PlainTextSerde.deserialize("notABoolean", booleanDataType())

    // then
    assert(result.isFailure())
  }

  @Test
  fun `fails to deserialize invalid value to IntegerDatatype`() {
    // when
    val result = PlainTextSerde.deserialize("notAnInteger", integerDataType())

    // then
    assert(result.isFailure())
  }

  @Test
  fun `fails to deserialize invalid value to NumberDatatype`() {
    // when
    val result = PlainTextSerde.deserialize("notANumber", numberDataType())

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
    val result = PlainTextSerde.deserialize("123", oneOfDataType(subTypes = listOf(stringDataType(), integerDataType())))

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `successfully deserializes anyOf with primitive subtypes`() {
    // when
    val result = PlainTextSerde.deserialize("true", anyOfDataType(subTypes = listOf(stringDataType(), booleanDataType())))

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `successfully deserializes composite with mixed object and primitive subtypes`() {
    // when
    val result = PlainTextSerde.deserialize(
      "hello",
      oneOfDataType(subTypes = listOf(
        objectDataType(properties = mapOf("a" to stringDataType())),
        stringDataType()
      ))
    )

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `fails to deserialize composite with only object subtypes`() {
    // when
    val result = PlainTextSerde.deserialize(
      "test",
      oneOfDataType(subTypes = listOf(
        objectDataType(properties = mapOf("a" to stringDataType())),
        objectDataType(properties = mapOf("b" to integerDataType()))
      ))
    )

    // then
    assert(result.isFailure())
  }
}