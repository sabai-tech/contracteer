package tech.sabai.contracteer.verifier.junit

import org.http4k.server.Http4kServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll

class StaticTestServerPortTest {

  @ContracteerTest(
    openApiDoc = "src/test/resources/api_mixing_random_values_and_example_for_4xx_status.yaml",
    serverPort = 9090)
  fun `verify contracts using static server port`() {
  }

  companion object {
    private lateinit var server: Http4kServer

    @JvmStatic
    @BeforeAll
    fun startServer() {
      server = TestServer.start(9090)
    }

    @JvmStatic
    @AfterAll
    fun stopServer() {
      server.stop()
    }
  }
}