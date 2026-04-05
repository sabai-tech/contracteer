package tech.sabai.contracteer.verifier

import tech.sabai.contracteer.core.datatype.*
import tech.sabai.contracteer.verifier.TestFixture.arrayDataType
import tech.sabai.contracteer.verifier.TestFixture.integerDataType
import tech.sabai.contracteer.verifier.TestFixture.objectDataType
import tech.sabai.contracteer.verifier.TestFixture.stringDataType
import kotlin.test.Test

class TypeMismatchMutationTest {

  @Test
  fun `produces invalid value for IntegerDataType`() {
    // Given
    val dataType = integerDataType()

    // When
    val result = TypeMismatchMutation.mutate(dataType)

    // Then
    assert(result == "<<not a integer>>")
  }

  @Test
  fun `produces invalid value for NumberDataType`() {
    // Given
    val dataType = NumberDataType.create("number").assertSuccess()

    // When
    val result = TypeMismatchMutation.mutate(dataType)

    // Then
    assert(result == "<<not a number>>")
  }

  @Test
  fun `produces invalid value for BooleanDataType`() {
    // Given
    val dataType = BooleanDataType.create("boolean").assertSuccess()

    // When
    val result = TypeMismatchMutation.mutate(dataType)

    // Then
    assert(result == "<<not a boolean>>")
  }

  @Test
  fun `produces invalid value for DateDataType`() {
    // Given
    val dataType = DateDataType.create("date").assertSuccess()

    // When
    val result = TypeMismatchMutation.mutate(dataType)

    // Then
    assert(result == "<<not a string/date>>")
  }

  @Test
  fun `produces invalid value for DateTimeDataType`() {
    // Given
    val dataType = DateTimeDataType.create("dateTime").assertSuccess()

    // When
    val result = TypeMismatchMutation.mutate(dataType)

    // Then
    assert(result == "<<not a string/date-time>>")
  }

  @Test
  fun `produces invalid value for UuidDataType`() {
    // Given
    val dataType = UuidDataType.create("uuid", false, emptyList()).assertSuccess()

    // When
    val result = TypeMismatchMutation.mutate(dataType)

    // Then
    assert(result == "<<not a string/uuid>>")
  }

  @Test
  fun `produces invalid value for EmailDataType`() {
    // Given
    val dataType = EmailDataType.create("email").assertSuccess()

    // When
    val result = TypeMismatchMutation.mutate(dataType)

    // Then
    assert(result == "<<not a string/email>>")
  }

  @Test
  fun `produces invalid value for Base64DataType`() {
    // Given
    val dataType = Base64DataType.create("base64").assertSuccess()

    // When
    val result = TypeMismatchMutation.mutate(dataType)

    // Then
    assert(result == "<<not a string/byte>>")
  }

  @Test
  fun `produces invalid value for ObjectDataType`() {
    // Given
    val dataType = objectDataType(properties = mapOf("name" to stringDataType()))

    // When
    val result = TypeMismatchMutation.mutate(dataType)

    // Then
    assert(result == "<<not a object>>")
  }

  @Test
  fun `produces invalid value for ArrayDataType with non-string items`() {
    // Given
    val dataType = arrayDataType(itemDataType = integerDataType())

    // When
    val result = TypeMismatchMutation.mutate(dataType)

    // Then
    assert(result == "<<not a array>>")
  }

  @Test
  fun `returns null for ArrayDataType with string items`() {
    // Given
    val dataType = arrayDataType(itemDataType = stringDataType())

    // When
    val result = TypeMismatchMutation.mutate(dataType)

    // Then
    assert(result == null)
  }

  @Test
  fun `produces invalid value for OneOfDataType`() {
    // Given
    val dataType = OneOfDataType.create(
      "oneOf",
      listOf(integerDataType(), stringDataType())
    ).assertSuccess()

    // When
    val result = TypeMismatchMutation.mutate(dataType)

    // Then
    assert(result == "<<not a oneOf>>")
  }

  @Test
  fun `produces invalid value for AnyOfDataType`() {
    // Given
    val dataType = AnyOfDataType.create(
      "anyOf",
      listOf(integerDataType(), stringDataType())
    ).assertSuccess()

    // When
    val result = TypeMismatchMutation.mutate(dataType)

    // Then
    assert(result == "<<not a anyOf>>")
  }

  @Test
  fun `produces invalid value for AllOfDataType`() {
    // Given
    val dataType = AllOfDataType.create(
      "allOf",
      listOf(
        objectDataType(properties = mapOf("a" to stringDataType())),
        objectDataType(properties = mapOf("b" to integerDataType()))
      )
    ).assertSuccess()

    // When
    val result = TypeMismatchMutation.mutate(dataType)

    // Then
    assert(result == "<<not a allOf>>")
  }

  @Test
  fun `returns null for StringDataType`() {
    assert(TypeMismatchMutation.mutate(stringDataType()) == null)
  }

  @Test
  fun `returns null for BinaryDataType`() {
    assert(TypeMismatchMutation.mutate(BinaryDataType.create("binary").assertSuccess()) == null)
  }

  @Test
  fun `returns null for AnyDataType`() {
    assert(TypeMismatchMutation.mutate(AnyDataType) == null)
  }
}
