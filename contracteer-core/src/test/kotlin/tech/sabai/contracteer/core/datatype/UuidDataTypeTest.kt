package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.DataTypeFixture.uuidDataType
import java.util.*

class UuidDataTypeTest {

  @Test
  fun `validates a value of type string representing UUID`() {
    // given
    val uuidDataType = uuidDataType()

    // when
    val result = uuidDataType.validate(UUID.randomUUID().toString())

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate string value which does not represent a UUID`() {
    // given
    val uuidDataType = uuidDataType()

    // when
    val result = uuidDataType.validate("john doe")

    // then
    assert(result.isFailure())
  }
  @Test
  fun `validates null value if it is nullable`() {
    // given
    val uuidDataType = uuidDataType(isNullable = true)

    // when
    val result = uuidDataType.validate(null)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate null value if it is not nullable`() {
    // given
    val uuidDataType = uuidDataType(isNullable = false)

    // when
    val result = uuidDataType.validate(null)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `should generate a string representing a uuid`() {
    // given
    val uuidDataType = uuidDataType()

    // when
    val randomUuid = uuidDataType.randomValue()

    // then
    assert(uuidDataType.validate(randomUuid).isSuccess())
  }

  @Test
  fun `validates a string representing a uuid with enum values`() {
    // given
    val uuidDataType = uuidDataType(enum = listOf("15fec06f-7494-4dec-89a5-b12bf45198c2", "b4f323c5-20a9-4f88-b438-8c34865e5416"))

    // when
    val result = uuidDataType.validate("b4f323c5-20a9-4f88-b438-8c34865e5416")

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate a string with enum values`() {
    // given
    val uuidDataType = uuidDataType(enum = listOf("15fec06f-7494-4dec-89a5-b12bf45198c2", "b4f323c5-20a9-4f88-b438-8c34865e5416"))

    // when
    val result = uuidDataType.validate("e74dde48-aa4a-4403-884b-cac181b114e4")

    // then
    assert(result.isFailure())
  }

  @Test
  fun `generates random value with enum values`() {
    // given
    val enum = listOf("15fec06f-7494-4dec-89a5-b12bf45198c2", "b4f323c5-20a9-4f88-b438-8c34865e5416")
    val uuidDataType = uuidDataType(enum = enum)

    // when
    val result = uuidDataType.randomValue()

    // then
    assert(enum.contains(result))
  }
}