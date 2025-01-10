package dev.blitzcraft.contracts.mockserver

import dev.blitzcraft.contracts.core.contract.*
import dev.blitzcraft.contracts.core.datatype.IntegerDataType
import dev.blitzcraft.contracts.core.datatype.ObjectDataType
import dev.blitzcraft.contracts.core.datatype.StringDataType
import io.restassured.RestAssured
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.*
import org.hamcrest.Matchers.emptyOrNullString
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import kotlin.test.Test

class MockServerTest {

  private lateinit var mockServer: MockServer

  @AfterEach
  fun tearDown() {
    mockServer.stop()
  }

  @Test
  fun `responds correctly when mixing contract with example and contracts with no example for the same path and method`() {
    // given
    val contracts = listOf(
      Contract(
        ContractRequest(method = "GET",
                        path = "/v1/users/{id}",
                        pathParameters = listOf(PathParameter("id", IntegerDataType()))),
        ContractResponse(statusCode = 200,
                         body = Body(
                           contentType = "application/json",
                           dataType = ObjectDataType(properties = mapOf("id" to IntegerDataType()))))),
      Contract(
        ContractRequest(method = "GET",
                        path = "/v1/users/{id}",
                        pathParameters = listOf(PathParameter("id", IntegerDataType(), Example(999)))),
        ContractResponse(statusCode = 404,
                         body = Body(
                           contentType = "application/json",
                           dataType = ObjectDataType(properties = mapOf("error" to StringDataType())),
                           example = Example(mapOf("error" to "Not Found")))),
        "Not Found Example")
    )
    mockServer = MockServer(contracts = contracts)

    // when
    mockServer.start()
    RestAssured.port = mockServer.port()

    // then
    given()
      .accept("application/json")
      .get("/v1/users/999").then()
      .assertThat()
      .statusCode(404)
      .body("error", equalTo("Not Found"))
    // and
    given()
      .accept("application/json")
      .get("/v1/users/42").then()
      .assertThat()
      .statusCode(200)
      .body("id", notNullValue(Int::class.java))
  }

  @Test
  fun `responds with 409 status code when there is more than one matching contract with the same priority`() {
    // given
    val contracts = listOf(
      Contract(
        ContractRequest(method = "GET",
                        path = "/v1/users/{id}",
                        pathParameters = listOf(PathParameter("id", IntegerDataType(), Example(999)))),
        ContractResponse(statusCode = 404,
                         body = Body(
                           "application/json",
                           ObjectDataType(properties = mapOf("error" to StringDataType())),
                           Example(mapOf("error" to "Not Found")))),
        "Not Found Example 1"),
      Contract(
        ContractRequest(method = "GET",
                        path = "/v1/users/{id}",
                        pathParameters = listOf(PathParameter("id", IntegerDataType(), Example(999)))),
        ContractResponse(statusCode = 404,
                         body = Body(
                           "application/json",
                           ObjectDataType(properties = mapOf("error" to StringDataType())),
                           Example(mapOf("error" to "Not Found")))),
        "Not Found Example 2")
    )
    mockServer = MockServer(contracts = contracts)

    // when
    mockServer.start()
    RestAssured.port = mockServer.port()

    // then
    given()
      .accept("application/json")
      .get("/v1/users/999").then()
      .assertThat()
      .statusCode(409)
      .body(allOf(
        containsString(contracts[0].description()),
        containsString(contracts[1].description()))
      )
  }

  @Nested
  inner class PathContractParameter {
    @Test
    fun `responds for a contract with a path parameter`() {
      // given
      val contract = Contract(
        ContractRequest(method = "GET",
                        path = "/v1/users/{id}",
                        pathParameters = listOf(PathParameter("id", IntegerDataType()))),
        ContractResponse(statusCode = 200,
                         body = Body("application/json", ObjectDataType(properties = mapOf("id" to IntegerDataType()))))
      )
      mockServer = MockServer(contracts = listOf(contract))

      // when
      mockServer.start()
      RestAssured.port = mockServer.port()

      // then
      given()
        .accept("application/json")
        .get("/v1/users/123").then()
        .assertThat()
        .statusCode(200)
        .body("id", notNullValue(Int::class.java))
    }

    @Test
    fun `responds for a contract with a path parameter example`() {
      // given
      val contract = Contract(
        ContractRequest(method = "GET",
                        path = "/v1/users/{id}",
                        pathParameters = listOf(PathParameter("id", IntegerDataType(), Example(42)))),
        ContractResponse(statusCode = 200,
                         body = Body(
                           "application/json",
                           ObjectDataType(properties = mapOf("id" to IntegerDataType())),
                           Example(mapOf("id" to 42)))),
        "simple contract")
      mockServer = MockServer(contracts = listOf(contract))

      // when
      mockServer.start()
      RestAssured.port = mockServer.port()

      // then
      given()
        .accept("application/json")
        .get("/v1/users/42").then()
        .assertThat()
        .statusCode(200)
        .body("id", equalTo(42))
    }

    @Test
    fun `responds with status code 404 when path parameter is not equal to example value`() {
      // given
      val contract = Contract(
        ContractRequest(method = "GET",
                        path = "/v1/users/{id}",
                        pathParameters = listOf(PathParameter("id", IntegerDataType(), Example(42)))),
        ContractResponse(statusCode = 200,
                         body = Body(
                           "application/json",
                           ObjectDataType(properties = mapOf("id" to IntegerDataType())),
                           Example(mapOf("id" to 42)))),
        "simple contract")
      mockServer = MockServer(contracts = listOf(contract))

      // when
      mockServer.start()
      RestAssured.port = mockServer.port()

      // then
      given()
        .accept("application/json")
        .get("/v1/users/123").then()
        .assertThat()
        .statusCode(404)
        .contentType("text/plain")
        .body(allOf(
          containsString(contract.description()),
          containsString("value does not match. Expected: 42, Actual: 123")
        ))
    }

    @Test
    fun `responds with status code 404 when path parameter is missing`() {
      // given
      val contract = Contract(
        ContractRequest(method = "GET",
                        path = "/v1/users/{id}",
                        pathParameters = listOf(PathParameter("id", IntegerDataType(), Example(42)))),
        ContractResponse(statusCode = 200,
                         body = Body(
                           "application/json",
                           ObjectDataType(properties = mapOf("id" to IntegerDataType())))),
        "simple example"
      )
      mockServer = MockServer(contracts = listOf(contract))

      // when
      mockServer.start()
      RestAssured.port = mockServer.port()

      // then
      given()
        .accept("application/json")
        .get("/v1/users").then()
        .assertThat()
        .statusCode(404)
    }
  }

  @Nested
  inner class QueryParameter {
    @Test
    fun `responds for a contract with a required query parameter`() {
      // given
      val contract = Contract(
        ContractRequest(method = "GET",
                        path = "/v1/users",
                        queryParameters = listOf(ContractParameter("id", IntegerDataType(), true))),
        ContractResponse(statusCode = 200,
                         body = Body("application/json", ObjectDataType(properties = mapOf("id" to IntegerDataType()))))
      )
      mockServer = MockServer(contracts = listOf(contract))

      // when
      mockServer.start()
      RestAssured.port = mockServer.port()

      // then
      given()
        .accept("application/json")
        .get("/v1/users?id=123").then()
        .assertThat()
        .statusCode(200)
        .body("id", notNullValue(Int::class.java))
    }

    @Test
    fun `responds with status code 404 when required query parameter is missing`() {
      // given
      val contract = Contract(
        ContractRequest(method = "GET",
                        path = "/v1/users",
                        queryParameters = listOf(ContractParameter("id", IntegerDataType(), true))),
        ContractResponse(statusCode = 200,
                         body = Body("application/json", ObjectDataType(properties = mapOf("id" to IntegerDataType()))))
      )
      mockServer = MockServer(contracts = listOf(contract))

      // when
      mockServer.start()
      RestAssured.port = mockServer.port()

      // then
      given()
        .accept("application/json")
        .get("/v1/users").then()
        .assertThat()
        .statusCode(404)
    }

    @Test
    fun `responds for a contract with a query parameter example`() {
      // given
      val contract = Contract(
        ContractRequest(method = "GET",
                        path = "/v1/users",
                        queryParameters = listOf(ContractParameter("id", IntegerDataType(), true, Example(42)))),
        ContractResponse(statusCode = 200,
                         body = Body("application/json", ObjectDataType(properties = mapOf("id" to IntegerDataType()))))
      )
      mockServer = MockServer(contracts = listOf(contract))

      // when
      mockServer.start()
      RestAssured.port = mockServer.port()

      // then
      given()
        .accept("application/json")
        .get("/v1/users?id=42").then()
        .assertThat()
        .statusCode(200)
        .body("id", notNullValue(Int::class.java))
    }

    @Test
    fun `responds with status code 404 when query parameter is not equal to example value`() {
      // given
      val contract = Contract(
        ContractRequest(method = "GET",
                        path = "/v1/users",
                        queryParameters = listOf(ContractParameter("id", IntegerDataType(), true, Example(42)))),
        ContractResponse(statusCode = 200,
                         body = Body("application/json", ObjectDataType(properties = mapOf("id" to IntegerDataType())))),
        "simple example"
      )
      mockServer = MockServer(contracts = listOf(contract))

      // when
      mockServer.start()
      RestAssured.port = mockServer.port()

      // then
      given()
        .accept("application/json")
        .get("/v1/users?id=123").then()
        .assertThat()
        .statusCode(404)
    }
  }

  @Nested
  inner class Cookie {
    @Test
    fun `responds for a contract with a required cookie`() {
      // given
      val contract = Contract(
        ContractRequest(method = "GET",
                        path = "/v1/users",
                        cookies = listOf(ContractParameter("id", IntegerDataType(), true))),
        ContractResponse(statusCode = 200,
                         body = Body("application/json", ObjectDataType(properties = mapOf("id" to IntegerDataType()))))
      )
      mockServer = MockServer(contracts = listOf(contract))

      // when
      mockServer.start()
      RestAssured.port = mockServer.port()

      // then
      given()
        .accept("application/json")
        .cookies(mapOf("id" to "123"))
        .get("/v1/users").then()
        .assertThat()
        .statusCode(200)
        .body("id", notNullValue(Int::class.java))
    }

    @Test
    fun `responds with status code 404 when required cookie is missing`() {
      // given
      val contract = Contract(
        ContractRequest(method = "GET",
                        path = "/v1/users",
                        cookies = listOf(ContractParameter("id", IntegerDataType(), true))),
        ContractResponse(statusCode = 200,
                         body = Body("application/json", ObjectDataType(properties = mapOf("id" to IntegerDataType()))))
      )
      mockServer = MockServer(contracts = listOf(contract))

      // when
      mockServer.start()
      RestAssured.port = mockServer.port()

      // then
      given()
        .accept("application/json")
        .get("/v1/users").then()
        .assertThat()
        .statusCode(404)
    }

    @Test
    fun `responds for a contract with a cookie example`() {
      // given
      val contract = Contract(
        ContractRequest(method = "GET",
                        path = "/v1/users",
                        cookies = listOf(ContractParameter("id", IntegerDataType(), true, Example(42)))),
        ContractResponse(statusCode = 200,
                         body = Body("application/json", ObjectDataType(properties = mapOf("id" to IntegerDataType()))))
      )
      mockServer = MockServer(contracts = listOf(contract))

      // when
      mockServer.start()
      RestAssured.port = mockServer.port()

      // then
      given()
        .accept("application/json")
        .cookies(mapOf("id" to "42"))
        .get("/v1/users").then()
        .assertThat()
        .statusCode(200)
        .body("id", notNullValue(Int::class.java))
    }

    @Test
    fun `responds with status code 404 when cookie is not equal to example value`() {
      // given
      val contract = Contract(
        ContractRequest(method = "GET",
                        path = "/v1/users",
                        cookies = listOf(ContractParameter("id", IntegerDataType(), true, Example(42)))),
        ContractResponse(statusCode = 200,
                         body = Body("application/json", ObjectDataType(properties = mapOf("id" to IntegerDataType())))),
        "simple example"
      )
      mockServer = MockServer(contracts = listOf(contract))

      // when
      mockServer.start()
      RestAssured.port = mockServer.port()

      // then
      given()
        .accept("application/json")
        .cookies(mapOf("id" to "123"))
        .get("/v1/users").then()
        .assertThat()
        .statusCode(404)
    }
  }

  @Nested
  inner class BodyContractRequest {
    @Test
    fun `responds for contract with a request body`() {
      // given
      val contract = Contract(
        ContractRequest(method = "POST",
                        path = "/v1/users",
                        body = Body("application/json", ObjectDataType(properties = mapOf("id" to IntegerDataType())))),
        ContractResponse(statusCode = 201,
                         body = Body("application/json", ObjectDataType(properties = mapOf("id" to IntegerDataType()))))
      )
      mockServer = MockServer(contracts = listOf(contract))

      // when
      mockServer.start()
      RestAssured.port = mockServer.port()

      // then
      given()
        .accept("application/json")
        .contentType("application/json")
        .body("""{"id": 42}""")
        .post("/v1/users").then()
        .assertThat()
        .statusCode(201)
        .body("id", notNullValue(Int::class.java))
    }

    @Test
    fun `responds with status code 404 when a request body does not match`() {
      // given
      val contract = Contract(
        ContractRequest(method = "POST",
                        path = "/v1/users",
                        body = Body("application/json", ObjectDataType(properties = mapOf("id" to IntegerDataType())))),
        ContractResponse(statusCode = 201,
                         body = Body("application/json", ObjectDataType(properties = mapOf("id" to IntegerDataType()))))
      )
      mockServer = MockServer(contracts = listOf(contract))

      // when
      mockServer.start()
      RestAssured.port = mockServer.port()

      // then
      given()
        .accept("application/json")
        .contentType("application/json")
        .body("""{"id": "john"}""")
        .post("/v1/users").then()
        .assertThat()
        .statusCode(404)
    }

    @Test
    fun `responds for contract with a request body example`() {
      // given
      val contract = Contract(
        ContractRequest(method = "POST",
                        path = "/v1/users",
                        body = Body("application/json",
                                    ObjectDataType(properties = mapOf("id" to IntegerDataType())),
                                    Example(mapOf("id" to 42)))),
        ContractResponse(statusCode = 201,
                         body = Body("application/json",
                                     ObjectDataType(properties = mapOf("id" to IntegerDataType())),
                                     Example(mapOf("id" to 999)))),
        "simple example"
      )
      mockServer = MockServer(contracts = listOf(contract))

      // when
      mockServer.start()
      RestAssured.port = mockServer.port()

      // then
      given()
        .accept("application/json")
        .contentType("application/json")
        .body("""{"id": 42}""")
        .post("/v1/users").then()
        .assertThat()
        .statusCode(201)
        .body("id", equalTo(999))
    }

    @Test
    fun `responds with status code 404 when a request body does not match example value`() {
      // given
      val contract = Contract(
        ContractRequest(method = "POST",
                        path = "/v1/users",
                        body = Body("application/json",
                                    ObjectDataType(properties = mapOf("id" to IntegerDataType())),
                                    Example(mapOf("id" to 42)))),
        ContractResponse(statusCode = 201,
                         body = Body("application/json",
                                     ObjectDataType(properties = mapOf("id" to IntegerDataType())),
                                     Example(mapOf("id" to 999)))),
        "simple example"
      )
      mockServer = MockServer(contracts = listOf(contract))

      // when
      mockServer.start()
      RestAssured.port = mockServer.port()

      // then
      given()
        .accept("application/json")
        .contentType("application/json")
        .body("""{"id": 99}""")
        .post("/v1/users").then()
        .assertThat()
        .statusCode(404)
    }
  }

  @Nested
  inner class ContentType {

    @Test
    fun `respond with status code 404 when request content-type does not match contract`() {
      // given
      val contract = Contract(
        ContractRequest(method = "POST",
                        path = "/v1/users",
                        body = Body("application/json", ObjectDataType(properties = mapOf("id" to IntegerDataType())))),
        ContractResponse(statusCode = 201,
                         body = Body("application/json", ObjectDataType(properties = mapOf("id" to IntegerDataType()))))
      )
      mockServer = MockServer(contracts = listOf(contract))

      // when
      mockServer.start()
      RestAssured.port = mockServer.port()

      // then
      given()
        .accept("application/json")
        .contentType("text/plain")
        .body("""{"id": 42}""")
        .post("/v1/users").then()
        .assertThat()
        .statusCode(404)
    }

    @Test
    fun `respond with status code 404 when request header 'Accept' does not match contract response content type`() {
      // given
      val contract = Contract(
        ContractRequest(method = "GET",
                        path = "/v1/users/{id}",
                        pathParameters = listOf(PathParameter("id", IntegerDataType()))),
        ContractResponse(statusCode = 200,
                         body = Body("application/json", ObjectDataType(properties = mapOf("id" to IntegerDataType()))))
      )
      mockServer = MockServer(contracts = listOf(contract))

      // when
      mockServer.start()
      RestAssured.port = mockServer.port()

      // then
      given()
        .accept("text/plain")
        .get("/v1/users/123").then()
        .assertThat()
        .statusCode(404)
    }

    @Test
    fun `responds with no content-type for response`() {
      // given
      val contract = Contract(
        ContractRequest(method = "POST",
                        path = "/v1/users",
                        body = Body("application/json", ObjectDataType(properties = mapOf("id" to IntegerDataType())))),
        ContractResponse(statusCode = 201)
      )
      mockServer = MockServer(contracts = listOf(contract))

      // when
      mockServer.start()
      RestAssured.port = mockServer.port()

      // then
      given()
        .body(mapOf("id" to 123))
        .contentType("application/json")
        .post("/v1/users")
        .then()
        .assertThat()
        .statusCode(201)
        .contentType(`is`(emptyOrNullString()))
        .body(`is`(emptyOrNullString()))
    }
  }
}