package tech.sabai.contracteer.verifier.junit

import org.http4k.server.Http4kServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll

class RandomTestServerPortTest {

  @ContractTest(openApiSpecPath = "src/test/resources/api_mixing_random_values_and_example_for_4xx_status.yaml")
  fun `verify contracts using random server port`() {
  }

  companion object {
    private lateinit var server: Http4kServer

    @field:ContractServerPort
    private var serverPort: Int = 0

    @JvmStatic
    @BeforeAll
    fun startServer() {
      server = TestServer.start(0)
      serverPort = server.port()
    }

    @JvmStatic
    @AfterAll
    fun stopServer() {
      server.stop()
    }
  }
}