package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.dsl.arrayType
import tech.sabai.contracteer.core.dsl.objectType
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
  fun `randomValue stays bounded under mutual recursion across many types with sibling arrays`() {
    // given — 4-type cycle A -> B -> C -> D -> A, each holding an array of 10 items pointing to the next.
    // Without a generation budget this produces ~20_000 nodes (10^4 leaf objects plus containers);
    // the proxy guard alone does not bound this because each proxy only blocks its own re-entry.
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
    val value = a.randomValue()

    // then
    assert(countNodes(value) <= 10_000) { "expected bounded tree, got ${countNodes(value)} nodes" }
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
}

@Suppress("UNCHECKED_CAST")
private fun Any?.asMap() =
  this as Map<String, Any?>

private fun countNodes(value: Any?): Int = when (value) {
  is Map<*, *> -> 1 + value.values.sumOf { countNodes(it) }
  is List<*>   -> 1 + value.sumOf { countNodes(it) }
  else         -> 1
}
