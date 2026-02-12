package tech.sabai.contracteer.verifier.junit

import org.http4k.server.Http4kServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll

class StaticTestServerPortTest {

  @ContracteerTest(
    openApiDoc = "src/test/resources/api_with_scenario_and_schema_based_responses.yaml",
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