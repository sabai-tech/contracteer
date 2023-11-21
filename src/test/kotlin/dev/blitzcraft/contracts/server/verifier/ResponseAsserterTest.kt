package dev.blitzcraft.contracts.server.verifier

import dev.blitzcraft.contracts.core.Body
import dev.blitzcraft.contracts.core.Property
import dev.blitzcraft.contracts.core.ResponseContract
import dev.blitzcraft.contracts.core.datatype.ArrayDataType
import dev.blitzcraft.contracts.core.datatype.IntegerDataType
import dev.blitzcraft.contracts.core.datatype.ObjectDataType
import dev.blitzcraft.contracts.core.datatype.StringDataType
import io.mockk.every
import io.mockk.mockk
import io.restassured.http.Header
import io.restassured.http.Headers
import io.restassured.response.Response
import kotlin.test.Test
import kotlin.test.assertFails

class ResponseAsserterTest {

  @Test
  fun `Validates Response status code`() {
    // given
    val responseContract = ResponseContract(202)
    val response = mockk<Response>()
    every { response.statusCode } returns 202
    every { response.contentType } returns null

    // when
    ResponseAsserter(responseContract).assert(response)

    // then no exception
  }

  @Test
  fun `Fails when Response status code is different from Contract`() {
    // given
    val responseContract = ResponseContract(202)
    val response = mockk<Response>()
    every { response.statusCode } returns 201
    every { response.contentType } returns null

    // expect
    val exception = assertFails { ResponseAsserter(responseContract).assert(response) }
    assert(exception.message!!.contains(Regex("(?=.*201)(?=.*202)")))
  }

  @Test
  fun `Validates Response headers`() {
    // given
    val responseContract = ResponseContract(202, headers = mapOf("x-test" to Property(IntegerDataType())))
    val response = mockk<Response>()
    every { response.statusCode } returns 202
    every { response.contentType } returns null
    every { response.headers } returns Headers(Header("x-test", "42"))

    // when
    ResponseAsserter(responseContract).assert(response)

    // then no exception
  }

  @Test
  fun `Fails Response headers`() {
    // given
    val responseContract = ResponseContract(202, headers = mapOf("x-test" to Property(IntegerDataType())))
    val response = mockk<Response>()
    every { response.statusCode } returns 202
    every { response.contentType } returns null
    every { response.headers } returns Headers(Header("x-test", "42"))

    // when
    ResponseAsserter(responseContract).assert(response)

    // then no exception
  }

  @Test
  fun `Validates body with JSON Content-Type`() {
    // given
    val responseContract = ResponseContract(statusCode = 200,
                                            body = Body("application/json",
                                                        ObjectDataType(mapOf("name" to Property(StringDataType())))))
    val response = mockk<Response>()
    every { response.statusCode } returns 200
    every { response.contentType } returns "application/json; charset=utf-8"
    every { response.body.asString() } returns """{ "name2":"John", "age": 25}"""

    // when
    ResponseAsserter(responseContract).assert(response)

    // then no exception
  }

  @Test
  fun `Validates body with array as root element and with JSON Content-Type`() {
    // given
    val responseContract = ResponseContract(statusCode = 200,
                                            body = Body("application/json", ArrayDataType(IntegerDataType())))
    val response = mockk<Response>()
    every { response.statusCode } returns 200
    every { response.contentType } returns "application/json; charset=utf-8"
    every { response.body.asString() } returns """[1,2,3]"""

    // when
    ResponseAsserter(responseContract).assert(response)

    // then no exception
  }

  @Test
  fun `Fails body with wrong Content-Type`() {
    // given
    val responseContract = ResponseContract(statusCode = 200,
                                            body = Body("text/plain",
                                                        ObjectDataType(mapOf("name" to Property(StringDataType())))))
    val response = mockk<Response>()
    every { response.statusCode } returns 200
    every { response.contentType } returns "application/json; charset=utf-8"
    every { response.body.asString() } returns """{ "name":"John", "age": 42}"""

    // expect
    val exception = assertFails { ResponseAsserter(responseContract).assert(response) }
    assert(exception.message!!.contains(Regex("(?=.*text/plain)(?=.*application/json; charset=utf-8)")))
  }

  @Test
  fun `Fails when Contract defines no Content-Type but Response has Content-Type`() {
    // given
    val responseContract = ResponseContract(statusCode = 200)
    val response = mockk<Response>()
    every { response.statusCode } returns 200
    every { response.contentType } returns "application/json; charset=utf-8"
    every { response.body.asString() } returns """{ "name":"John", "age": 42}"""

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
    every { response.statusCode } returns 200
    every { response.contentType } returns null

    // expect
    val exception = assertFails { ResponseAsserter(responseContract).assert(response) }
    assert(exception.message!!.contains("application/json"))
  }
}