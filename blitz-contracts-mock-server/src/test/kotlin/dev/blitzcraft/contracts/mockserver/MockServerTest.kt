package dev.blitzcraft.contracts.mockserver

import dev.blitzcraft.contracts.core.*
import dev.blitzcraft.contracts.core.datatype.IntegerDataType
import dev.blitzcraft.contracts.core.datatype.ObjectDataType
import io.restassured.RestAssured
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.Matchers.emptyOrNullString
import kotlin.test.AfterTest
import kotlin.test.Test

class MockServerTest {

  private lateinit var mockServer: MockServer

  @AfterTest
  fun tearDown() {
    mockServer.stop()
  }

  @Test
  fun `responds for a contract with a path parameter`() {
    // given
    val contract = Contract(
      RequestContract(method = "GET",
                      path = "/v1/users/{id}",
                      pathParameters = listOf(Property("id", IntegerDataType(), required = true))),
      ResponseContract(statusCode = 200,
                       body = Body("application/json",  ObjectDataType(listOf(Property("id", IntegerDataType())))))
    )
    mockServer = MockServer(contracts = setOf(contract))

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
      RequestContract(method = "GET",
                      path = "/v1/users/{id}",
                      pathParameters = listOf(Property("id", IntegerDataType(), Example(42), required = true))),
      ResponseContract(statusCode = 200,
                       body = Body("application/json", ObjectDataType(listOf(Property("id", IntegerDataType())))))
    )
    mockServer = MockServer(contracts = setOf(contract))

    // when
    mockServer.start()
    RestAssured.port = mockServer.port()

    // then
    given()
      .accept("application/json")
      .get("/v1/users/42").then()
      .assertThat()
      .statusCode(200)
      .body("id", notNullValue(Int::class.java))
  }

  @Test
  fun `does not respond when path parameter is not equal to example value`() {
    // given
    val contract = Contract(
      RequestContract(method = "GET",
                      path = "/v1/users/{id}",
                      pathParameters = listOf(Property("id", IntegerDataType(), Example(42), required = true))),
      ResponseContract(statusCode = 200,
                       body = Body("application/json", ObjectDataType(listOf(Property("id", IntegerDataType()))))),
      "simple example"
    )
    mockServer = MockServer(contracts = setOf(contract))

    // when
    mockServer.start()
    RestAssured.port = mockServer.port()

    // then
    given()
      .accept("application/json")
      .get("/v1/users/123").then()
      .assertThat()
      .statusCode(404)
  }

  @Test
  fun `responds for a contract with a required query parameter`() {
    // given
    val contract = Contract(
      RequestContract(method = "GET",
                      path = "/v1/users",
                      queryParameters = listOf(Property("id", IntegerDataType(), required = true))),
      ResponseContract(statusCode = 200,
                       body = Body("application/json", ObjectDataType(listOf(Property("id", IntegerDataType())))))
    )
    mockServer = MockServer(contracts = setOf(contract))

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
  fun `does not respond when required query parameter is missing`() {
    // given
    val contract = Contract(
      RequestContract(method = "GET",
                      path = "/v1/users",
                      queryParameters = listOf(Property("id", IntegerDataType(), required = true))),
      ResponseContract(statusCode = 200,
                       body = Body("application/json", ObjectDataType(listOf(Property("id", IntegerDataType())))))
    )
    mockServer = MockServer(contracts = setOf(contract))

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
      RequestContract(method = "GET",
                      path = "/v1/users",
                      queryParameters = listOf(Property("id", IntegerDataType(), Example(42), required = true))),
      ResponseContract(statusCode = 200,
                       body = Body("application/json", ObjectDataType(listOf(Property("id", IntegerDataType())))))
    )
    mockServer = MockServer(contracts = setOf(contract))

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
  fun `does not respond when query parameter is not equal to example value`() {
    // given
    val contract = Contract(
      RequestContract(method = "GET",
                      path = "/v1/users",
                      queryParameters = listOf(Property("id", IntegerDataType(), Example(42), required = true))),
      ResponseContract(statusCode = 200,
                       body = Body("application/json", ObjectDataType(listOf(Property("id", IntegerDataType()))))),
      "simple example"
    )
    mockServer = MockServer(contracts = setOf(contract))

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

  @Test
  fun `responds for a contract with a required cookie`() {
    // given
    val contract = Contract(
      RequestContract(method = "GET",
                      path = "/v1/users",
                      cookies = listOf(Property("id", IntegerDataType(), required = true))),
      ResponseContract(statusCode = 200,
                       body = Body("application/json", ObjectDataType(listOf(Property("id", IntegerDataType())))))
    )
    mockServer = MockServer(contracts = setOf(contract))

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
  fun `does not respond when required cookie is missing`() {
    // given
    val contract = Contract(
      RequestContract(method = "GET",
                      path = "/v1/users",
                      cookies = listOf(Property("id", IntegerDataType(), required = true))),
      ResponseContract(statusCode = 200,
                       body = Body("application/json", ObjectDataType(listOf(Property("id", IntegerDataType())))))
    )
    mockServer = MockServer(contracts = setOf(contract))

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
      RequestContract(method = "GET",
                      path = "/v1/users",
                      cookies = listOf(Property("id", IntegerDataType(), Example(42), required = true))),
      ResponseContract(statusCode = 200,
                       body = Body("application/json", ObjectDataType(listOf(Property("id", IntegerDataType())))))
    )
    mockServer = MockServer(contracts = setOf(contract))

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
  fun `does not respond when cookie is not equal to example value`() {
    // given
    val contract = Contract(
      RequestContract(method = "GET",
                      path = "/v1/users",
                      cookies = listOf(Property("id", IntegerDataType(), Example(42), required = true))),
      ResponseContract(statusCode = 200,
                       body = Body("application/json", ObjectDataType(listOf(Property("id", IntegerDataType()))))),
      "simple example"
    )
    mockServer = MockServer(contracts = setOf(contract))

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

  @Test
  fun `responds for contract with a request body`() {
    // given
    val contract = Contract(
      RequestContract(method = "POST",
                      path = "/v1/users",
                      body = Body("application/json", ObjectDataType(listOf(Property("id", IntegerDataType()))))),
      ResponseContract(statusCode = 201,
                       body = Body("application/json", ObjectDataType(listOf(Property("id", IntegerDataType())))))
    )
    mockServer = MockServer(contracts = setOf(contract))

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
  fun `does not respond for contract with a request body which does not match`() {
    // given
    val contract = Contract(
      RequestContract(method = "POST",
                      path = "/v1/users",
                      body = Body("application/json", ObjectDataType(listOf(Property("id", IntegerDataType()))))),
      ResponseContract(statusCode = 201,
                       body = Body("application/json", ObjectDataType(listOf(Property("id", IntegerDataType())))))
    )
    mockServer = MockServer(contracts = setOf(contract))

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
      RequestContract(method = "POST",
                      path = "/v1/users",
                      body = Body("application/json",
                                  ObjectDataType(listOf(Property("id", IntegerDataType()))),
                                  Example(mapOf("id" to 42)))),
      ResponseContract(statusCode = 201,
                       body = Body("application/json", ObjectDataType(listOf(Property("id", IntegerDataType()))))),
      "simple example"
    )
    mockServer = MockServer(contracts = setOf(contract))

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
  fun `does not for contract with a request body example when request body does not match example value`() {
    // given
    val contract = Contract(
      RequestContract(method = "POST",
                      path = "/v1/users",
                      body = Body("application/json",
                                  ObjectDataType(listOf(Property("id", IntegerDataType()))),
                                  Example(mapOf("id" to 42)))),
      ResponseContract(statusCode = 201,
                       body = Body("application/json", ObjectDataType(listOf(Property("id", IntegerDataType()))))),
      "simple example"
    )
    mockServer = MockServer(contracts = setOf(contract))

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

  @Test
  fun `responds with no content-type for response`() {
    // given
    val contract = Contract(
      RequestContract(method = "POST",
                      path = "/v1/users",
                      body = Body("application/json", ObjectDataType(listOf(Property("id", IntegerDataType()))))),
      ResponseContract(statusCode = 201)
    )
    mockServer = MockServer(contracts = setOf(contract))

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