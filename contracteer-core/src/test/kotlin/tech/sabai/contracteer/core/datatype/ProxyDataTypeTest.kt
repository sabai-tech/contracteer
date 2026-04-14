package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.TestFixture.arrayDataType
import tech.sabai.contracteer.core.TestFixture.objectDataType
import tech.sabai.contracteer.core.TestFixture.stringDataType

class ProxyDataTypeTest {

  @Test
  @Suppress("UNCHECKED_CAST")
  fun `randomValue produces finite value for self-referencing object`() {
    // given
    val proxy = ProxyDataType("Person")
    val person = objectDataType(
      name = "Person",
      properties = mapOf(
        "name" to stringDataType(),
        "friend" to proxy
      )
    )
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

    val person = objectDataType(
      name = "Person",
      properties = mapOf(
        "name" to stringDataType(),
        "address" to addressProxy
      )
    )
    val address = objectDataType(
      name = "Address",
      properties = mapOf(
        "street" to stringDataType(),
        "resident" to personProxy
      )
    )
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
    val conditionArray = arrayDataType(itemDataType = proxy)
    val condition = objectDataType(
      name = "Condition",
      properties = mapOf(
        "type" to stringDataType(),
        "sub-conditions" to conditionArray
      ),
      requiredProperties = setOf("type", "sub-conditions")
    )
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
  fun `randomValue keeps null for nullable recursive property at cycle boundary`() {
    // given
    val proxy = ProxyDataType("Node")
    val nullableNode = objectDataType(
      name = "Node",
      properties = mapOf(
        "value" to stringDataType(),
        "next" to proxy
      ),
      requiredProperties = setOf("value", "next"),
      isNullable = true
    )
    proxy.delegate = nullableNode

    val node = objectDataType(
      name = "Node",
      properties = mapOf(
        "value" to stringDataType(),
        "next" to proxy
      ),
      requiredProperties = setOf("value", "next")
    )

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
