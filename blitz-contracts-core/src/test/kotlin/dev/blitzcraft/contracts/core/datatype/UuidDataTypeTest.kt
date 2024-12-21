package dev.blitzcraft.contracts.core.datatype

import org.junit.jupiter.api.Test
import java.util.UUID

class UuidDataTypeTest {

  @Test
  fun `validates a value of type string representing UUID`() {
    // given
    val uuidDataType = UuidDataType()

    // when
    val result = uuidDataType.validate(UUID.randomUUID().toString())

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate string value which does not represent a UUID`() {
    // given
    val uuidDataType = UuidDataType()

    // when
    val result = uuidDataType.validate("john doe")

    // then
    assert(result.isSuccess().not())
  }
  @Test
  fun `validates null value if it is nullable`() {
    // given
    val uuidDataType = UuidDataType(isNullable = true)

    // when
    val result = uuidDataType.validate(null)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `does not validate null value if it is not nullable`() {
    // given
    val uuidDataType = UuidDataType(isNullable = false)

    // when
    val result = uuidDataType.validate(null)

    // then
    assert(result.isSuccess().not())
  }

  @Test
  fun `should generate a string representing a uuid`() {
    // given
    val uuidDataType = UuidDataType()

    // when
    val randomUuid = uuidDataType.randomValue()

    // then
    assert(uuidDataType.validate(randomUuid).isSuccess())
  }
}