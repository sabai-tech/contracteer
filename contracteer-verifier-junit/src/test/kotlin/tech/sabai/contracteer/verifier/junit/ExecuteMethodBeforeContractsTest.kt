package tech.sabai.contracteer.verifier.junit

import org.http4k.server.Http4kServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll

class ExecuteMethodBeforeContractsTest {

  @ContracteerTest(openApiDoc = "src/test/resources/api_with_scenario_and_schema_based_responses.yaml")
  fun `execute method before verification`() {
    serverPort = server.port()
  }

  companion object {
    private lateinit var server: Http4kServer

    @field:ContracteerServerPort
    private var serverPort: Int = 0

    @JvmStatic
    @BeforeAll
    fun startServer() {
      server = TestServer.start(0)
    }

    @JvmStatic
    @AfterAll
    fun stopServer() {
      server.stop()
    }
  }
}
