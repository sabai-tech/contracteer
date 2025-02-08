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
  specificationFilePath = "src/test/resources/oas3_spec.yaml",
  portProperty = "mock.server1.port",
)
@ContracteerMockServer(
  specificationFilePath = "src/test/resources/oas3_spec.yaml",
  portProperty = "mock.server2.port",
)
class MultipleMockServersTest {

  @Autowired
  lateinit var environment: Environment


  @Test
  fun `start mockserver on random port`() {
    assertServerIsRunningWell(environment["mock.server1.port"]!!.toInt())
    assertServerIsRunningWell(environment["mock.server2.port"]!!.toInt())
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
