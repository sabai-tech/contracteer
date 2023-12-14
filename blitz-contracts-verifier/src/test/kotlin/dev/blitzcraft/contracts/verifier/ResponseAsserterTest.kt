package dev.blitzcraft.contracts.verifier

import dev.blitzcraft.contracts.core.Body
import dev.blitzcraft.contracts.core.Property
import dev.blitzcraft.contracts.core.ResponseContract
import dev.blitzcraft.contracts.core.datatype.ArrayDataType
import dev.blitzcraft.contracts.core.datatype.IntegerDataType
import dev.blitzcraft.contracts.core.datatype.ObjectDataType
import dev.blitzcraft.contracts.core.datatype.StringDataType
import io.mockk.every
import io.mockk.mockk
import org.http4k.core.Response
import org.http4k.core.Status
import kotlin.test.Test
import kotlin.test.assertFails

class ResponseAsserterTest {

  @Test
  fun `Validates Response status code`() {
    // given
    val responseContract = ResponseContract(202)
    val response = mockk<Response>()
    every { response.status } returns Status.ACCEPTED
    every { response.headers } returns  emptyList()
    every { response.header("Content-Type") } returns null

    // when
    ResponseAsserter(responseContract).assert(response)

    // then no exception
  }

  @Test
  fun `Fails when Response status code is different from Contract`() {
    // given
    val responseContract = ResponseContract(202)
    val response = mockk<Response>()
    every { response.status } returns Status.CREATED
    every { response.headers } returns emptyList()
    every { response.header("Content-Type") } returns null

    // expect
    val exception = assertFails { ResponseAsserter(responseContract).assert(response) }
    assert(exception.message!!.contains(Regex("(?=.*201)(?=.*202)")))
  }

  @Test
  fun `Validates Response headers`() {
    // given
    val responseContract = ResponseContract(202, headers = mapOf("x-test" to Property(IntegerDataType())))
    val response = mockk<Response>()
    every { response.status } returns Status.ACCEPTED
    every { response.headers } returns listOf("x-test" to "42")
    every { response.header("Content-Type") } returns null

    // when
    ResponseAsserter(responseContract).assert(response)

    // then no exception
  }

  @Test
  fun `Fails for invalid Response header`() {
    // given
    val responseContract = ResponseContract(202, headers = mapOf("x-test" to Property(IntegerDataType())))
    val response = mockk<Response>()
    every { response.status } returns Status.ACCEPTED
    every { response.headers } returns listOf("x-test" to "John")    
    every { response.header("Content-Type") } returns null

    // expect
    val exception = assertFails { ResponseAsserter(responseContract).assert(response) }
    assert(exception.message!!.contains("x-test"))
  }

  @Test
  fun `Does not fails for missing optional Response header`() {
    // given
    val responseContract = ResponseContract(202, headers = mapOf("x-test" to Property(IntegerDataType())))
    val response = mockk<Response>()
    every { response.status } returns Status.ACCEPTED
    every { response.header("Content-Type") } returns null
    every { response.headers } returns  emptyList()
    // when
    ResponseAsserter(responseContract).assert(response)

    // then no exception
  }
  @Test
  fun `Fails for missing required Response header`() {
    // given
    val responseContract = ResponseContract(202, headers = mapOf("x-test" to Property(IntegerDataType(), required = true)))
    val response = mockk<Response>()
    every { response.status } returns Status.ACCEPTED
    every { response.header("Content-Type") } returns null
    every { response.headers } returns  emptyList()

    // expect
    val exception = assertFails { ResponseAsserter(responseContract).assert(response) }
    assert(exception.message!!.contains(Regex("(?=.*missing)(?=.*x-test)")))
  }

  @Test
  fun `Validates body with JSON Content-Type`() {
    // given
    val responseContract = ResponseContract(statusCode = 200,
                                            body = Body("application/json",
                                                        ObjectDataType(mapOf(
                                                          "name" to Property(StringDataType()),
                                                          "age" to Property(IntegerDataType())))))
    val response = mockk<Response>()
    every { response.status } returns Status.OK
    every { response.header("Content-Type") } returns "application/json; charset=utf-8"
    every { response.headers } returns listOf( "Content-Type" to "application/json; charset=utf-8")
    every { response.bodyString() } returns """{ "name":"John", "age": 25}"""

    // when
    ResponseAsserter(responseContract).assert(response)

    // then no exception
  }

  @Test
  fun `Validates body with JSON Content-Type and missing optional properties`() {
    // given
    val responseContract = ResponseContract(statusCode = 200,
                                            body = Body("application/json",
                                                        ObjectDataType(mapOf(
                                                          "name" to Property(StringDataType(), required = true),
                                                          "age" to Property(IntegerDataType(), required = false)))))
    val response = mockk<Response>()
    every { response.status } returns Status.OK
    every { response.header("Content-Type") } returns "application/json; charset=utf-8"
    every { response.headers } returns listOf( "Content-Type" to "application/json; charset=utf-8")
    every { response.bodyString() } returns """{ "name":"John"}"""

    // when
    ResponseAsserter(responseContract).assert(response)

    // then no exception
  }

  @Test
  fun `Fails to validate body with JSON Content-Type and missing required properties`() {
    // given
    val responseContract = ResponseContract(statusCode = 200,
                                            body = Body("application/json",
                                                        ObjectDataType(mapOf(
                                                          "name" to Property(StringDataType(), required = true),
                                                          "age" to Property(IntegerDataType(), required = false)))))
    val response = mockk<Response>()
    every { response.status } returns Status.OK    
    every { response.header("Content-Type") } returns "application/json; charset=utf-8"
    every { response.headers } returns listOf( "Content-Type" to "application/json; charset=utf-8")
    every { response.bodyString() } returns """{ "age":42}"""

    // expect
    val exception = assertFails { ResponseAsserter(responseContract).assert(response) }
    assert(exception.message!!.contains("name"))
  }

  @Test
  fun `Validates body with array as root element and with JSON Content-Type`() {
    // given
    val responseContract = ResponseContract(statusCode = 200,
                                            body = Body("application/json", ArrayDataType(IntegerDataType())))
    val response = mockk<Response>()
    every { response.status } returns Status.OK
    every { response.header("Content-Type") } returns "application/json; charset=utf-8"
    every { response.headers } returns listOf( "Content-Type" to "application/json; charset=utf-8")
    every { response.bodyString() } returns """[1,2,3]"""

    // when
    ResponseAsserter(responseContract).assert(response)

    // then no exception
  }

  @Test
  fun `Fails when Body has wrong Content-Type`() {
    // given
    val responseContract = ResponseContract(statusCode = 200,
                                            body = Body("text/plain",
                                                        ObjectDataType(mapOf("name" to Property(StringDataType())))))
    val response = mockk<Response>()
    every { response.status } returns Status.OK
    every { response.header("Content-Type") } returns "application/json; charset=utf-8"
    every { response.headers } returns listOf( "Content-Type" to "application/json; charset=utf-8")
    every { response.bodyString() } returns """{ "name":"John", "age": 42}"""

    // expect
    val exception = assertFails { ResponseAsserter(responseContract).assert(response) }
    assert(exception.message!!.contains(Regex("(?=.*text/plain)(?=.*application/json; charset=utf-8)")))
  }

  @Test
  fun `Fails when Contract defines no Content-Type but Response has Content-Type`() {
    // given
    val responseContract = ResponseContract(statusCode = 200)
    val response = mockk<Response>()
    every { response.status } returns Status.OK
    every { response.header("Content-Type") } returns "application/json; charset=utf-8"
    every { response.headers } returns listOf( "Content-Type" to "application/json; charset=utf-8")
    every { response.bodyString() } returns """{ "name":"John", "age": 42}"""

    // expect
    val exception = assertFails { ResponseAsserter(responseContract).assert(response) }
    assert(exception.message!!.contains("application/json; charset=utf-8"))
  }

  @Test
  fun `Fails when Contract defines Content-Type but Response has no Content-Type`() {
    // given
    val responseContract = ResponseContract(statusCode = 200,
                                            body = Body("application/json",
                                                        ObjectDataType(mapOf("name" to Property(StringDataType())))))
    val response = mockk<Response>()
    every { response.status } returns Status.OK
    every { response.header("Content-Type") } returns null
    every { response.headers } returns  emptyList()

    // expect
    val exception = assertFails { ResponseAsserter(responseContract).assert(response) }
    assert(exception.message!!.contains("application/json"))
  }
}