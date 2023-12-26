package dev.blitzcraft.contracts.verifier

import dev.blitzcraft.contracts.core.Body
import dev.blitzcraft.contracts.core.Property
import dev.blitzcraft.contracts.core.ResponseContract
import dev.blitzcraft.contracts.core.datatype.*
import io.mockk.every
import io.mockk.mockk
import org.http4k.core.Response
import org.http4k.core.Status
import kotlin.test.Test

class ResponseValidatorTest {

  @Test
  fun `Validates Response status code`() {
    // given
    val responseContract = ResponseContract(202)
    val response = mockk<Response>()
    every { response.status } returns Status.ACCEPTED
    every { response.headers } returns emptyList()
    every { response.header("Content-Type") } returns null

    // when
    val validationResult = ResponseValidator(responseContract).validate(response)

    // then
    assert(validationResult.isSuccess())
  }

  @Test
  fun `Fails when Response status code is different from Contract`() {
    // given
    val responseContract = ResponseContract(202)
    val response = mockk<Response>()
    every { response.status } returns Status.CREATED
    every { response.headers } returns emptyList()
    every { response.header("Content-Type") } returns null

    // when
    val validationResult = ResponseValidator(responseContract).validate(response)

    // then
    assert(validationResult.isSuccess().not())
    assert(validationResult.errors().size == 1)
    assert(validationResult.errors().first().contains(Regex("(?=.*201)(?=.*202)")))
  }

  @Test
  fun `Validates Response headers`() {
    // given
    val responseContract = ResponseContract(202, headers = listOf(Property("x-test", IntegerDataType())))
    val response = mockk<Response>()
    every { response.status } returns Status.ACCEPTED
    every { response.headers } returns listOf("x-test" to "42")
    every { response.header("Content-Type") } returns null

    // when
    val validationResult = ResponseValidator(responseContract).validate(response)

    // then
    assert(validationResult.isSuccess())
  }

  @Test
  fun `Fails for invalid Response header`() {
    // given
    val responseContract = ResponseContract(202, headers = listOf(Property("x-test", IntegerDataType())))
    val response = mockk<Response>()
    every { response.status } returns Status.ACCEPTED
    every { response.headers } returns listOf("x-test" to "John")
    every { response.header("Content-Type") } returns null

    // when
    val validationResult = ResponseValidator(responseContract).validate(response)

    // then
    assert(validationResult.isSuccess().not())
    assert(validationResult.errors().size == 1)
    assert(validationResult.errors().first().contains("x-test"))
  }

  @Test
  fun `Does not fails for missing optional Response header`() {
    // given
    val responseContract = ResponseContract(202, headers = listOf(Property("x-test", IntegerDataType())))
    val response = mockk<Response>()
    every { response.status } returns Status.ACCEPTED
    every { response.header("Content-Type") } returns null
    every { response.headers } returns emptyList()

    // when
    val validationResult = ResponseValidator(responseContract).validate(response)

    // then
    assert(validationResult.isSuccess())
  }

  @Test
  fun `Fails for missing required Response header`() {
    // given
    val responseContract =
      ResponseContract(202, headers = listOf(Property("x-test", IntegerDataType(), required = true)))
    val response = mockk<Response>()
    every { response.status } returns Status.ACCEPTED
    every { response.header("Content-Type") } returns null
    every { response.headers } returns emptyList()

    // expect
    val validationResult = ResponseValidator(responseContract).validate(response)
    assert(validationResult.isSuccess().not())
    assert(validationResult.errors().size == 1)
    assert(validationResult.errors().first().contains(Regex("(?=.*Missing)(?=.*x-test)")))
  }

  @Test
  fun `Validates body with JSON Content-Type`() {
    // given
    val responseContract = ResponseContract(statusCode = 200,
                                            body = Body("application/json",
                                                        ObjectDataType(listOf(
                                                          Property("name", StringDataType()),
                                                          Property("age", IntegerDataType())))))
    val response = mockk<Response>()
    every { response.status } returns Status.OK
    every { response.header("Content-Type") } returns "application/json; charset=utf-8"
    every { response.headers } returns listOf("Content-Type" to "application/json; charset=utf-8")
    every { response.bodyString() } returns """{ "name":"John", "age": 25}"""

    // when
    val validationResult = ResponseValidator(responseContract).validate(response)

    // then
    assert(validationResult.isSuccess())
  }

  @Test
  fun `Validates body with JSON Content-Type and missing optional properties`() {
    // given
    val responseContract = ResponseContract(statusCode = 200,
                                            body = Body("application/json",
                                                        ObjectDataType(listOf(
                                                          Property("name", StringDataType(), required = true),
                                                          Property("age", IntegerDataType(), required = false)))))
    val response = mockk<Response>()
    every { response.status } returns Status.OK
    every { response.header("Content-Type") } returns "application/json; charset=utf-8"
    every { response.headers } returns listOf("Content-Type" to "application/json; charset=utf-8")
    every { response.bodyString() } returns """{ "name":"John"}"""

    // when
    val validationResult = ResponseValidator(responseContract).validate(response)

    // then
    assert(validationResult.isSuccess())
  }

  @Test
  fun `Fails to validate body with JSON Content-Type and missing required properties`() {
    // given
    val responseContract = ResponseContract(statusCode = 200,
                                            body = Body("application/json",
                                                        ObjectDataType(listOf(
                                                          Property("name", StringDataType(), required = true),
                                                          Property("age", IntegerDataType(), required = false)))))
    val response = mockk<Response>()
    every { response.status } returns Status.OK
    every { response.header("Content-Type") } returns "application/json; charset=utf-8"
    every { response.headers } returns listOf("Content-Type" to "application/json; charset=utf-8")
    every { response.bodyString() } returns """{ "age":42}"""

    // when
    val validationResult = ResponseValidator(responseContract).validate(response)

    // then
    assert(validationResult.isSuccess().not())
    assert(validationResult.errors().size == 1)
    assert(validationResult.errors().first().contains("name"))
  }

  @Test
  fun `Validates body with array as root element and with JSON Content-Type`() {
    // given
    val responseContract = ResponseContract(statusCode = 200,
                                            body = Body("application/json", ArrayDataType(IntegerDataType())))
    val response = mockk<Response>()
    every { response.status } returns Status.OK
    every { response.header("Content-Type") } returns "application/json; charset=utf-8"
    every { response.headers } returns listOf("Content-Type" to "application/json; charset=utf-8")
    every { response.bodyString() } returns """[1,2,3]"""

    // when
    val validationResult = ResponseValidator(responseContract).validate(response)

    // then
    assert(validationResult.isSuccess())
  }

  @Test
  fun `Fails when Body has wrong Content-Type`() {
    // given
    val responseContract = ResponseContract(statusCode = 200,
                                            body = Body("text/plain",
                                                        ObjectDataType(listOf(Property("name", StringDataType())))))
    val response = mockk<Response>()
    every { response.status } returns Status.OK
    every { response.header("Content-Type") } returns "application/json; charset=utf-8"
    every { response.headers } returns listOf("Content-Type" to "application/json; charset=utf-8")
    every { response.bodyString() } returns """{ "name":"John", "age": 42}"""

    // when
    val validationResult = ResponseValidator(responseContract).validate(response)

    // then
    assert(validationResult.isSuccess().not())
    assert(validationResult.errors().size == 1)
    assert(validationResult.errors().first().contains(Regex("(?=.*text/plain)(?=.*application/json; charset=utf-8)")))
  }

  @Test
  fun `Fails when Contract defines no Content-Type but Response has Content-Type`() {
    // given
    val responseContract = ResponseContract(statusCode = 200)
    val response = mockk<Response>()
    every { response.status } returns Status.OK
    every { response.header("Content-Type") } returns "application/json; charset=utf-8"
    every { response.headers } returns listOf("Content-Type" to "application/json; charset=utf-8")
    every { response.bodyString() } returns """{ "name":"John", "age": 42}"""

    // when
    val validationResult = ResponseValidator(responseContract).validate(response)

    // then
    assert(validationResult.isSuccess().not())
    assert(validationResult.errors().size == 1)
    assert(validationResult.errors().first().contains("application/json; charset=utf-8"))
  }

  @Test
  fun `Fails when Contract defines Content-Type but Response has no Content-Type`() {
    // given
    val responseContract = ResponseContract(statusCode = 200,
                                            body = Body("application/json",
                                                        ObjectDataType(listOf(Property("name", StringDataType())))))
    val response = mockk<Response>()
    every { response.status } returns Status.OK
    every { response.header("Content-Type") } returns null
    every { response.headers } returns emptyList()

    // when
    val validationResult = ResponseValidator(responseContract).validate(response)

    // then
    assert(validationResult.isSuccess().not())
    assert(validationResult.errors().size == 1)
    assert(validationResult.errors().first().contains("application/json"))
  }
}