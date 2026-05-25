package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.datatype.GenerationOutcome.Boundary
import tech.sabai.contracteer.core.datatype.GenerationOutcome.Reason
import tech.sabai.contracteer.core.datatype.GenerationOutcome.Value
import tech.sabai.contracteer.core.dsl.anyOfType
import tech.sabai.contracteer.core.dsl.arrayType
import tech.sabai.contracteer.core.dsl.booleanType
import tech.sabai.contracteer.core.dsl.integerType
import tech.sabai.contracteer.core.dsl.objectType
import tech.sabai.contracteer.core.dsl.stringType
import tech.sabai.contracteer.core.normalize

class AnyOfDataTypeTest {

  private val dog = objectType(name = "dog") {
    properties {
      "barks" to booleanType()
      "age" to integerType()
      "type" to stringType()
    }
    required("type", "barks")
  }
  private val cat = objectType(name = "cat") {
    properties {
      "hunts" to booleanType()
      "age" to integerType()
      "type" to stringType()
    }
    required("type", "hunts")
  }

  private val quantity = integerType(name = "quantity")

  private val name = stringType(name = "name")

  @Test
  fun `validation fails when none of sub datatype validates the value`() {
    // given
    val anyOfDataType = anyOfType { subType(dog); subType(cat); subType(quantity); subType(name) }

    // when
    val result = anyOfDataType.validate(true)

    // then
    assert(result.isFailure())
    assert(result.errors().first().contains("dog"))
    assert(result.errors().first().contains("cat"))
    assert(result.errors().first().contains("quantity"))
    assert(result.errors().first().contains("name"))
  }

  @Test
  fun `validation succeeds when multiple sub datatypes validate the value`() {
    // given
    val anyOfDataType = anyOfType { subType(quantity); subType(name); subType(integerType()) }

    // when
    val result = anyOfDataType.validate(2)

    // then
    assert(result.isSuccess())
  }

  @Test
  fun `generates a valid random value`() {
    // given
    val anyOfDataType = anyOfType { subType(dog); subType(cat); subType(quantity); subType(name) }

    // when
    val randomValue = anyOfDataType.randomValue()

    // then
    assert(anyOfDataType.validate(randomValue).isSuccess())
  }

  @Test
  fun `randomValue picks non-cycling subtype of anyOf when one subtype cycles`() {
    // given
    val proxy = ProxyDataType("Recursive")
    val recursive = objectType(name = "Recursive") {
      properties { "self" to proxy }
      required("self")
    }
    proxy.delegate = recursive
    val other = objectType(name = "Other") {
      properties { "marker" to stringType() }
    }
    val anyOfDataType = anyOfType(name = "AnyOfTypes") {
      subType(proxy)
      subType(other)
    }

    // when
    val results = (1..50).map { anyOfDataType.randomValue(GenerationContext.default()) }

    // then
    assert(results.all { it is Value && (it.value as Map<*, *>).containsKey("marker") })
  }

  @Test
  fun `randomValue injects discriminator on the picked anyOf subtype`() {
    // given
    val wrapperProxy = ProxyDataType("Wrapper")
    val wrapper = objectType(name = "Wrapper") {
      properties {
        "type" to stringType()
        "recursive" to wrapperProxy
      }
      required("type", "recursive")
    }
    wrapperProxy.delegate = wrapper
    val other = objectType(name = "Other") {
      properties {
        "type" to stringType()
        "marker" to stringType()
      }
    }
    val anyOfDataType = anyOfType(name = "AnyOfTypes") {
      subType(wrapper)
      subType(other)
      discriminator("type") {
        mapping("wrap", "Wrapper")
        mapping("other", "Other")
      }
    }

    // when
    val results = (1..50).map { anyOfDataType.randomValue(GenerationContext.default()) }

    // then
    assert(results.all { it is Value && (it.value as Map<*, *>)["type"] == "other" })
  }

  @Test
  fun `randomValue returns Boundary when all anyOf subtypes cycle`() {
    // given
    val proxyA = ProxyDataType("A")
    val a = objectType(name = "A") {
      properties { "child" to proxyA }
      required("child")
    }
    proxyA.delegate = a
    val proxyB = ProxyDataType("B")
    val b = objectType(name = "B") {
      properties { "child" to proxyB }
      required("child")
    }
    proxyB.delegate = b
    val anyOfDataType = anyOfType(name = "AnyOfBoth") {
      subType(a)
      subType(b)
    }

    // when
    val result = anyOfDataType.randomValue(GenerationContext.default())

    // then
    assert(result is Boundary && result.reason == Reason.CYCLE)
  }

  @Test
  fun `randomValue propagates the subtype boundary reason when budget exhausts every anyOf subtype`() {
    // given
    val anyOfDataType = anyOfType(name = "AnyOfTypes") {
      subType(stringType())
      subType(integerType())
    }
    val ctx = GenerationContext.withBudget(maxDepth = 10, maxNodes = 1)

    // when
    val result = anyOfDataType.randomValue(ctx)

    // then
    assert(result is Boundary && result.reason == Reason.NODES)
  }

  @Nested
  inner class WithEnum {

    @Test
    fun `creation fails when enum contains a value that does not match any provided sub datatype`() {
      // when
      val result = AnyOfDataType.create(
        name = "anyOf",
        subTypes = listOf(dog, cat, quantity, name),
        enum = listOf("John", 42, true)
      )

      // then
      assert(result.isFailure())
    }

    @Test
    fun `validation succeeds when the value is included in the enum`() {
      // given
      val anyOfDataType = anyOfType(
        enum = listOf(mapOf("barks" to true, "age" to 3, "type" to "dog"),
                      mapOf("hunts" to true, "age" to 2, "type" to "cat"),
                      42)
      ) { subType(dog); subType(cat); subType(quantity); subType(name) }

      // when
      val result = anyOfDataType.validate(42)

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `validation fails when the value is not included in the enum`() {
      // given
      val anyOfDataType = anyOfType(
        enum = listOf(mapOf("barks" to true, "age" to 3, "type" to "dog"),
                      mapOf("hunts" to true, "age" to 2, "type" to "cat"),
                      42)
      ) { subType(dog); subType(cat); subType(quantity); subType(name) }

      // when
      val result = anyOfDataType.validate(mapOf("barks" to false, "age" to 2))

      // then
      assert(result.isFailure())
    }

    @Test
    fun `generates valid random value that matches one of the enumerated values`() {
      // given
      val enum = listOf(mapOf("barks" to true, "age" to 3, "type" to "dog"),
                        mapOf("hunts" to true, "age" to 2, "type" to "cat"),
                        42,
                        "John")
      val anyOfDataType = anyOfType(enum = enum) {
        subType(dog); subType(cat); subType(quantity); subType(name)
      }

      // when
      val result = anyOfDataType.randomValue()

      // then
      assert(enum.map { obj -> obj.normalize() }.contains(result))
    }
  }

  @Nested
  inner class WithDiscriminator {

    @Test
    fun `creation fails when discriminator mapping references unknown sub datatype`() {
      // when
      val result = AnyOfDataType.create(
        name = "anyOf",
        subTypes = listOf(dog, cat),
        discriminator = Discriminator("type",
                                      mapOf("DOG" to "lizard")
        )
      )
      // then
      assert(result.isFailure())
    }

    @Test
    fun `creation fails when discriminator is provided and not all sub datatypes are composite structured`() {
      // when
      val result = AnyOfDataType.create(
        name = "anyOf",
        subTypes = listOf(dog, cat, quantity, name),
        discriminator = Discriminator("type")
      )

      // then
      assert(result.isFailure())
    }

    @Test
    fun `creation succeeds when sub datatypes are a composite structured datatype`() {
      // when
      val result = AnyOfDataType.create(
        name = "anyOf",
        subTypes = listOf(
          dog,
          anyOfType(name = "other") {
            subType(cat)
            subType(objectType(name = "lizard") {
              properties {
                "age" to integerType()
                "type" to stringType()
              }
              required("age", "type")
            })
          }),
        discriminator = Discriminator("type")
      )

      // then
      assert(result.isSuccess())

    }

    @Test
    fun `validation succeeds with discriminator mapping`() {
      // given
      val anyOfDataType = anyOfType {
        subType(dog); subType(cat)
        discriminator("type") { mapping("DOG", "dog") }
      }

      // when
      val result = anyOfDataType.validate(mapOf("type" to "DOG", "barks" to true))

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `validation falls back to plain matching when discriminator property is absent`() {
      // given
      val anyOfDataType = anyOfType {
        subType(objectType(name = "dog") {
          properties {
            "barks" to booleanType()
            "type" to stringType()
          }
          required("barks")
        })
        subType(objectType(name = "cat") {
          properties {
            "hunts" to booleanType()
            "type" to stringType()
          }
          required("hunts")
        })
        discriminator("type")
      }

      // when
      val result = anyOfDataType.validate(mapOf("barks" to true))

      // then
      assert(result.isSuccess())
    }

    @Test
    fun `validation falls back to plain matching when discriminator property is not a string`() {
      // given
      val anyOfDataType = anyOfType {
        subType(dog)
        subType(cat)
        discriminator("type")
      }

      // when — discriminator value is not a string; fallback produces no-matching-schema error
      val result = anyOfDataType.validate(mapOf("type" to 42, "barks" to true))

      // then
      assert(result.isFailure())
      assert(result.errors().first().contains("No matching schema"))
    }

    @Test
    fun `validation fails with clear error when discriminator value does not match any mapping`() {
      // given
      val anyOfDataType = anyOfType {
        subType(dog)
        subType(cat)
        discriminator("type") { mapping("DOG", "dog") }
      }

      // when
      val result = anyOfDataType.validate(mapOf("type" to "FISH", "barks" to true))

      // then
      assert(result.isFailure())
      assert(result.errors().first().contains("No schema found for discriminator"))
    }

    @Test
    fun `generates random valid value with discriminator`() {
      // given
      val anyOfDataType = anyOfType {
        subType(dog)
        subType(cat)
        discriminator("type")
      }

      // when
      val randomValue = anyOfDataType.randomValue() as Map<*, *>

      assert(
        (dog.validate(randomValue).isSuccess() && randomValue["type"] == "dog") ||
        (cat.validate(randomValue).isSuccess() && randomValue["type"] == "cat")
      )
    }

    @Test
    fun `generates random valid value for composite structured sub datatypes`() {
      // given
      val anyOfDataType = anyOfType {
        subType(dog)
        subType(anyOfType(name = "other") {
          subType(cat)
          subType(objectType(name = "lizard") {
            properties {
              "age" to integerType()
              "type" to stringType()
            }
            required("age", "type")
          })
        })
        discriminator("type")
      }

      // when
      val randomValue = anyOfDataType.randomValue()

      // then
      assert(anyOfDataType.validate(randomValue).isSuccess())
      assert((randomValue as Map<*, *>)["type"] == "dog" || (randomValue["type"] == "other"))
    }
  }
}
