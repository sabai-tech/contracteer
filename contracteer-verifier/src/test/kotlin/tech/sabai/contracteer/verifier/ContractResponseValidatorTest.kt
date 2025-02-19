package tech.sabai.contracteer.verifier

import tech.sabai.contracteer.core.contract.Body
import tech.sabai.contracteer.core.contract.ContractParameter
import tech.sabai.contracteer.core.contract.ContractResponse
import io.mockk.every
import io.mockk.mockk
import org.http4k.core.Response
import org.http4k.core.Status
import tech.sabai.contracteer.core.datatype.*
import tech.sabai.contracteer.verifier.DataTypeFixture.arrayDataType
import tech.sabai.contracteer.verifier.DataTypeFixture.integerDataType
import tech.sabai.contracteer.verifier.DataTypeFixture.objectDataType
import tech.sabai.contracteer.verifier.DataTypeFixture.stringDataType
import kotlin.test.Test

class ContractResponseValidatorTest {

  @Test
  fun `Validates successfully Response status code`() {
    // given
    val responseContract = ContractResponse(202)
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
  fun `Does not validate when Response status code is different from Contract`() {
    // given
    val responseContract = ContractResponse(202)
    val response = mockk<Response>()
    every { response.status } returns Status.CREATED
    every { response.headers } returns emptyList()
    every { response.header("Content-Type") } returns null

    // when
    val validationResult = ResponseValidator(responseContract).validate(response)

    // then
    assert(validationResult.isFailure())
    assert(validationResult.errors().size == 1)
    assert(validationResult.errors().first().contains(Regex("(?=.*201)(?=.*202)")))
  }

  @Test
  fun `Validates successfully Response headers`() {
    // given
    val responseContract =
      ContractResponse(202, headers = listOf(ContractParameter("x-test", integerDataType())))
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
  fun `Does not validate invalid Response header`() {
    // given
    val responseContract =
      ContractResponse(202, headers = listOf(ContractParameter("x-test", integerDataType())))
    val response = mockk<Response>()
    every { response.status } returns Status.ACCEPTED
    every { response.headers } returns listOf("x-test" to "John")
    every { response.header("Content-Type") } returns null

    // when
    val validationResult = ResponseValidator(responseContract).validate(response)

    // then
    assert(validationResult.isFailure())
    assert(validationResult.errors().size == 1)
    assert(validationResult.errors().first().contains("x-test"))
  }

  @Test
  fun `Validates successfully optional Response header is missing`() {
    // given
    val responseContract =
      ContractResponse(202, headers = listOf(ContractParameter("x-test", integerDataType())))
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
  fun `Does not validate successfully when required Response header is missing`() {
    // given
    val responseContract =
      ContractResponse(202, headers = listOf(ContractParameter("x-test", integerDataType(), true)))
    val response = mockk<Response>()
    every { response.status } returns Status.ACCEPTED
    every { response.header("Content-Type") } returns null
    every { response.headers } returns emptyList()

    // expect
    val validationResult = ResponseValidator(responseContract).validate(response)
    assert(validationResult.isFailure())
    assert(validationResult.errors().size == 1)
    assert(validationResult.errors().first().contains(Regex("(?=.*Missing)(?=.*x-test)")))
  }

  @Test
  fun `Validates body with JSON Content-Type`() {
    // given
    val responseContract = ContractResponse(statusCode = 200,
                                            body = Body(contentType = "application/json",
                                                        dataType = objectDataType(properties = mapOf(
                                                          "name" to stringDataType(),
                                                          "age" to integerDataType()))))
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
    val responseContract = ContractResponse(statusCode = 200,
                                            body = Body(contentType = "application/json",
                                                        dataType = objectDataType(
                                                          properties = mapOf(
                                                            "name" to stringDataType(),
                                                            "age" to integerDataType()),
                                                          requiredProperties = setOf("name"))))
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
  fun `Does not validate body with JSON Content-Type and missing required properties`() {
    // given
    val responseContract = ContractResponse(statusCode = 200,
                                            body = Body(contentType = "application/json",
                                                        dataType = objectDataType(
                                                          properties = mapOf(
                                                            "name" to stringDataType(),
                                                            "age" to integerDataType()),
                                                          requiredProperties = setOf("name", "age"))))
    val response = mockk<Response>()
    every { response.status } returns Status.OK
    every { response.header("Content-Type") } returns "application/json; charset=utf-8"
    every { response.headers } returns listOf("Content-Type" to "application/json; charset=utf-8")
    every { response.bodyString() } returns """{ "age":42}"""

    // when
    val validationResult = ResponseValidator(responseContract).validate(response)

    // then
    assert(validationResult.isFailure())
    assert(validationResult.errors().size == 1)
    assert(validationResult.errors().first().contains("name"))
  }

  @Test
  fun `Validates body with JSON Content-Type and array as root element`() {
    // given
    val responseContract = ContractResponse(statusCode = 200,
                                            body = Body("application/json",
                                                        arrayDataType(itemDataType = integerDataType())))
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
  fun `Does not validate when content-type does not match contract`() {
    // given
    val responseContract = ContractResponse(statusCode = 200,
                                            body = Body("text/plain",
                                                        objectDataType(properties = mapOf("name" to stringDataType()))))
    val response = mockk<Response>()
    every { response.status } returns Status.OK
    every { response.header("Content-Type") } returns "application/json; charset=utf-8"
    every { response.headers } returns listOf("Content-Type" to "application/json; charset=utf-8")
    every { response.bodyString() } returns """{ "name":"John", "age": 42}"""

    // when
    val validationResult = ResponseValidator(responseContract).validate(response)

    // then
    assert(validationResult.isFailure())
    assert(validationResult.errors().size == 1)
    assert(validationResult.errors().first().contains(Regex("(?=.*text/plain)(?=.*application/json; charset=utf-8)")))
  }

  @Test
  fun `Does not validate when Contract Response has no Content-Type but Response has Content-Type`() {
    // given
    val responseContract = ContractResponse(statusCode = 200)
    val response = mockk<Response>()
    every { response.status } returns Status.OK
    every { response.header("Content-Type") } returns "application/json; charset=utf-8"
    every { response.headers } returns listOf("Content-Type" to "application/json; charset=utf-8")
    every { response.bodyString() } returns """{ "name":"John", "age": 42}"""

    // when
    val validationResult = ResponseValidator(responseContract).validate(response)

    // then
    assert(validationResult.isFailure())
    assert(validationResult.errors().size == 1)
    assert(validationResult.errors().first().contains("application/json; charset=utf-8"))
  }

  @Test
  fun `Does not validate when Contract Response defines a Content-Type but Response has no Content-Type`() {
    // given
    val responseContract = ContractResponse(statusCode = 200,
                                            body = Body("application/json",
                                                        objectDataType(properties = mapOf("name" to stringDataType()))))
    val response = mockk<Response>()
    every { response.status } returns Status.OK
    every { response.header("Content-Type") } returns null
    every { response.headers } returns emptyList()

    // when
    val validationResult = ResponseValidator(responseContract).validate(response)

    // then
    assert(validationResult.isFailure())
    assert(validationResult.errors().size == 1)
    assert(validationResult.errors().first().contains("application/json"))
  }
}