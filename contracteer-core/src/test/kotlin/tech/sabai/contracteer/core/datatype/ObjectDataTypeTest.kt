package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.assertSuccess
import tech.sabai.contracteer.core.dsl.arrayType
import tech.sabai.contracteer.core.dsl.booleanType
import tech.sabai.contracteer.core.dsl.integerType
import tech.sabai.contracteer.core.dsl.objectType
import tech.sabai.contracteer.core.dsl.stringType

class ObjectDataTypeTest {

  @Test
  fun `creation fails when a required property is not defined as a property`() {
    // when
    val result = ObjectDataType.create(name = "cat",
                                       properties = mapOf("hunts" to booleanType(),
                                                          "age" to integerType()),
                                       allowAdditionalProperties = false,
                                       isNullable = false,
                                       requiredProperties = setOf("hunts", "age", "type"))
    // then
    assert(result.isFailure())
    assert(result.errors().first().contains("'type'"))
  }

  @Test
  fun `validation succeeds for a null value when nullable`() {
    // given
    val objectDataType = objectType(isNullable = true) { properties { "prop" to integerType() } }

    // when
    val result = objectDataType.validate(null)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validation fails for a null value when not nullable`() {
    // given
    val objectDataType = objectType(isNullable = false) { properties { "prop" to integerType() } }

    // when
    val result = objectDataType.validate(null)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `validation fails when a value is not of type Map`() {
    // given
    val objectDataType = objectType { properties { "prop" to integerType() } }

    // when
    val result = objectDataType.validate(123)

    // then
    assert(result.isFailure())
  }

  @Test
  fun `validation succeeds when a value is of type Map`() {
    // given
    val objectDataType = objectType { properties { "prop" to integerType() } }

    // when
    val result = objectDataType.validate(mapOf("prop" to 1))

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validation succeeds when a non required property is not present`() {
    // given
    val objectDataType = objectType {
      properties {
        "prop" to integerType()
        "prop2" to integerType()
      }
      required("prop")
    }
    // when
    val result = objectDataType.validate(mapOf("prop" to 1))

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validation succeeds when a non required and non nullable property is not present`() {
    // given
    val objectDataType = objectType {
      properties {
        "prop" to integerType()
        "prop2" to integerType(isNullable = false)
      }
      required("prop")
    }
    // when
    val result = objectDataType.validate(mapOf("prop" to 1))

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `validation fails when a property is not of the expected type`() {
    // given
    val objectDataType = objectType { properties { "prop" to integerType() } }

    // when
    val result = objectDataType.validate(mapOf("prop" to true))

    // then
    assert(result.isFailure())
  }

  @Test
  fun `validation fails when a non nullable property is null`() {
    // given
    val objectDataType = objectType {
      properties {
        "prop" to integerType(isNullable = false)
        "prop2" to booleanType()
      }
    }

    // when
    val result = objectDataType.validate(mapOf(
      "prop" to null,
      "prop2" to true))

    // then
    assert(result.isFailure())
  }

  @Test
  fun `validation fails when a required property is missing`() {
    // given
    val objectDataType = objectType {
      properties {
        "prop" to integerType()
        "prop2" to booleanType()
      }
      required("prop")
    }

    // when
    val result = objectDataType.validate(mapOf("prop2" to true))

    // then
    assert(result.isFailure())
    assert(result.errors().size == 1)
    assert(listOf("is required", "prop").all { result.errors().first().contains(it) })
  }

  @Nested
  inner class WithAdditionalProperties {

    @Test
    fun `validation fails when extra properties are provided and additionalProperties is disabled`() {
      // given
      val objectDataType =
        objectType(allowAdditionalProperties = false) { properties { "prop" to integerType() } }

      // when
      val result = objectDataType.validate(mapOf("prop" to 1, "prop2" to 2, "prop3" to 3))

      // then
      assert(result.isFailure())
    }

    @Test
    fun `validation fails when extra properties are not of the expected type`() {
      // given
      val objectDataType = objectType(
        allowAdditionalProperties = true,
        additionalPropertiesDataType = stringType()
      ) { properties { "prop" to integerType() } }

      // when
      val result = objectDataType.validate(mapOf("prop" to 1, "prop2" to true, "prop3" to 3.5))

      // then
      assert(result.isFailure())
    }

    @Test
    fun `validation succeeds when extra properties datatype is not specified`() {
      // given
      val objectDataType = objectType(allowAdditionalProperties = true) {
        properties { "prop" to integerType() }
      }

      // when
      val result = objectDataType.validate(mapOf("prop" to 1, "prop2" to true, "prop3" to 3.5))

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `validation succeeds when allow additional properties is true but there is no extra properties`() {
      // given
      val objectDataType = objectType(allowAdditionalProperties = true) {
        properties { "prop" to integerType() }
      }

      // when
      val result = objectDataType.validate(mapOf("prop" to 1))

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `validation succeeds when allow additional properties datatype is specified but there is no extra properties`() {
      // given
      val objectDataType = objectType(
        allowAdditionalProperties = true,
        additionalPropertiesDataType = stringType()
      ) { properties { "prop" to integerType() } }

      // when
      val result = objectDataType.validate(mapOf("prop" to 1))

      // then
      assert(result.isSuccess())
    }
  }


  @Test
  fun `asRequestType removes readOnly properties and adjusts requiredProperties`() {
    // given
    val dataType = objectType(readOnlyProperties = setOf("id")) {
      properties {
        "id" to integerType()
        "name" to stringType()
        "password" to stringType()
      }
      required("id", "name", "password")
    }

    // when
    val requestType = dataType.asRequestType() as ObjectDataType

    // then
    assert(!requestType.properties.containsKey("id"))
    assert(requestType.properties.containsKey("name"))
    assert(requestType.properties.containsKey("password"))
    assert(!requestType.requiredProperties.contains("id"))
    assert(requestType.requiredProperties.contains("name"))
  }

  @Test
  fun `asResponseType removes writeOnly properties and adjusts requiredProperties`() {
    // given
    val dataType = objectType(writeOnlyProperties = setOf("password")) {
      properties {
        "id" to integerType()
        "name" to stringType()
        "password" to stringType()
      }
      required("id", "name", "password")
    }

    // when
    val responseType = dataType.asResponseType() as ObjectDataType

    // then
    assert(responseType.properties.containsKey("id"))
    assert(responseType.properties.containsKey("name"))
    assert(!responseType.properties.containsKey("password"))
    assert(!responseType.requiredProperties.contains("password"))
    assert(responseType.requiredProperties.contains("id"))
  }

  @Test
  fun `asRequestType and asResponseType return this when no readOnly or writeOnly properties`() {
    // given
    val dataType = objectType { properties { "name" to stringType() } }

    // then
    assert(dataType.asRequestType() === dataType)
    assert(dataType.asResponseType() === dataType)
  }

  @Test
  fun `request variant randomValue excludes readOnly and response variant excludes writeOnly`() {
    // given
    val dataType = objectType(
      readOnlyProperties = setOf("id"),
      writeOnlyProperties = setOf("password")
    ) {
      properties {
        "id" to integerType()
        "name" to stringType()
        "password" to stringType()
      }
    }

    // when
    val requestValue = dataType.asRequestType().randomValue()!!
    val responseValue = dataType.asResponseType().randomValue()!!

    // then
    assert(!requestValue.containsKey("id") && requestValue.containsKey("name") && requestValue.containsKey("password"))
    assert(responseValue.containsKey("id") && responseValue.containsKey("name") && !responseValue.containsKey("password"))
  }

  @Test
  fun `request variant validation succeeds without readOnly required field`() {
    // given
    val requestType = objectType(readOnlyProperties = setOf("id")) {
      properties {
        "id" to integerType()
        "name" to stringType()
      }
      required("id", "name")
    }.asRequestType()

    // when
    val result = requestType.validate(mapOf("name" to "Athos"))

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `response variant validation succeeds without writeOnly required field`() {
    // given
    val responseType = objectType(writeOnlyProperties = setOf("password")) {
      properties {
        "password" to stringType()
        "name" to stringType()
      }
      required("password", "name")
    }.asResponseType()

    // when
    val result = responseType.validate(mapOf("name" to "Athos"))

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `asRequestType transforms nested objects recursively`() {
    // given
    val address = objectType(name = "address", readOnlyProperties = setOf("id")) {
      properties {
        "id" to integerType()
        "street" to stringType()
      }
    }
    val user = objectType {
      properties {
        "address" to address
        "name" to stringType()
      }
    }

    // when
    val requestType = user.asRequestType() as ObjectDataType
    val addressRequestType = requestType.properties["address"] as ObjectDataType

    // then
    assert(!addressRequestType.properties.containsKey("id"))
    assert(addressRequestType.properties.containsKey("street"))
  }

  @Test
  fun `asRequestType transforms objects nested inside arrays`() {
    // given
    val item = objectType(name = "item", readOnlyProperties = setOf("id")) {
      properties {
        "id" to integerType()
        "name" to stringType()
      }
    }
    val parent = objectType {
      properties {
        "items" to arrayType(item)
        "label" to stringType()
      }
    }

    // when
    val requestType = parent.asRequestType() as ObjectDataType
    val arrayType = requestType.properties["items"] as ArrayDataType
    val itemRequestType = arrayType.itemDataType as ObjectDataType

    // then
    assert(!itemRequestType.properties.containsKey("id"))
    assert(itemRequestType.properties.containsKey("name"))
  }

  @Nested
  inner class WithEnum {

    @Test
    fun `creation fails when enum contains a value that does not match any provided sub datatype`() {
      // when
      val result = ObjectDataType.create(name = "object",
                                         properties = mapOf("prop" to integerType(), "prop2" to integerType()),
                                         requiredProperties = setOf("prop2"),
                                         allowAdditionalProperties = true,
                                         isNullable = false,
                                         enum = listOf(mapOf("prop" to 1, "prop2" to "2"), mapOf("prop" to 2)))

      // then
      assert(result.isFailure())
    }

    @Test
    fun `validation succeeds when the value is included in the enum`() {
      // given
      val objectDataType = objectType(enum = listOf(mapOf("prop" to 1), mapOf("prop" to 2))) {
        properties { "prop" to integerType() }
      }

      // when
      val result = objectDataType.validate(mapOf("prop" to 1))

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `validation fails when the value is not included in the enum`() {
      // given
      val objectDataType = objectType(enum = listOf(mapOf("prop" to 1), mapOf("prop" to 2))) {
        properties { "prop" to integerType() }
      }

      // when
      val result = objectDataType.validate(mapOf("john" to 5))

      // then
      assert(result.isFailure())
    }

    @Test
    fun `generates valid random value that matches one of the enumerated values`() {
      // given
      val enum = listOf(mapOf("prop" to "value1"), mapOf("prop" to "value2"))
      val objectDataType = objectType(enum = enum) { properties { "prop" to stringType() } }

      // when
      val result = objectDataType.randomValue()!!

      // then
      assert(enum.contains(result))
    }
  }

  @Nested
  inner class WithMinProperties {

    @Test
    fun `creation fails when minProperties is negative`() {
      // when
      val result = ObjectDataType.create(
        name = "object",
        properties = mapOf("a" to stringType()),
        allowAdditionalProperties = true,
        isNullable = false,
        minProperties = -1
      )

      // then
      assert(result.isFailure())
    }

    @Test
    fun `creation fails when minProperties exceeds declared properties and no additionalProperties schema`() {
      // when
      val result = ObjectDataType.create(
        name = "object",
        properties = mapOf("a" to stringType(), "b" to stringType()),
        allowAdditionalProperties = true,
        isNullable = false,
        minProperties = 3
      )

      // then
      assert(result.isFailure())
    }

    @Test
    fun `creation succeeds when minProperties exceeds declared properties but additionalProperties schema is available`() {
      // when
      val result = ObjectDataType.create(
        name = "Tags",
        properties = emptyMap(),
        allowAdditionalProperties = true,
        additionalPropertiesDataType = stringType(),
        isNullable = false,
        minProperties = 1
      )

      // then
      val dataType = result.assertSuccess()
      val generated = dataType.randomValue()!!
      assert(generated.isNotEmpty())
      assert(generated.values.all { it is String })
    }

    @Test
    fun `validation fails when object has fewer properties than minProperties`() {
      // given
      val objectDataType = objectType(minProperties = 2) {
        properties {
          "a" to stringType()
          "b" to stringType()
        }
      }

      // when
      val result = objectDataType.validate(mapOf("a" to "hello"))

      // then
      assert(result.isFailure())
    }

    @Test
    fun `validation succeeds when object has exactly minProperties`() {
      // given
      val objectDataType = objectType(minProperties = 2) {
        properties {
          "a" to stringType()
          "b" to stringType()
        }
      }

      // when
      val result = objectDataType.validate(mapOf("a" to "hello", "b" to "world"))

      // then
      assert(result.isSuccess())
    }
  }

  @Nested
  inner class WithMaxProperties {

    @Test
    fun `creation fails when maxProperties is negative`() {
      // when
      val result = ObjectDataType.create(
        name = "object",
        properties = mapOf("a" to stringType()),
        allowAdditionalProperties = true,
        isNullable = false,
        maxProperties = -1
      )

      // then
      assert(result.isFailure())
    }

    @Test
    fun `creation fails when maxProperties is less than required properties count`() {
      // when
      val result = ObjectDataType.create(
        name = "object",
        properties = mapOf("a" to stringType(), "b" to stringType(), "c" to stringType()),
        requiredProperties = setOf("a", "b", "c"),
        allowAdditionalProperties = true,
        isNullable = false,
        maxProperties = 2
      )

      // then
      assert(result.isFailure())
    }

    @Test
    fun `validation fails when object has more properties than maxProperties`() {
      // given
      val objectDataType = objectType(allowAdditionalProperties = true, maxProperties = 1) {
        properties {
          "a" to stringType()
          "b" to stringType()
        }
      }

      // when
      val result = objectDataType.validate(mapOf("a" to "hello", "b" to "world"))

      // then
      assert(result.isFailure())
    }

    @Test
    fun `validation succeeds when object has exactly maxProperties`() {
      // given
      val objectDataType = objectType(maxProperties = 2) {
        properties {
          "a" to stringType()
          "b" to stringType()
        }
      }

      // when
      val result = objectDataType.validate(mapOf("a" to "hello", "b" to "world"))

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `generates object with at most maxProperties including required`() {
      // given
      val objectDataType = objectType(maxProperties = 2) {
        properties {
          "a" to stringType()
          "b" to stringType()
          "c" to stringType()
        }
        required("a")
      }

      // when
      val result = objectDataType.randomValue()!!

      // then
      assert(result.size <= 2)
      assert(result.containsKey("a"))
    }
  }

  @Nested
  inner class WithMinAndMaxProperties {

    @Test
    fun `creation fails when minProperties is combined with readOnly properties`() {
      // when
      val result = ObjectDataType.create(
        name = "object",
        properties = mapOf("a" to stringType(), "b" to stringType()),
        readOnlyProperties = setOf("a"),
        allowAdditionalProperties = true,
        isNullable = false,
        minProperties = 1
      )

      // then
      assert(result.isFailure())
    }

    @Test
    fun `creation fails when maxProperties is combined with writeOnly properties`() {
      // when
      val result = ObjectDataType.create(
        name = "object",
        properties = mapOf("a" to stringType(), "b" to stringType()),
        writeOnlyProperties = setOf("a"),
        allowAdditionalProperties = true,
        isNullable = false,
        maxProperties = 2
      )

      // then
      assert(result.isFailure())
    }

    @Test
    fun `creation fails when minProperties is greater than maxProperties`() {
      // when
      val result = ObjectDataType.create(
        name = "object",
        properties = mapOf("a" to stringType(), "b" to stringType(), "c" to stringType()),
        allowAdditionalProperties = true,
        isNullable = false,
        minProperties = 3,
        maxProperties = 1
      )

      // then
      assert(result.isFailure())
    }
  }
}
