package tech.sabai.contracteer.mockserver.spring

import io.restassured.RestAssured
import io.restassured.RestAssured.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment
import org.springframework.core.env.get
import kotlin.test.Test


@SpringBootTest(classes = [TestApp::class])
@ContracteerMockServer(
  openApiDoc = "src/test/resources/oas3_definition.yaml",
  port = 45678,
  portProperty = "mock.server.port",
  baseUrlProperty = "mock.server.baseUrl"
)
class UseUserConfigurationMockServerTest {

  @Autowired
  lateinit var environment: Environment

  @Test
  fun `start mockserver on random port`() {
    val serverPort = environment["mock.server.port"]?.toInt()
    val serverUrl = environment["mock.server.baseUrl"]

    assert(serverPort == 45678)
    assert(serverUrl == "http://localhost:45678")
    assertServerIsRunningWell(serverPort!!)
  }

  private fun assertServerIsRunningWell(serverPort: Int) {
    RestAssured.port = serverPort
    given()
      .get("/products/42")
      .then()
      .assertThat()
      .statusCode(200)
  }
}
