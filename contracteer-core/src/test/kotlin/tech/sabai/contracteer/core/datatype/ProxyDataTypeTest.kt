package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.datatype.GenerationOutcome.Boundary
import tech.sabai.contracteer.core.datatype.GenerationOutcome.Value
import tech.sabai.contracteer.core.dsl.allOfType
import tech.sabai.contracteer.core.dsl.anyOfType
import tech.sabai.contracteer.core.dsl.arrayType
import tech.sabai.contracteer.core.dsl.objectType
import tech.sabai.contracteer.core.dsl.oneOfType
import tech.sabai.contracteer.core.dsl.stringType

class ProxyDataTypeTest {

  @Test
  @Suppress("UNCHECKED_CAST")
  fun `randomValue produces finite value for self-referencing object`() {
    // given
    val proxy = ProxyDataType("Person")
    val person = objectType(name = "Person") {
      properties {
        "name" to stringType()
        "friend" to proxy
      }
    }
    proxy.delegate = person

    // when
    val value = person.randomValue().asMap()

    // then — first level has a nested person
    assert(value.containsKey("name"))
    assert(value["friend"] is Map<*, *>)

    // second level omits the optional non-nullable cycle property
    val nested = value["friend"].asMap()
    assert(nested.containsKey("name"))
    assert(!nested.containsKey("friend"))
  }

  @Test
  fun `randomValue produces finite value for mutual cycle`() {
    // given
    val personProxy = ProxyDataType("Person")
    val addressProxy = ProxyDataType("Address")

    val person = objectType(name = "Person") {
      properties {
        "name" to stringType()
        "address" to addressProxy
      }
    }
    val address = objectType(name = "Address") {
      properties {
        "street" to stringType()
        "resident" to personProxy
      }
    }
    addressProxy.delegate = address
    personProxy.delegate = person

    // when
    val value = person.randomValue().asMap()

    // then — person → address → person (cycle stops, optional property omitted)
    assert(value.containsKey("name"))
    assert(value["address"].asMap().containsKey("street"))

    val residentValue = value["address"].asMap()["resident"].asMap()
    assert(residentValue.containsKey("name"))
    assert(!residentValue.containsKey("address"))
  }

  @Test
  @Suppress("UNCHECKED_CAST")
  fun `randomValue generates empty array for required non-nullable recursive array property`() {
    // given — models the api2cart sub-conditions pattern
    val proxy = ProxyDataType("Condition")
    val conditionArray = arrayType(items = proxy)
    val condition = objectType(name = "Condition") {
      properties {
        "type" to stringType()
        "sub-conditions" to conditionArray
      }
      required("type", "sub-conditions")
    }
    proxy.delegate = condition

    // when
    val value = condition.randomValue().asMap()

    // then — first level has a nested array with conditions
    assert(value.containsKey("type"))
    assert(value["sub-conditions"] is List<*>)
    val subConditions = value["sub-conditions"] as List<Map<String, Any?>>
    assert(subConditions.isNotEmpty())

    // second level — required array property is empty (cycle stops)
    val nested = subConditions[0]
    assert(nested.containsKey("type"))
    assert(nested["sub-conditions"] == emptyList<Any>())
  }

  @Test
  fun `randomValue produces Boundary under mutual recursion across many types when required arrays cannot be empty`() {
    // given — 4-type cycle A -> B -> C -> D -> A, each holding a required array of 10 items pointing to the next.
    // The cycle has no escape: every step is required and non-nullable, and minItems=10 forbids an empty array.
    val proxyA = ProxyDataType("A")
    val proxyB = ProxyDataType("B")
    val proxyC = ProxyDataType("C")
    val proxyD = ProxyDataType("D")

    val a = objectType(name = "A") {
      properties { "bs" to arrayType(items = proxyB, minItems = 10, maxItems = 10) }
      required("bs")
    }
    val b = objectType(name = "B") {
      properties { "cs" to arrayType(items = proxyC, minItems = 10, maxItems = 10) }
      required("cs")
    }
    val c = objectType(name = "C") {
      properties { "ds" to arrayType(items = proxyD, minItems = 10, maxItems = 10) }
      required("ds")
    }
    val d = objectType(name = "D") {
      properties { "as_" to arrayType(items = proxyA, minItems = 10, maxItems = 10) }
      required("as_")
    }

    proxyA.delegate = a
    proxyB.delegate = b
    proxyC.delegate = c
    proxyD.delegate = d

    // when
    val result = a.randomValue(GenerationContext.default())

    // then
    assert(result is Boundary)
  }

  @Test
  fun `randomValue does not crash when required property is oneOf with a recursive variant`() {
    // given — Parent.child is oneOf[ProxyToParent, Other], required and non-nullable.
    val parentProxy = ProxyDataType("Parent")
    val other = objectType(name = "Other") {
      properties { "marker" to stringType() }
    }
    val composed = oneOfType(name = "Composed") {
      subType(parentProxy)
      subType(other)
    }
    val parent = objectType(name = "Parent") {
      properties { "child" to composed }
      required("child")
    }
    parentProxy.delegate = parent

    // when — repeat to cover both random picks at every level
    repeat(50) {
      val value = parent.randomValue().asMap()

      // then
      assert(value["child"] is Map<*, *>)
    }
  }

  @Test
  fun `randomValue does not crash when required property is anyOf with a recursive variant`() {
    // given — Parent.child is anyOf[ProxyToParent, Other], required and non-nullable.
    val parentProxy = ProxyDataType("Parent")
    val other = objectType(name = "Other") {
      properties { "marker" to stringType() }
    }
    val composed = anyOfType(name = "Composed") {
      subType(parentProxy)
      subType(other)
    }
    val parent = objectType(name = "Parent") {
      properties { "child" to composed }
      required("child")
    }
    parentProxy.delegate = parent

    // when — repeat to cover both random picks at every level
    repeat(50) {
      val value = parent.randomValue().asMap()

      // then
      assert(value["child"] is Map<*, *>)
    }
  }

  @Test
  fun `randomValue returns Boundary for required non-nullable string at cycle boundary`() {
    // given
    val proxy = ProxyDataType("Person")
    val person = objectType(name = "Person") {
      properties {
        "name" to stringType()
        "friend" to proxy
      }
      required("name", "friend")
    }
    proxy.delegate = person
    val ctx = GenerationContext.withBudget(maxDepth = 2, maxNodes = 100)

    // when
    val result = person.randomValue(ctx)

    // then
    assert(result is Boundary)
  }

  @Test
  fun `randomValue keeps null for nullable recursive property at cycle boundary`() {
    // given
    val proxy = ProxyDataType("Node")
    val nullableNode = objectType(name = "Node", isNullable = true) {
      properties {
        "value" to stringType()
        "next" to proxy
      }
      required("value", "next")
    }
    proxy.delegate = nullableNode

    val node = objectType(name = "Node") {
      properties {
        "value" to stringType()
        "next" to proxy
      }
      required("value", "next")
    }

    // when
    val value = node.randomValue().asMap()

    // then — nullable property keeps null at cycle boundary
    assert(value.containsKey("next"))
    assert(value["next"].asMap()["next"] == null)
  }

  @Test
  fun `randomValue omits optional non-nullable property when child boundaries`() {
    // given
    val proxy = ProxyDataType("Person")
    val person = objectType(name = "Person") {
      properties {
        "name" to stringType()
        "friend" to proxy
      }
      required("name")
    }
    proxy.delegate = person
    val ctx = GenerationContext.withBudget(maxDepth = 2, maxNodes = 100)

    // when
    val result = person.randomValue(ctx)

    // then
    assert(result is Value)
    val entries = (result as Value).value as Map<*, *>
    assert("name" in entries.keys)
    assert("friend" !in entries.keys)
  }

  @Test
  fun `Boundary path tracks descent through nested object properties`() {
    // given
    val proxy = ProxyDataType("Cycler")
    val cycler = objectType(name = "Cycler") {
      properties {
        "self" to proxy
      }
      required("self")
    }
    proxy.delegate = cycler

    val middle = objectType(name = "Middle") {
      properties {
        "cycler" to cycler
      }
      required("cycler")
    }
    val outer = objectType(name = "Outer") {
      properties {
        "middle" to middle
      }
      required("middle")
    }

    // when
    val result = outer.randomValue(GenerationContext.default())

    // then
    assert(result is Boundary)
    val boundary = result as Boundary
    assert(boundary.path == "middle.cycler.self.self") {
      "expected path 'middle.cycler.self.self', got '${boundary.path}'"
    }
  }

  @Test
  fun `Boundary path tracks descent through array items`() {
    // given
    val proxy = ProxyDataType("Cycler")
    val cycler = objectType(name = "Cycler") {
      properties {
        "self" to proxy
      }
      required("self")
    }
    proxy.delegate = cycler

    val outer = objectType(name = "Outer") {
      properties {
        "items" to arrayType(items = cycler, minItems = 1, maxItems = 1)
      }
      required("items")
    }

    // when
    val result = outer.randomValue(GenerationContext.default())

    // then
    assert(result is Boundary)
    val boundary = result as Boundary
    assert(boundary.path == "items[0].self.self")
  }

  @Test
  fun `Boundary path tracks descent through oneOf subtype`() {
    // given
    val proxy = ProxyDataType("Cycler")
    val cycler = objectType(name = "Cycler") {
      properties {
        "self" to proxy
      }
      required("self")
    }
    proxy.delegate = cycler

    val choice = oneOfType(name = "Choice") {
      subType(cycler)
    }

    // when
    val result = choice.randomValue(GenerationContext.default())

    // then
    assert(result is Boundary)
    val boundary = result as Boundary
    assert(boundary.path == "Cycler.self.self")
  }

  @Test
  fun `Boundary path tracks descent through allOf subtype`() {
    // given
    val proxy = ProxyDataType("Cycler")
    val cycler = objectType(name = "Cycler") {
      properties {
        "self" to proxy
      }
      required("self")
    }
    proxy.delegate = cycler

    val composed = allOfType(name = "Composed") {
      subType(cycler)
    }

    // when
    val result = composed.randomValue(GenerationContext.default())

    // then
    assert(result is Boundary)
    val boundary = result as Boundary
    assert(boundary.path == "Cycler.self.self")
  }

  @Test
  fun `randomValue keeps null for required nullable property when child boundaries`() {
    // given
    val proxy = ProxyDataType("Node")
    val node = objectType(name = "Node", isNullable = true) {
      properties {
        "value" to stringType()
        "next" to proxy
      }
      required("value", "next")
    }
    proxy.delegate = node
    val ctx = GenerationContext.withBudget(maxDepth = 2, maxNodes = 100)

    // when
    val result = node.randomValue(ctx)

    // then
    assert(result is Value)
    val entries = (result as Value).value as Map<*, *>
    assert("next" in entries.keys)
    assert(entries["next"] == null)
  }
}

@Suppress("UNCHECKED_CAST")
private fun Any?.asMap() =
  this as Map<String, Any?>
