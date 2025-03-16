package tech.sabai.contracteer.mockserver

import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters
import tech.sabai.contracteer.core.swagger.OpenApiLoader
import java.util.concurrent.Callable

@Command(name = "server-verifier",
         mixinStandardHelpOptions = true,
         version = ["0.0.1"],
         description = [
           "Starts a Contracteer Mock Server based on an OpenAPI 3 Definition."
         ]
)
class MockServerCli: Callable<Int> {

  @Parameters(index = "0",
              description = [
                "Specifies the location of an OpenAPI 3 specification,",
                "either as a local file path or a remote URL."
              ])
  lateinit var path: String

  @CommandLine.Option(
    names = ["-p", "--port"],
    required = false,
    description = ["Port to run Contracteer Mock Server (default: \${DEFAULT-VALUE})"])
  private var port = 8080

  override fun call(): Int {
    var exitCode = 0
    val result = OpenApiLoader.loadContracts(path)
    if (result.isFailure()) {
      println(CommandLine.Help.Ansi.AUTO.string("@|bold,red ❌ Failed to load OpenAPI Definition:|@"))
      result.errors().forEach { println(CommandLine.Help.Ansi.AUTO.string("     - @|yellow $it|@")) }
      exitCode = 1
    } else {
      val mockServer = MockServer(result.value!!, port)
      mockServer.start()
    }
    return exitCode
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>) { CommandLine(MockServerCli()).execute(*args) }
  }
}