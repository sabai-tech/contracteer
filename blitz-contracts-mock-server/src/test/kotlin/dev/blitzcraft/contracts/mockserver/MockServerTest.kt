package dev.blitzcraft.contracts.mockserver

import dev.blitzcraft.contracts.core.*
import dev.blitzcraft.contracts.core.datatype.IntegerDataType
import dev.blitzcraft.contracts.core.datatype.ObjectDataType
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
  fun `responds for a contract with a path parameter and no request body`() {
    // given
    val contract = Contract(
      RequestContract(method = "GET",
                      path = "/v1/users/{id}",
                      pathParameters = mapOf("id" to Property(IntegerDataType()))),
      ResponseContract(statusCode = 200,
                       body = Body("application/json", ObjectDataType(mapOf("id" to Property(IntegerDataType())))))
    )
    mockServer = MockServer(listOf(contract))

    // when
    mockServer.start()

    // then
    given()
      .accept("application/json")
      .get("/v1/users/123").then()
      .assertThat()
      .statusCode(200)
      .body("id", notNullValue(Int::class.java))
  }

  @Test
  fun `responds for a contract with a path parameter example and no request body`() {
    // given
    val contract = Contract(
      RequestContract(method = "GET",
                      path = "/v1/users/{id}",
                      pathParameters = mapOf("id" to Property(IntegerDataType(), Example(42)))),
      ResponseContract(statusCode = 200,
                       body = Body("application/json", ObjectDataType(mapOf("id" to Property(IntegerDataType())))))
    )
    mockServer = MockServer(listOf(contract))

    // when
    mockServer.start()

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
                      pathParameters = mapOf("id" to Property(IntegerDataType(), Example(42)))),
      ResponseContract(statusCode = 200,
                       body = Body("application/json", ObjectDataType(mapOf("id" to Property(IntegerDataType()))))),
      "simple example"
    )
    mockServer = MockServer(listOf(contract))

    // when
    mockServer.start()

    // then
    given()
      .accept("application/json")
      .get("/v1/users/123").then()
      .assertThat()
      .statusCode(404)
  }

  @Test
  fun `responds for contract with a request body`() {
    // given
    val contract = Contract(
      RequestContract(method = "POST",
                      path = "/v1/users",
                      body = Body("application/json", ObjectDataType(mapOf("id" to Property(IntegerDataType()))))),
      ResponseContract(statusCode = 201,
                       body = Body("application/json", ObjectDataType(mapOf("id" to Property(IntegerDataType())))))
    )
    mockServer = MockServer(listOf(contract))

    // when
    mockServer.start()

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
                      body = Body("application/json", ObjectDataType(mapOf("id" to Property(IntegerDataType()))))),
      ResponseContract(statusCode = 201,
                       body = Body("application/json", ObjectDataType(mapOf("id" to Property(IntegerDataType())))))
    )
    mockServer = MockServer(listOf(contract))

    // when
    mockServer.start()

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
                                  ObjectDataType(mapOf("id" to Property(IntegerDataType()))),
                                  Example(mapOf("id" to 42)))),
      ResponseContract(statusCode = 201,
                       body = Body("application/json", ObjectDataType(mapOf("id" to Property(IntegerDataType()))))),
      "simple example"
    )
    mockServer = MockServer(listOf(contract))

    // when
    mockServer.start()

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
  fun `do no responds for contract with a request body example when body does not match example value`() {
    // given
    val contract = Contract(
      RequestContract(method = "POST",
                      path = "/v1/users",
                      body = Body("application/json",
                                  ObjectDataType(mapOf("id" to Property(IntegerDataType()))),
                                  Example(mapOf("id" to 42)))),
      ResponseContract(statusCode = 201,
                       body = Body("application/json", ObjectDataType(mapOf("id" to Property(IntegerDataType()))))),
      "simple example"
    )
    mockServer = MockServer(listOf(contract))

    // when
    mockServer.start()

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
                      body = Body("application/json", ObjectDataType(mapOf("id" to Property(IntegerDataType()))))),
      ResponseContract(statusCode = 201)
    )
    mockServer = MockServer(listOf(contract))

    // when
    mockServer.start()

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