package tech.sabai.contracteer.core.datatype

import org.junit.jupiter.api.Test
import tech.sabai.contracteer.core.TestFixture.objectDataType
import tech.sabai.contracteer.core.TestFixture.stringDataType

class ProxyDataTypeTest {

  @Test
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
    @Suppress("UNCHECKED_CAST")
    val value = person.randomValue() as Map<String, Any?>

    // then — first level has a nested person
    assert(value.containsKey("name"))
    assert(value["friend"] is Map<*, *>)

    // second level stops at the cycle
    @Suppress("UNCHECKED_CAST")
    val nested = value["friend"] as Map<String, Any?>
    assert(nested.containsKey("name"))
    assert(nested["friend"] == null)
  }

  @Test
  fun `randomValue produces finite value for mutual cycle`() {
    // given
    val personProxy = ProxyDataType("Person")
    val addressProxy = ProxyDataType("Address")

    val address = objectDataType(
      name = "Address",
      properties = mapOf(
        "street" to stringDataType(),
        "resident" to personProxy
      )
    )
    addressProxy.delegate = address

    val person = objectDataType(
      name = "Person",
      properties = mapOf(
        "name" to stringDataType(),
        "address" to addressProxy
      )
    )
    personProxy.delegate = person

    // when
    @Suppress("UNCHECKED_CAST")
    val value = person.randomValue() as Map<String, Any?>

    // then — person → address → person (cycle stops here)
    assert(value.containsKey("name"))
    @Suppress("UNCHECKED_CAST")
    val addressValue = value["address"] as Map<String, Any?>
    assert(addressValue.containsKey("street"))
    @Suppress("UNCHECKED_CAST")
    val residentValue = addressValue["resident"] as Map<String, Any?>
    assert(residentValue.containsKey("name"))
    assert(residentValue["address"] == null)
  }
}