package tech.sabai.contracteer.verifier

import tech.sabai.contracteer.core.datatype.AnyDataType
import tech.sabai.contracteer.core.dsl.allOfType
import tech.sabai.contracteer.core.dsl.anyOfType
import tech.sabai.contracteer.core.dsl.arrayType
import tech.sabai.contracteer.core.dsl.base64Type
import tech.sabai.contracteer.core.dsl.binaryType
import tech.sabai.contracteer.core.dsl.booleanType
import tech.sabai.contracteer.core.dsl.dateTimeType
import tech.sabai.contracteer.core.dsl.dateType
import tech.sabai.contracteer.core.dsl.emailType
import tech.sabai.contracteer.core.dsl.integerType
import tech.sabai.contracteer.core.dsl.numberType
import tech.sabai.contracteer.core.dsl.objectType
import tech.sabai.contracteer.core.dsl.oneOfType
import tech.sabai.contracteer.core.dsl.stringType
import tech.sabai.contracteer.core.dsl.uuidType
import kotlin.test.Test

class TypeMismatchMutationTest {

  @Test
  fun `produces invalid value for IntegerDataType`() {
    assert(TypeMismatchMutation.mutate(integerType()) == "<<not a integer>>")
  }

  @Test
  fun `produces invalid value for NumberDataType`() {
    assert(TypeMismatchMutation.mutate(numberType()) == "<<not a number>>")
  }

  @Test
  fun `produces invalid value for BooleanDataType`() {
    assert(TypeMismatchMutation.mutate(booleanType()) == "<<not a boolean>>")
  }

  @Test
  fun `produces invalid value for DateDataType`() {
    assert(TypeMismatchMutation.mutate(dateType()) == "<<not a string/date>>")
  }

  @Test
  fun `produces invalid value for DateTimeDataType`() {
    assert(TypeMismatchMutation.mutate(dateTimeType()) == "<<not a string/date-time>>")
  }

  @Test
  fun `produces invalid value for UuidDataType`() {
    assert(TypeMismatchMutation.mutate(uuidType()) == "<<not a string/uuid>>")
  }

  @Test
  fun `produces invalid value for EmailDataType`() {
    assert(TypeMismatchMutation.mutate(emailType()) == "<<not a string/email>>")
  }

  @Test
  fun `produces invalid value for Base64DataType`() {
    assert(TypeMismatchMutation.mutate(base64Type()) == "<<not a string/byte>>")
  }

  @Test
  fun `produces invalid value for ObjectDataType`() {
    // Given
    val dataType = objectType { properties { "name" to stringType() } }

    // When
    val result = TypeMismatchMutation.mutate(dataType)

    // Then
    assert(result == "<<not a object>>")
  }

  @Test
  fun `produces invalid value for ArrayDataType with non-string items`() {
    assert(TypeMismatchMutation.mutate(arrayType(items = integerType())) == "<<not a array>>")
  }

  @Test
  fun `returns null for ArrayDataType with string items`() {
    // Given
    val dataType = arrayType(items = stringType())

    // When
    val result = TypeMismatchMutation.mutate(dataType)

    // Then
    assert(result == null)
  }

  @Test
  fun `produces invalid value for OneOfDataType`() {
    // Given
    val dataType = oneOfType {
      subType(integerType())
      subType(stringType())
    }

    // When
    val result = TypeMismatchMutation.mutate(dataType)

    // Then
    assert(result == "<<not a oneOf>>")
  }

  @Test
  fun `produces invalid value for AnyOfDataType`() {
    // Given
    val dataType = anyOfType {
      subType(integerType())
      subType(stringType())
    }

    // When
    val result = TypeMismatchMutation.mutate(dataType)

    // Then
    assert(result == "<<not a anyOf>>")
  }

  @Test
  fun `produces invalid value for AllOfDataType`() {
    // Given
    val dataType = allOfType {
      subType(objectType { properties { "a" to stringType() } })
      subType(objectType { properties { "b" to integerType() } })
    }

    // When
    val result = TypeMismatchMutation.mutate(dataType)

    // Then
    assert(result == "<<not a allOf>>")
  }

  @Test
  fun `returns null for StringDataType`() {
    assert(TypeMismatchMutation.mutate(stringType()) == null)
  }

  @Test
  fun `returns null for BinaryDataType`() {
    assert(TypeMismatchMutation.mutate(binaryType()) == null)
  }

  @Test
  fun `returns null for AnyDataType`() {
    assert(TypeMismatchMutation.mutate(AnyDataType) == null)
  }
}
