package tech.sabai.contracteer.mockserver

import tech.sabai.contracteer.core.swagger.loadContracts
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters
import java.io.File
import java.util.concurrent.Callable

@Command(name = "server-verifier",
         mixinStandardHelpOptions = true,
         version = ["0.0.1"],
         description = ["Mock server emulating your API using the OpenAPI 3 specification"])
class MockServerCli: Callable<Int> {

  @Parameters(index = "0", description = ["Path of the Open Api 3 file "])
  lateinit var specFile: File

  @CommandLine.Option(
    names = ["-p", "--port"],
    required = false,
    description = ["Server port (default: \${DEFAULT-VALUE})"])
  private var port = 8080

  override fun call(): Int {
    var exitCode = 0
    val result = specFile.loadContracts()
    if (result.isFailure()) {
      println(CommandLine.Help.Ansi.AUTO.string("@|bold,red Invalid file:|@"))
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