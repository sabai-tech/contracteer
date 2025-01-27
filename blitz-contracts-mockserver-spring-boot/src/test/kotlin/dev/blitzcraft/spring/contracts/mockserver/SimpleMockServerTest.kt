package dev.blitzcraft.spring.contracts.mockserver

import io.restassured.RestAssured
import io.restassured.RestAssured.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment
import org.springframework.core.env.get
import kotlin.test.Test


@SpringBootTest(classes = [TestApp::class])
@BlitzContractsMockServer(specificationFilePath = "src/test/resources/oas3_spec.yaml")
class SimpleMockServerTest {

  @Autowired
  lateinit var environment: Environment

  @Test
  fun `start mockserver on random port`() {
    val serverPort = environment["blitzcontracts.server.port"]!!.toInt()
    val serverUrl = environment["blitzcontracts.server.baseUrl"]

    assert(serverPort > 0)
    assert(serverUrl != null)
    assertServerIsRunningWell(serverPort)
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
