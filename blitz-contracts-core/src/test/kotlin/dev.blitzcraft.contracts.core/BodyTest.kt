package dev.blitzcraft.contracts.core

import dev.blitzcraft.contracts.core.datatype.ArrayDataType
import dev.blitzcraft.contracts.core.datatype.IntegerDataType
import dev.blitzcraft.contracts.core.datatype.ObjectDataType
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.Test
import kotlin.test.assertFailsWith

class BodyTest {

  @Test
  fun `fails when DataType is not of type ObjectDataType or ArrayDataType for Json Content-Type`() {
    // expect
    assertFailsWith(IllegalArgumentException::class) { Body("application/json", IntegerDataType()) }
  }

  @Test
  fun `fails when Example value is not of type Map or Array for Json Content-Type`() {
    // expect
    assertFailsWith(IllegalArgumentException::class) {
      Body("application/json", ObjectDataType(mapOf("id" to Property(IntegerDataType()))), Example(42))
    }
  }

  @Test
  fun `accepts ObjectDataType as Body content`() {
    // expect
    assertDoesNotThrow {
      Body("application/json",
           ObjectDataType(properties = mapOf("id" to Property(IntegerDataType()))))
    }
  }

  @Test
  fun `accepts ArrayDataType as Body content`() {
    // expect
    assertDoesNotThrow { Body("application/json", ArrayDataType(IntegerDataType())) }
  }

  @Test
  fun `accepts Map as Body content Example`() {
    // expect
    assertDoesNotThrow {
      Body("application/json",
           ObjectDataType(properties = mapOf("id" to Property(IntegerDataType()))),
           Example(mapOf("id" to 42)))
    }
  }

  @Test
  fun `accepts Array as Body content Example`() {
    // expect
    assertDoesNotThrow {
      Body("application/json",
           ObjectDataType(properties = mapOf("id" to Property(IntegerDataType()))),
           Example(arrayOf(1, 2, 3)))
    }
  }
}
